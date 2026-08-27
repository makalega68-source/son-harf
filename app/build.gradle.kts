import java.security.MessageDigest

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
        val bannerAdUnitId = providers.gradleProperty("SON_HARF_ADMOB_BANNER_ID")
            .orElse("ca-app-pub-3940256099942544/6300978111")
            .get()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$rewardedAdUnitId\"")
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$bannerAdUnitId\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Keep the GLB as a directly readable APK asset for Filament/SceneView.
    androidResources {
        noCompress += "glb"
    }
}

val eveAssetPath = "src/main/assets/models/eve/eve.glb"
val eveAssetExpectedSize = 4_870_220L
val eveAssetExpectedSha256 = "0c68ac4c4f5475332fac77ccb9bda4bb08bd202a5d596114552e37ab27d6c39e"

val verifyEveAsset by tasks.registering {
    group = "verification"
    description = "Fail-fast verification for the accepted rigged Eve GLB."
    val assetFile = layout.projectDirectory.file(eveAssetPath).asFile

    doLast {
        if (!assetFile.isFile) {
            throw GradleException("FATAL: $eveAssetPath is missing. Refusing to build a fake/fallback mascot APK.")
        }
        if (assetFile.length() != eveAssetExpectedSize) {
            throw GradleException(
                "FATAL: Eve GLB size mismatch. Expected $eveAssetExpectedSize bytes, found ${assetFile.length()} bytes.",
            )
        }

        val header = assetFile.inputStream().use { input -> ByteArray(12).also { bytes ->
            if (input.read(bytes) != bytes.size) throw GradleException("FATAL: Eve GLB header is truncated.")
        } }
        val magicOk = header[0] == 'g'.code.toByte() && header[1] == 'l'.code.toByte() &&
            header[2] == 'T'.code.toByte() && header[3] == 'F'.code.toByte()
        val version = (header[4].toInt() and 0xff) or
            ((header[5].toInt() and 0xff) shl 8) or
            ((header[6].toInt() and 0xff) shl 16) or
            ((header[7].toInt() and 0xff) shl 24)
        if (!magicOk || version != 2) {
            throw GradleException("FATAL: Eve asset is not a valid GLB 2.0 file.")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val actualSha = assetFile.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (!actualSha.equals(eveAssetExpectedSha256, ignoreCase = true)) {
            throw GradleException(
                "FATAL: Eve GLB SHA-256 mismatch. Expected $eveAssetExpectedSha256, found $actualSha.",
            )
        }
        logger.lifecycle(">> [PASS] Eve GLB verified: ${assetFile.length()} bytes, SHA-256 $actualSha")
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyEveAsset)
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

    // Real-time glTF/GLB rendering on top of Google Filament. No 2D/MP4 mascot fallback.
    implementation("io.github.sceneview:sceneview:4.31.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.7.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:3.5.1")

    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
