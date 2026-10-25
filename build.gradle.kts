plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.liquidgallery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.liquidgallery"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil 图像加载 (支持 HEIF / Ultra HDR)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Media3 ExoPlayer (用于播放动态照片 Motion Photo 内嵌 MP4 视频流)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // OkHttp (用于 WebDAV PROPFIND/MKCOL/PUT 与主流网盘 REST API 上传)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google ML Kit 端侧人脸检测 (离线本地运行)
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
