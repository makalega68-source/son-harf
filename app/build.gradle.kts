plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.sonharf.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonharf.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.5.0"

        val supabaseUrl = providers.gradleProperty("SON_HARF_SUPABASE_URL")
            .orElse("https://bzdtftzdjtjoqhtcqtxb.supabase.co")
            .get()
        val supabaseKey = providers.gradleProperty("SON_HARF_SUPABASE_KEY")
            .orElse("sb_publishable_e0SbZKPDCfEcRlxaJsXC7g_D9xkaDjf")
            .get()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.7.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-okhttp:3.5.1")

    implementation("com.android.billingclient:billing-ktx:8.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
