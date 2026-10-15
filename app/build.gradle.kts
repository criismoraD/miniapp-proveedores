import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Lee local.properties (fuera de control de versiones) para inyectar las claves
// de Gemini en BuildConfig. Así el código fuente NUNCA contiene secretos.
// Mismo esquema que la app "Manos a la Obra".
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: "").trim()

android {
    namespace = "com.example.provedoreschiclayo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.provedoreschiclayo"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Claves del asistente de voz (Gemini). Si no existen, quedan en ""
        // y el asistente responde con el motor local sin conexión.
        buildConfigField("String", "GEMINI_FREE_KEY", "\"${secret("GEMINI_FREE_KEY")}\"")
        buildConfigField("String", "GEMINI_PAID_KEY", "\"${secret("GEMINI_PAID_KEY")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Cliente WebSocket para la Gemini Live API (conversación por voz en vivo).
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
