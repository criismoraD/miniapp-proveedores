package com.example.provedoreschiclayo.voice;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Cliente de la Gemini Live API (audio bidireccional) para ProveeChiclayo.
 *
 * Conversa en tiempo real: el usuario habla, el modelo responde con voz y va
 * llamando herramientas ({@code filtrar_productos}) para ir filtrando la lista
 * de productos de la pantalla principal mientras conversa.
 */
public final class ProveeLiveClient {

    private static final String TAG = "ProveeLive";

    /** Muestreo de ENVÍO exigido por la Live API. */
    public static final int SEND_SAMPLE_RATE = 16000;
    /** Muestreo de RECEPCIÓN del audio del modelo. */
    public static final int RECEIVE_SAMPLE_RATE = 24000;

    private static final String WS_URL =
            "wss://generativelanguage.googleapis.com/ws/"
            + "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent";

    private static final String DEFAULT_VOICE = "Puck";

    /** Callbacks hacia la UI (siempre llegan en el hilo de OkHttp). */
    public interface Listener {
        /** La sesión quedó configurada y lista (llegó setupComplete). */
        void onReady();

        /** Texto que dijo el usuario (transcripción de entrada). */
        void onUserSaid(String text);

        /** Texto que respondió el modelo (transcripción de salida). */
        void onModelSaid(String text);

        /** Estado legible para el HUD. */
        void onStatus(String status);

        /** El modelo interrumpió su respuesta (barge-in). */
        void onInterrupted();

        /** Error fatal de la sesión. */
        void onError(String message);

        /** La sesión se cerró. */
        void onClosed();

        /**
         * El modelo pidió filtrar la lista de productos. Debe devolver cuántos
         * productos quedaron para que el modelo lo diga en voz alta.
         */
        int onFilterProducts(String query, String category);
    }

    private final Listener listener;
    private final String apiKey;
    private final String voiceName;

    private OkHttpClient http;
    private WebSocket ws;
    private volatile boolean connected = false;
    private volatile boolean closedByUser = false;
    private volatile boolean setupDone = false;

    /* -------- Audio -------- */
    private AudioRecord recorder;
    private AudioTrack player;
    private Thread captureThread;
    private volatile boolean capturing = false;
    private volatile boolean modelSpeaking = false;
    /**
     * true cuando el usuario pausó al asistente: se deja de mandar el micrófono
     * y se corta el audio que esté sonando, pero la sesión sigue abierta.
     */
    private volatile boolean paused = false;
    private final Object playLock = new Object();
    private ByteArrayOutputStream playBuffer = new ByteArrayOutputStream();
    private Thread playThread;

    public ProveeLiveClient(String apiKey, String voiceName, Listener listener) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.voiceName = (voiceName == null || voiceName.isEmpty()) ? DEFAULT_VOICE : voiceName;
        this.listener = listener;
    }

    public boolean isConnected() { return connected && setupDone; }

    /** true si el asistente está pausado por el usuario. */
    public boolean isPaused() { return paused; }

    /**
     * Pausa o reanuda al asistente SIN cerrar la sesión.
     * Al pausar: deja de enviar el micrófono y corta el audio que esté sonando.
     * Al reanudar: vuelve a escuchar con la misma conversación.
     */
    public void setPaused(boolean value) {
        paused = value;
        if (paused) {
            modelSpeaking = false;
            synchronized (playLock) { playBuffer = new ByteArrayOutputStream(); }
            try { if (player != null) player.pause(); } catch (Exception ignored) {}
            try { if (player != null) player.flush(); } catch (Exception ignored) {}
            try { if (player != null) player.play(); } catch (Exception ignored) {}
            status("Asistente en pausa");
        } else {
            status("Te escucho… habla con naturalidad");
        }
    }

    /**
     * Manda un turno de texto (chips de sugerencia o el contexto inicial del
     * catálogo). El modelo lo procesa igual que si se lo hubieran dicho.
     */
    public void sendText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (paused) return;
        WebSocket w = ws;
        if (w == null || !setupDone) return;
        try {
            JSONObject msg = new JSONObject().put("clientContent", new JSONObject()
                    .put("turns", new JSONArray().put(new JSONObject()
                            .put("role", "user")
                            .put("parts", new JSONArray().put(new JSONObject()
                                    .put("text", text)))))
                    .put("turnComplete", true));
            w.send(msg.toString());
        } catch (Exception e) {
            Log.w(TAG, "sendText error", e);
        }
    }

    /* ============================== Ciclo de vida ============================== */

    public void connect() {
        status("Conectando con la voz en vivo…");

        http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();

        Request req = new Request.Builder()
                .url(WS_URL)
                .addHeader("x-goog-api-key", apiKey)
                .build();

        ws = http.newWebSocket(req, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                sendSetup(webSocket);
            }

            @Override public void onMessage(WebSocket webSocket, String text) {
                handleServerMessage(text);
            }

            @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleServerMessage(bytes.utf8());
            }

            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;
                stopAudio();
                String detail = t == null ? "desconocido" : t.getMessage();
                int code = response == null ? -1 : response.code();
                Log.w(TAG, "WS failure " + code + ": " + detail);
                if (!closedByUser) {
                    listener.onError("No se pudo conectar con la voz en vivo"
                            + (code > 0 ? " (HTTP " + code + ")" : "") + ": " + detail);
                }
                listener.onClosed();
            }

            @Override public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                setupDone = false;
                stopAudio();
                listener.onClosed();
            }
        });
    }

    public void close() {
        closedByUser = true;
        stopAudio();
        try { if (ws != null) ws.close(1000, "user closed"); } catch (Exception ignored) {}
        ws = null;
        connected = false;
        setupDone = false;
        try { if (http != null) http.dispatcher().executorService().shutdown(); } catch (Exception ignored) {}
    }

    /* ================================ Setup =================================== */

    private void sendSetup(WebSocket webSocket) {
        try {
            JSONObject cfg = new JSONObject();
            cfg.put("responseModalities", new JSONArray().put("AUDIO"));

            JSONObject voiceConfig = new JSONObject()
                    .put("prebuiltVoiceConfig", new JSONObject().put("voiceName", voiceName));
            cfg.put("speechConfig", new JSONObject()
                    .put("voiceConfig", voiceConfig)
                    .put("languageCode", "es-US"));

            JSONObject systemInstruction = new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject()
                            .put("text", systemInstruction())));

            JSONObject setup = new JSONObject();
            setup.put("model", "models/" + GeminiConfig.LIVE_MODEL);
            setup.put("generationConfig", cfg);
            setup.put("systemInstruction", systemInstruction);
            setup.put("tools", buildTools());
            setup.put("inputAudioTranscription", new JSONObject()
                    .put("languageCodes", new JSONArray().put("es-US")));
            setup.put("outputAudioTranscription", new JSONObject());

            webSocket.send(new JSONObject().put("setup", setup).toString());
        } catch (Exception e) {
            Log.e(TAG, "setup error", e);
            listener.onError("No pude configurar la sesión de voz: " + e.getMessage());
        }
    }

    private static String systemInstruction() {
        return "Eres el asistente de voz de ProveeChiclayo, app mayorista B2B de Chiclayo y "
                + "Lambayeque, Perú. Conversas en vivo con bodegueros y dueños de tienda. "
                + "Hablas en español neutro, cálido, breve (máximo 2 frases por respuesta) y "
                + "usas SOLO el catálogo que te doy. "
                + "REGLA PRINCIPAL — FILTRAR EN PANTALLA: tienes la herramienta "
                + "'filtrar_productos'. Úsala SIEMPRE que el usuario mencione un producto, una "
                + "marca, una categoría, un proveedor o diga cosas como 'muéstrame', 'filtra', "
                + "'busca', 'quiero ver', 'los más baratos', 'de la zona X'. La pantalla de atrás "
                + "se va filtrando en vivo mientras conversan, así que llama a la herramienta "
                + "en cuanto tengas claro qué buscar, sin esperar a terminar de hablar. "
                + "Si el usuario va afinando (por ejemplo 'y el más barato', 'solo de La "
                + "Victoria', 'esa marca no, la otra'), vuelve a llamar 'filtrar_productos' con "
                + "los criterios nuevos y confirma en voz alta. Si quiere ver todo otra vez, "
                + "llama a la herramienta con los dos campos vacíos. "
                + "PRECIOS: indica la opción más barata y la más conveniente considerando el "
                + "flete a la zona del usuario. "
                + "REGLA DE MONEDA: cuando hables de dinero di SIEMPRE la palabra «soles» "
                + "(por ejemplo «23 soles con 50 céntimos»); NUNCA el símbolo «S/» ni «sol». "
                + "TELÉFONOS: dalos separados en grupos de tres cifras. "
                + "Nunca inventes productos, precios ni teléfonos que no estén en el catálogo; "
                + "si algo no está, dilo con naturalidad.";
    }

    /** Herramientas (function calling) que puede invocar el modelo. */
    private static JSONArray buildTools() {
        JSONArray tools = new JSONArray();
        try {
            JSONArray decls = new JSONArray();
            decls.put(new JSONObject()
                    .put("name", "filtrar_productos")
                    .put("description", "Filtra EN VIVO la lista de productos de la pantalla "
                            + "principal de la app. Úsala siempre que el usuario pida ver, "
                            + "mostrar, filtrar o buscar productos por nombre, marca, categoría "
                            + "o proveedor. Si envías los dos campos vacíos, vuelve a mostrar "
                            + "todo el catálogo.")
                    .put("parameters", new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject()
                                    .put("consulta", new JSONObject()
                                            .put("type", "string")
                                            .put("description", "Texto libre a buscar por "
                                                    + "nombre, marca, presentación o nombre del "
                                                    + "proveedor. Vacío para no filtrar por texto."))
                                    .put("categoria", new JSONObject()
                                            .put("type", "string")
                                            .put("description", "Categoría exacta: Abarrotes, "
                                                    + "Bebidas, Golosinas o Limpieza. Vacío o "
                                                    + "'Todas' para no filtrar por categoría.")))
                            .put("required", new JSONArray())));
            tools.put(new JSONObject().put("functionDeclarations", decls));
        } catch (Exception e) {
            Log.e(TAG, "buildTools error", e);
        }
        return tools;
    }

    /* ========================= Procesar el servidor ========================== */

    private void handleServerMessage(String raw) {
        try {
            JSONObject msg = new JSONObject(raw);

            if (msg.has("setupComplete")) {
                setupDone = true;
                listener.onReady();
                status("Te escucho… habla con naturalidad");
                startAudio();
                return;
            }

            JSONObject sc = msg.optJSONObject("serverContent");
            if (sc != null) {
                JSONObject inT = sc.optJSONObject("inputTranscription");
                if (inT != null && inT.has("text")) listener.onUserSaid(inT.getString("text"));

                JSONObject outT = sc.optJSONObject("outputTranscription");
                if (outT != null && outT.has("text")) listener.onModelSaid(outT.getString("text"));

                if (sc.optBoolean("interrupted", false)) {
                    synchronized (playLock) { playBuffer = new ByteArrayOutputStream(); }
                    modelSpeaking = false;
                    listener.onInterrupted();
                }

                JSONObject modelTurn = sc.optJSONObject("modelTurn");
                if (modelTurn != null) {
                    JSONArray parts = modelTurn.optJSONArray("parts");
                    if (parts != null) {
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject p = parts.optJSONObject(i);
                            if (p == null) continue;
                            JSONObject inline = p.optJSONObject("inlineData");
                            if (inline == null) continue;
                            String mime = inline.optString("mimeType", "");
                            String data = inline.optString("data", "");
                            if (data.isEmpty()) continue;
                            if (mime.startsWith("audio/")) {
                                queueAudio(Base64.decode(data, Base64.DEFAULT));
                            }
                        }
                    }
                }

                if (sc.optBoolean("turnComplete", false)) {
                    modelSpeaking = false;
                    status("Te escucho… habla con naturalidad");
                }
                return;
            }

            if (msg.has("toolCall")) {
                handleToolCall(msg.optJSONObject("toolCall"));
                return;
            }

            if (msg.has("goAway")) {
                status("La sesión se cerrará pronto…");
            }
        } catch (Exception e) {
            Log.w(TAG, "mensaje ilegible: " + raw);
        }
    }

    /* ============================ Function calling ============================ */

    private void handleToolCall(JSONObject toolCall) {
        if (toolCall == null) return;
        JSONArray functionCalls = toolCall.optJSONArray("functionCalls");
        if (functionCalls == null || functionCalls.length() == 0) return;

        JSONArray responses = new JSONArray();
        for (int i = 0; i < functionCalls.length(); i++) {
            JSONObject fc = functionCalls.optJSONObject(i);
            if (fc == null) continue;
            String id = fc.optString("id", "");
            String name = fc.optString("name", "");
            JSONObject args = fc.optJSONObject("args");
            if (args == null) args = new JSONObject();

            JSONObject result = new JSONObject();
            try {
                if ("filtrar_productos".equals(name)) {
                    String q = args.optString("consulta", "");
                    String cat = args.optString("categoria", "");
                    int count = listener.onFilterProducts(q, cat);
                    result.put("productos_mostrados", count);
                    result.put("ok", true);
                } else {
                    result.put("error", "herramienta desconocida: " + name);
                }
            } catch (Exception e) {
                try { result.put("error", "No pude ejecutar " + name + ": " + e.getMessage()); }
                catch (Exception ignored) {}
            }

            try {
                responses.put(new JSONObject().put("id", id).put("name", name).put("response", result));
            } catch (Exception ignored) {}
        }

        if (responses.length() == 0) return;
        try {
            JSONObject msg = new JSONObject().put("toolResponse",
                    new JSONObject().put("functionResponses", responses));
            WebSocket w = ws;
            if (w != null) w.send(msg.toString());
        } catch (Exception e) {
            Log.w(TAG, "no pude devolver el resultado de las herramientas", e);
        }
    }

    /* ================================ Audio =================================== */

    private void startAudio() {
        startCapture();
        startPlayer();
    }

    private void stopAudio() {
        capturing = false;
        try { if (captureThread != null) { captureThread.interrupt(); captureThread = null; } }
        catch (Exception ignored) {}
        try { if (recorder != null) { recorder.stop(); recorder.release(); recorder = null; } }
        catch (Exception ignored) {}
        try { if (player != null) { player.pause(); player.flush(); player.stop(); player.release(); player = null; } }
        catch (Exception ignored) {}
        playThread = null;
        modelSpeaking = false;
        synchronized (playLock) { playBuffer = new ByteArrayOutputStream(); }
    }

    private void startCapture() {
        int minBuf = AudioRecord.getMinBufferSize(SEND_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuf <= 0) minBuf = SEND_SAMPLE_RATE * 2;
        final int bufSize = Math.max(minBuf * 2, SEND_SAMPLE_RATE);

        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SEND_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("micrófono no inicializado");
            }
            recorder.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "no pude abrir el micrófono", e);
            listener.onError("No pude abrir el micrófono: " + e.getMessage());
            return;
        }

        capturing = true;
        captureThread = new Thread(() -> {
            byte[] buf = new byte[bufSize / 2];
            while (capturing && connected) {
                int n;
                try {
                    n = recorder.read(buf, 0, buf.length);
                } catch (Exception e) {
                    break;
                }
                if (n <= 0) continue;
                // Pausado por el usuario: no mandamos nada.
                if (paused) continue;
                // Mientras el modelo habla NO enviamos: evita que se escuche a sí mismo.
                if (modelSpeaking) continue;
                try {
                    String b64 = Base64.encodeToString(buf, 0, n, Base64.NO_WRAP);
                    JSONObject audio = new JSONObject()
                            .put("data", b64)
                            .put("mimeType", "audio/pcm;rate=" + SEND_SAMPLE_RATE);
                    JSONObject msg = new JSONObject()
                            .put("realtimeInput", new JSONObject().put("audio", audio));
                    WebSocket w = ws;
                    if (w != null && setupDone) w.send(msg.toString());
                } catch (Exception ignored) {}
            }
        }, "provee-live-mic");
        captureThread.start();
    }

    private void queueAudio(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return;
        modelSpeaking = true;
        synchronized (playLock) {
            try { playBuffer.write(pcm); } catch (Exception ignored) {}
        }
    }

    private void startPlayer() {
        if (playThread != null) return;
        int minBuf = AudioTrack.getMinBufferSize(RECEIVE_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuf <= 0) minBuf = RECEIVE_SAMPLE_RATE * 2;
        final int trackBuf = Math.max(minBuf * 2, RECEIVE_SAMPLE_RATE);

        try {
            player = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RECEIVE_SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(trackBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            player.play();
        } catch (Exception e) {
            Log.e(TAG, "no pude abrir el altavoz", e);
            listener.onError("No pude reproducir el audio: " + e.getMessage());
            return;
        }

        playThread = new Thread(() -> {
            while (connected) {
                byte[] chunk = null;
                synchronized (playLock) {
                    if (playBuffer.size() > 0) {
                        chunk = playBuffer.toByteArray();
                        playBuffer = new ByteArrayOutputStream();
                    }
                }
                if (chunk != null) {
                    try { player.write(chunk, 0, chunk.length); } catch (Exception e) { break; }
                } else {
                    try { Thread.sleep(15); } catch (InterruptedException e) { break; }
                }
            }
        }, "provee-live-spk");
        playThread.start();
    }

    /* ============================ Contexto (catálogo) ========================= */

    /** Resumen compacto del catálogo para el contexto inicial de la sesión. */
    public static String catalog(VoiceQueryEngine.Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("ZONAS DE REPARTO: ");
        for (int i = 0; i < ctx.locations.size(); i++) {
            DistrictLocation l = ctx.locations.get(i);
            sb.append(l.name).append(" (").append(l.reference).append(")");
            if (i < ctx.locations.size() - 1) sb.append("; ");
        }
        if (ctx.currentLocation != null) {
            sb.append("\nZONA DEL USUARIO: ").append(ctx.currentLocation.name);
        }
        sb.append("\n\nPROVEEDORES:/n");
        for (Supplier s : ctx.suppliers) {
            sb.append("- ").append(s.name).append(" | ").append(s.district)
              .append(" | tel ").append(s.phone)
              .append(" | ").append(String.format(Locale.US, "%.1f", s.rating)).append(" estrellas")
              .append(" | entrega ").append(s.deliveryTimeEstimate)
              .append(" | flete base ").append(String.format(Locale.US, "%.2f", s.baseDeliveryFee))
              .append(" soles + ").append(String.format(Locale.US, "%.2f", s.perKmDeliveryFee))
              .append("/km | envío gratis desde ")
              .append(String.format(Locale.US, "%.2f", s.freeShippingThreshold)).append(" soles\n");
        }
        sb.append("\nPRODUCTOS (precio más barato / más conveniente con flete):\n");
        for (Product p : ctx.products) {
            SupplierOffer cheap = p.getLowestPriceOffer();
            sb.append("- ").append(p.name).append(" [").append(p.brand).append(", ")
              .append(p.category).append(", ").append(p.presentation).append("]: desde ")
              .append(String.format(Locale.US, "%.2f", cheap.price)).append(" soles (")
              .append(cheap.supplier.name).append(")");
            if (ctx.currentLocation != null) {
                SupplierOffer nice = p.getMostConvenientOffer(ctx.currentLocation, 1);
                if (nice != null) {
                    sb.append("; con flete ")
                      .append(String.format(Locale.US, "%.2f",
                              nice.calculateTotalCost(ctx.currentLocation, 1)))
                      .append(" soles (").append(nice.supplier.name).append(")");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void status(String s) {
        try { listener.onStatus(s); } catch (Exception ignored) {}
    }
}
