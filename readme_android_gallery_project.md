# Android 液态玻璃智能相册工程指南 (`LiquidGlassGalleryApp`)

本工程按照你指定的优先级（**第一优先级：原生 HDR 激发 + 动态照片解析播放 + 多图拼接/裁切/标记编辑** → **第二优先级：WebDAV 与主流网盘同步 + 端侧 AI 查重与人脸识别** → **第三优先级：按日期分组双指无级缩放网格 + iOS 风格液态玻璃选择栏**）构建。

## 1. 模块架构与优先级对应表

| 优先级 | 核心特性 | 核心类名 / 函数 | 技术实现要点 |
| :--- | :--- | :--- | :--- |
| **P1 (最高)** | 原生 Ultra HDR 激发与广色域渲染 | `HdrAndMotionPhotoEngine.setNativeHdrMode` | 使用 Android 14+ (`UPSIDE_DOWN_CAKE`) `Window.colorMode = ActivityInfo.COLOR_MODE_HDR` 配合 `Bitmap.hasGainmap()` 激发硬件亮部增益图；向下兼容 Display P3 广色域。 |
| **P1 (最高)** | 动态照片 (Motion Photo / Live) 原生解析与播放 | `HdrAndMotionPhotoEngine.parseMotionPhotoVideoOffset` | 解析 JPEG 头部的 XMP 元数据（兼容 `GCamera:MicroVideoOffset` 与 `Container:Directory` MotionPhoto 标准），通过 `RandomAccessFile` 零拷贝定位尾部内嵌 MP4 偏移并用 Media3 `ExoPlayer` 长按流畅播放。 |
| **P1 (最高)** | 多图拼接、自由裁切、画笔标记与马赛克 | `PhotoEditorEngine` / `PhotoCropAndMarkupEditorDialog` | 支持纵向长图、横向拼接、2x2 宫格合成（自动等比缩放与 Center-Crop）；支持归一化矩形裁切、彩色涂鸦画笔与实时像素化 `BitmapShader` 马赛克笔刷。 |
| **P2 (次高)** | WebDAV 协议与主流网盘上传 | `WebDavStorageClient` / `MainstreamCloudDriveClient` | 基于 OkHttp 原生实现 WebDAV `PROPFIND` 探测、`MKCOL` 自动建目录与 `PUT` 二进制流式上传（兼容群晖 NAS、Nextcloud、坚果云、Alist）；同时提供 Google Drive / OneDrive / 阿里云盘 REST 上传网关。 |
| **P2 (次高)** | 端侧 AI 查重 (64-bit DCT pHash) 与人脸识别归类 | `AiGalleryIntelligenceEngine` | 实现 32x32 离散余弦变换 (2D-DCT) 提取左上角 8x8 低频特征生成 64-bit 感知哈希，通过汉明距离 (`<= 8`) 精准识别连拍与重复图；集成 Google ML Kit Face Detection 端侧人脸聚类。 |
| **P3 (基础)** | 按日期分组 + 双指捏合无级缩放相册网格 | `TimelinePinchZoomGrid` | 按拍摄日期 (`yyyy-MM-dd`) 分组并显示当前排数（如 A 日期 20 张相片分 4 排则每排 5 张），通过 `detectTransformGestures` 监听双指缩放手势，在 2~8 列之间实时切换紧凑度。 |
| **P3 (基础)** | iOS 液态玻璃特效选择键 | `LiquidGlassPillBar` + `LIQUID_GLASS_AGSL_SHADER` | 基于 Android 13+ `RuntimeShader` (AGSL) 编写圆角矩形有符号距离场 (`sdRoundedBox`) 边缘透镜折射着色器，与 `RenderEffect.createBlurEffect` 链式组合成 iOS 液态毛玻璃折射高光导航栏。 |

## 2. `app/build.gradle.kts` 依赖配置

在 Android Studio 工程的 `app/build.gradle.kts` 中添加以下依赖即可直接编译运行：

```kotlin
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
}

dependencies {
    // Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil 图像加载 (支持 HEIF / Ultra HDR)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Media3 ExoPlayer (用于播放动态照片内嵌 MP4 视频流)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // OkHttp (用于 WebDAV PROPFIND/MKCOL/PUT 与主流网盘 REST 上传)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google ML Kit 端侧人脸检测 (完全离线可用)
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
```

## 3. `AndroidManifest.xml` 权限与 HDR 窗口配置

在 `AndroidManifest.xml` 中声明相册读取权限、网络权限，并为预览 Activity 开启硬件加速与广色域/HDR 支持：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Android 13+ 相册读取权限与网络上传权限 -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:hardwareAccelerated="true"
        android:label="LiquidGlass Gallery"
        android:supportsRtl="true">

        <!-- android:colorMode="hdr" 开启原生 Ultra HDR 渲染通道 -->
        <activity
            android:name=".LiquidGlassGalleryActivity"
            android:colorMode="hdr"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
