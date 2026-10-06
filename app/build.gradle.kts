plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) // или alias(libs.plugins.compose.compiler), если вы добавили оба
    alias(libs.plugins.devtools.ksp) // Ссылаемся на алиас из libs.versions.toml
}

android {
    namespace = "com.example.passwordlocker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.passwordlocker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Используем BOM для управления версиями Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Тестовые зависимости
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug зависимости
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Остальные зависимости ---
    implementation("com.google.code.gson:gson:2.11.0")

    // ViewModel + Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Навигация
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Иконки
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Корутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}