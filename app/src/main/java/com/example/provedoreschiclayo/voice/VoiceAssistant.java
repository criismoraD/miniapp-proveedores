package com.example.provedoreschiclayo.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Asistente de voz de ProveeChiclayo: TTS + Gemini (mismas claves que la otra
 * mini app) con respaldo al motor local {@link VoiceQueryEngine} sin conexión.
 */
public final class VoiceAssistant {
    private static final String TAG = "VoiceAssistant";

    /** Resultado unificado de una consulta (Gemini o motor local). */
    public interface Reply {
        void onReply(String title, List<String> rows, String spoken, String filterQuery);
        void onError(String message);
    }

    private TextToSpeech tts;
    private boolean ttsReady = false;

    // ---------------- Voz (TTS) ----------------

    public void initTts(Context ctx) {
        if (tts != null) return;
        tts = new TextToSpeech(ctx, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                int r = tts.setLanguage(new Locale("es", "PE"));
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA
                        && r != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!ttsReady) {
                    int r2 = tts.setLanguage(new Locale("es", "ES"));
                    ttsReady = r2 != TextToSpeech.LANG_MISSING_DATA
                            && r2 != TextToSpeech.LANG_NOT_SUPPORTED;
                }
                tts.setSpeechRate(0.98f);
            }
        });
    }

    public void speak(String text) {
        if (tts != null && ttsReady && text != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "provee_reply");
        }
    }

    public void stopSpeaking() {
        if (tts != null) tts.stop();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ttsReady = false;
        }
    }

    // ---------------- Consulta ----------------

    /** Gemini primero (si hay claves); si falla, motor local. */
    public void ask(final String question, final VoiceQueryEngine.Ctx ctx, final Reply cb) {
        if (GeminiConfig.hasKeys()) {
            askGemini(question, ctx, new Reply() {
                @Override
                public void onReply(String title, List<String> rows, String spoken, String filterQuery) {
                    cb.onReply(title, rows, spoken, filterQuery);
                }
                @Override
                public void onError(String message) {
                    Log.w(TAG, "Gemini falló, uso motor local: " + message);
                    local(question, ctx, cb);
                }
            });
        } else {
            local(question, ctx, cb);
        }
    }

    private void local(String question, VoiceQueryEngine.Ctx ctx, Reply cb) {
        try {
            VoiceQueryEngine.Answer a = VoiceQueryEngine.answer(question, ctx);
            cb.onReply(a.title, a.rows, a.spoken, a.filterQuery);
        } catch (Exception e) {
            cb.onError("No pude procesar la pregunta.");
        }
    }

    // ---------------- Gemini REST (HttpURLConnection, sin librerías) ----------------

    private void askGemini(final String question, final VoiceQueryEngine.Ctx ctx, final Reply cb) {
        new Thread(() -> {
            try {
                JSONObject sysInstruction = new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text",
                                "Eres el asistente de voz de ProveeChiclayo, app mayorista B2B "
                                + "de Chiclayo y Lambayeque, Perú. Respondes en español neutro, "
                                + "breve (máximo 3 frases) y usando SOLO el catálogo que te doy. "
                                + "REGLAS: 1) Si preguntan precios, indica la opción más barata y "
                                + "la más conveniente considerando el flete. 2) Nunca inventes "
                                + "productos, precios ni teléfonos que no estén en el catálogo. "
                                + "3) Cuando hables de dinero di SIEMPRE la palabra «soles» "
                                + "(ejemplo «23 soles con 50 céntimos»); NUNCA el símbolo «S/». "
                                + "4) Los teléfonos dalos separados en grupos de tres cifras. "
                                + "5) Si no está en el catálogo, dilo y sugiere preguntar por un "
                                + "producto, un proveedor o una zona.")));

                JSONObject userContent = new JSONObject()
                        .put("role", "user")
                        .put("parts", new JSONArray().put(new JSONObject()
                                .put("text", "CATÁLOGO:\n" + catalog(ctx)
                                        + "\n\nPREGUNTA DEL BODEGUERO: " + question)));

                JSONObject body = new JSONObject()
                        .put("systemInstruction", sysInstruction)
                        .put("contents", new JSONArray().put(userContent))
                        .put("generationConfig", new JSONObject()
                                .put("temperature", 0.3)
                                .put("maxOutputTokens", 320));

                String url = GeminiConfig.BASE_URL + GeminiConfig.TEXT_MODEL + ":generateContent";
                String text = post(url, GeminiConfig.FREE_API_KEY, body);
                if (text == null) text = post(url, GeminiConfig.PAID_API_KEY, body);
                if (text == null) {
                    cb.onError("sin respuesta de Gemini");
                    return;
                }
                final String reply = text;
                cb.onReply("🎙️  Asistente ProveeChiclayo",
                        java.util.Collections.singletonList(reply), reply, null);
            } catch (final Exception e) {
                Log.e(TAG, "askGemini error", e);
                cb.onError(e.getMessage());
            }
        }).start();
    }

    private static String post(String urlStr, String apiKey, JSONObject body) {
        if (apiKey == null || apiKey.isEmpty()) return null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key", apiKey);
            conn.setDoOutput(true);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(payload);
            os.close();

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            String resp = readAll(is);
            if (code < 200 || code >= 300) {
                Log.w(TAG, "HTTP " + code + ": " + resp);
                return null;
            }
            JSONObject json = new JSONObject(resp);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return null;
            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            if (content == null) return null;
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) return null;
            return parts.getJSONObject(0).optString("text", "").trim();
        } catch (Exception e) {
            Log.w(TAG, "post error", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    /** Resumen compacto del catálogo para el prompt de Gemini. */
    public static String catalog(VoiceQueryEngine.Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("ZONAS DE REPARTO: ");
        for (int i = 0; i < ctx.locations.size(); i++) {
            DistrictLocation l = ctx.locations.get(i);
            sb.append(l.name).append(" (").append(l.reference).append(")");
            if (i < ctx.locations.size() - 1) sb.append("; ");
        }
        sb.append("\nBODEGA DEL USUARIO: ").append(ctx.currentLocation.name).append("\n");
        sb.append("PROVEEDORES:\n");
        for (Supplier s : ctx.suppliers) {
            sb.append("- ").append(s.name).append(" | ").append(s.district)
              .append(" | tel ").append(s.phone)
              .append(" | ").append(String.format(Locale.US, "%.1f", s.rating)).append(" estrellas")
              .append(" | entrega ").append(s.deliveryTimeEstimate)
              .append(" | flete base S/ ").append(String.format(Locale.US, "%.2f", s.baseDeliveryFee))
              .append(" + S/ ").append(String.format(Locale.US, "%.2f", s.perKmDeliveryFee)).append("/km")
              .append(" | envío gratis desde S/ ")
              .append(String.format(Locale.US, "%.2f", s.freeShippingThreshold)).append("\n");
        }
        sb.append("PRODUCTOS (precio más barato / más conveniente con flete a la bodega del usuario):\n");
        for (Product p : ctx.products) {
            SupplierOffer cheap = p.getLowestPriceOffer();
            SupplierOffer nice = p.getMostConvenientOffer(ctx.currentLocation, 1);
            sb.append("- ").append(p.name).append(" [").append(p.brand).append(", ")
              .append(p.presentation).append("]: desde S/ ")
              .append(String.format(Locale.US, "%.2f", cheap.price)).append(" (")
              .append(cheap.supplier.name).append(")");
            if (nice != null) {
                sb.append("; con flete S/ ")
                  .append(String.format(Locale.US, "%.2f", nice.calculateTotalCost(ctx.currentLocation, 1)))
                  .append(" (").append(nice.supplier.name).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
