plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.marklab.hcelab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marklab.hcelab"
        // HCE em si funciona a partir da API 19, mas 24 evita ramificações
        // de compatibilidade em APIs de cardemulation menos usadas.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation("junit:junit:4.13.2")
}

// Propositalmente SEM Room, Retrofit, Coroutines ou dependências de
// Keystore nesta fase. Elas entram na Fase 2, junto com o código que as
// usa de fato — não antes.
