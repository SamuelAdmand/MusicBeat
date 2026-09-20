import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * Signing details, kept out of the repository in `keystore.properties`
 * (see keystore.properties.example). Absent on a fresh checkout, in which case
 * the release build still runs and simply comes out unsigned rather than
 * failing — only whoever holds the key can produce a shippable APK.
 */
val signing = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val lastfmApiKey: String = (
    localProps.getProperty("LASTFM_API_KEY")
        ?: System.getenv("LASTFM_API_KEY")
        ?: ""
    ).trim()
val lastfmSecret: String = (
    localProps.getProperty("LASTFM_SECRET")
        ?: System.getenv("LASTFM_SECRET")
        ?: ""
    ).trim()

android {
    namespace = "com.music.bitchord"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.samuel.musicbeat"
        // 26 keeps reach wide; real-time blur (RenderEffect) kicks in on API 31+,
        // Haze falls back to a translucent scrim below that.
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.5.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Last.fm credentials are supplied locally and never committed.
        buildConfigField("String", "LASTFM_API_KEY", "\"${lastfmApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "LASTFM_SECRET", "\"${lastfmSecret.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }


    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            applicationId = "com.samuel.musicbeat"
            resValue("string", "app_name", "MusicBeat")
        }
        // OnePlus / OPPO whitelisted package flavor 1: unlocks native Dolby Atmos / Dirac / OReality hardware audio enhancements
        create("qqmusic") {
            dimension = "distribution"
            applicationId = "com.tencent.qqmusic"
            resValue("string", "app_name", "MusicBeat")
        }
        // OnePlus / OPPO whitelisted package flavor 2: unlocks native hardware audio enhancements
        create("kugou") {
            dimension = "distribution"
            applicationId = "com.kugou.android"
            resValue("string", "app_name", "MusicBeat")
        }
        create("dev") {
            dimension = "distribution"
            applicationId = "com.dev.musicbeat"
            resValue("string", "app_name", "MusicBeat Dev")
        }
    }

    signingConfigs {
        val storePath = System.getenv("KEYSTORE_FILE")
            ?: signing.getProperty("storeFile")
        val store = storePath?.let { rootProject.file(it) }
        val sPassword = System.getenv("STORE_PASSWORD")
            ?: System.getenv("KEYSTORE_PASSWORD")
            ?: signing.getProperty("storePassword")
        val kAlias = System.getenv("KEY_ALIAS")
            ?: signing.getProperty("keyAlias")
        val kPassword = System.getenv("KEY_PASSWORD")
            ?: signing.getProperty("keyPassword")

        if (store != null && store.exists() && sPassword != null && kAlias != null && kPassword != null) {
            create("release") {
                storeFile = store
                storePassword = sPassword
                keyAlias = kAlias
                keyPassword = kPassword
            }
        }
    }

    buildTypes {
        release {
            /*
             * Off deliberately. Stream resolution runs YouTube's own player
             * JavaScript through Rhino, and NewPipe, Ktor and
             * kotlinx.serialization all reach for classes reflectively — none
             * of which R8 can see. Shrinking that reliably is a set of keep
             * rules to be written and then proven on a device, because the
             * breakage it causes appears at runtime rather than at build time.
             * Until then, a larger APK that works beats a smaller one that
             * might not. The rules below stay wired up for when it's revisited.
             */
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Null without a keystore to sign with: the build then produces
            // app-release-unsigned.apk instead of failing outright.
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Unit tests run against a stub android.jar whose methods throw
            // rather than return. That is the right default for anything whose
            // behaviour depends on the framework, and wrong for android.util.Log
            // — which [TrackLog] calls on every decision the source layer makes,
            // so a test of that layer fails on the logging rather than on the
            // logic it was written to check.
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}



dependencies {
    // ---- Compose (Material 3) ----
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    // Pinned above the BOM's 1.7.6: [IosOverscroll] uses OverscrollFactory,
    // which that version doesn't have. Newer foundation alongside the BOM's
    // older ui/material3 is a combination Compose supports deliberately —
    // foundation depends on ui, not the reverse — and this exact pairing was
    // already in effect (foundation was reaching 1.10.0 transitively through
    // the liquid-glass library before that dependency was removed).
    implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---- Media playback: Media3 / ExoPlayer ----
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("androidx.media3:media3-common:1.11.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.0")
    // Audio is progressive, but Apple serves its motion artwork as HLS — this
    // is what lets the animated sleeve play it. See CanvasArtworkPlayer.
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")
    // Source modules hand back manifests rather than files, and which kind is
    // the backend's choice, not ours: the Tidal one served `.m3u8` until
    // September 2026 and `.mpd` after it, for the same track and the same
    // request. Without this artifact a DASH manifest is not merely unplayed —
    // DefaultMediaSourceFactory cannot build a source for it, falls back to
    // progressive, and the extractors try to sniff XML as audio
    // (ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED). See withResolvedStreamType.
    implementation("androidx.media3:media3-exoplayer-dash:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // ---- Images: Coil 3 + Palette (dominant colors for the mesh gradient) ----
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // ---- Frosted glass / progressive blur (Telegram-style bars) ----
    implementation("dev.chrisbanes.haze:haze:1.3.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.3.1")


    // ---- Innertube (YouTube Music) client: Ktor + kotlinx.serialization ----
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // HTML parsing for Genius lyrics scraper
    implementation("org.jsoup:jsoup:1.18.3")

    // ---- Discord Rich Presence: the gateway is a WebSocket, so Ktor needs the plugin ----
    implementation("io.ktor:ktor-client-websockets:3.0.3")

    // Used by SpotifyCanvas for binary protobuf parsing
    implementation("com.google.protobuf:protobuf-javalite:4.35.0")

    // ---- Auth/session storage ----
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ---- JS module execution: QuickJS VM for style source plugins ----
    implementation("io.github.dokar3:quickjs-kt-android:1.0.5")

    // ---- Audio tag & cover editing: Native TagLib ----
    implementation("io.github.kyant0:taglib:1.0.6")


    testImplementation("junit:junit:4.13.2")
    // A real HTTP server for the addon tests. The addon protocol is entirely
    // "what does this app send, and what does it do with what comes back", and
    // a hand-rolled fake of the client would be a test of the fake. Pinned to
    // the OkHttp version already on the runtime classpath.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
