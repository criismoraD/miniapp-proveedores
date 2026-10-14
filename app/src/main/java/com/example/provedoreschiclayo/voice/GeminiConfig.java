package com.example.provedoreschiclayo.voice;

import com.example.provedoreschiclayo.BuildConfig;

/**
 * Configuración de las claves de Gemini para el asistente de voz.
 * Mismo esquema (y mismas claves) que la app "Manos a la Obra":
 * se leen de local.properties vía BuildConfig. Si están vacías, el asistente
 * funciona en modo local (motor de consultas sin conexión).
 */
public final class GeminiConfig {
    private GeminiConfig() {}

    public static final String FREE_API_KEY = BuildConfig.GEMINI_FREE_KEY;
    public static final String PAID_API_KEY = BuildConfig.GEMINI_PAID_KEY;

    public static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    public static final String TEXT_MODEL = "gemini-3.5-flash-lite";

    public static boolean hasKeys() {
        return (FREE_API_KEY != null && !FREE_API_KEY.isEmpty())
                || (PAID_API_KEY != null && !PAID_API_KEY.isEmpty());
    }
}
