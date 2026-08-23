import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val generatedSonHarfLogoResDir = layout.buildDirectory.dir("generated/sonHarfLogo/res")
val generateSonHarfLogo = tasks.register("generateSonHarfLogo") {
    val inputFile = layout.projectDirectory.file("src/main/assets/son_harf_brand_logo.b64")
    val outputFile = generatedSonHarfLogoResDir.map { it.file("drawable/son_harf_brand_logo.png") }
    inputs.file(inputFile)
    outputs.file(outputFile)
    doLast {
        val target = outputFile.get().asFile
        target.parentFile.mkdirs()
        val encoded = inputFile.asFile.readText().filterNot { it.isWhitespace() }
        target.writeBytes(Base64.getMimeDecoder().decode(encoded))
    }
}

android {
    namespace = "com.sonharf.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonharf.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "0.8.6"

        val supabaseUrl = providers.gradleProperty("SON_HARF_SUPABASE_URL")
            .orElse("https://bzdtftzdjtjoqhtcqtxb.supabase.co")
            .get()
        val supabaseKey = providers.gradleProperty("SON_HARF_SUPABASE_KEY")
            .orElse("sb_publishable_e0SbZKPDCfEcRlxaJsXC7g_D9xkaDjf")
            .get()
        val admobAppId = providers.gradleProperty("SON_HARF_ADMOB_APP_ID")
            .orElse("ca-app-pub-3940256099942544~3347511713")
            .get()
        val rewardedAdUnitId = providers.gradleProperty("SON_HARF_ADMOB_REWARDED_ID")
            .orElse("ca-app-pub-3940256099942544/5224354917")
            .get()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$rewardedAdUnitId\"")
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
    }

    val releaseStorePath = providers.gradleProperty("SON_HARF_RELEASE_STORE_FILE")
        .orElse(providers.environmentVariable("SON_HARF_RELEASE_STORE_FILE"))
        .getOrNull()
    val releaseStorePassword = providers.gradleProperty("SON_HARF_RELEASE_STORE_PASSWORD")
        .orElse(providers.environmentVariable("SON_HARF_RELEASE_STORE_PASSWORD"))
        .getOrNull()
    val releaseKeyAlias = providers.gradleProperty("SON_HARF_RELEASE_KEY_ALIAS")
        .orElse(providers.environmentVariable("SON_HARF_RELEASE_KEY_ALIAS"))
        .getOrNull()
    val releaseKeyPassword = providers.gradleProperty("SON_HARF_RELEASE_KEY_PASSWORD")
        .orElse(providers.environmentVariable("SON_HARF_RELEASE_KEY_PASSWORD"))
        .getOrNull()

    signingConfigs {
        if (!releaseStorePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    sourceSets.getByName("main").res.srcDir(generatedSonHarfLogoResDir)

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateSonHarfLogo)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.7.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:3.5.1")

    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
