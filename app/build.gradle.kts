plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val googleTestAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val googleTestRewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917"
val googleTestAdaptiveBannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"

val adMobAppIdProvider = providers.gradleProperty("SON_HARF_ADMOB_APP_ID")
    .orElse(googleTestAdMobAppId)
val rewardedAdUnitIdProvider = providers.gradleProperty("SON_HARF_ADMOB_REWARDED_ID")
    .orElse(googleTestRewardedAdUnitId)
val bannerAdUnitIdProvider = providers.gradleProperty("SON_HARF_ADMOB_BANNER_ID")
    .orElse(googleTestAdaptiveBannerAdUnitId)

android {
    namespace = "com.sonharf.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonharf.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 18
        versionName = "0.9.2"

        val supabaseUrl = providers.gradleProperty("SON_HARF_SUPABASE_URL")
            .orElse("https://bzdtftzdjtjoqhtcqtxb.supabase.co")
            .get()
        val supabaseKey = providers.gradleProperty("SON_HARF_SUPABASE_KEY")
            .orElse("sb_publishable_e0SbZKPDCfEcRlxaJsXC7g_D9xkaDjf")
            .get()
        val admobAppId = adMobAppIdProvider.get()
        val rewardedAdUnitId = rewardedAdUnitIdProvider.get()
        val bannerAdUnitId = bannerAdUnitIdProvider.get()
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
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}


tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        val appId = adMobAppIdProvider.get()
        val rewardedId = rewardedAdUnitIdProvider.get()
        val bannerId = bannerAdUnitIdProvider.get()

        check(appId.isNotBlank() && appId != googleTestAdMobAppId) {
            "Production release blocked: configure SON_HARF_ADMOB_APP_ID with the production AdMob app ID."
        }
        check(rewardedId.isNotBlank() && rewardedId != googleTestRewardedAdUnitId) {
            "Production release blocked: configure SON_HARF_ADMOB_REWARDED_ID with the production rewarded-ad unit ID."
        }
        check(bannerId.isNotBlank() && bannerId != googleTestAdaptiveBannerAdUnitId) {
            "Production release blocked: configure SON_HARF_ADMOB_BANNER_ID with the production adaptive-banner unit ID."
        }

        val signingValues = listOf(
            providers.gradleProperty("SON_HARF_RELEASE_STORE_FILE")
                .orElse(providers.environmentVariable("SON_HARF_RELEASE_STORE_FILE"))
                .orNull,
            providers.gradleProperty("SON_HARF_RELEASE_STORE_PASSWORD")
                .orElse(providers.environmentVariable("SON_HARF_RELEASE_STORE_PASSWORD"))
                .orNull,
            providers.gradleProperty("SON_HARF_RELEASE_KEY_ALIAS")
                .orElse(providers.environmentVariable("SON_HARF_RELEASE_KEY_ALIAS"))
                .orNull,
            providers.gradleProperty("SON_HARF_RELEASE_KEY_PASSWORD")
                .orElse(providers.environmentVariable("SON_HARF_RELEASE_KEY_PASSWORD"))
                .orNull,
        )
        check(signingValues.all { !it.isNullOrBlank() }) {
            "Production release blocked: release keystore path/password/key alias/key password are required."
        }
    }
}
