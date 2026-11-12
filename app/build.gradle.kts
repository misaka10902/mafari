plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---- 依赖版本：统一在此声明 ----
// 既用于下方 dependencies，也会写进 BuildConfig.DEP_VERSIONS，供「关于」页的「依赖版本」展示（改版本只需改这里）。
val miuixVersion = "0.9.4"                 // miuix 源码在 ./miuix，由 includeBuild 参与构建，此处为其版本号
val activityComposeVersion = "1.13.0"
val coreKtxVersion = "1.19.1"
val webkitVersion = "1.18.0"
val browserVersion = "1.10.0"
// activity-compose 传递依赖 navigationevent 1.0.0，离线构建需显式指定缓存中的版本
val navigationEventVersion = "1.2.0"
// 构建工具链：Kotlin / Compose Multiplatform / AGP 的版本声明在根 build.gradle.kts（miuix 的在 miuix/gradle/libs.versions.toml），
// Gradle 版本在 gradle/wrapper/gradle-wrapper.properties；升级时请同步这里，仅用于「关于」页展示
val kotlinVersion = "2.4.21"
val composeMultiplatformVersion = "1.12.2"
val agpVersion = "9.5.0"

// 格式：分组|名称|版本;分组|名称|版本…（分组用 ASCII 标识，界面文案由 AboutScreen 映射）
val dependencyVersions = listOf(
    Triple("ui", "miuix-glass", miuixVersion),
    Triple("ui", "Compose Multiplatform", composeMultiplatformVersion),
    Triple("androidx", "Activity Compose", activityComposeVersion),
    Triple("androidx", "Core KTX", coreKtxVersion),
    Triple("androidx", "WebKit", webkitVersion),
    Triple("androidx", "Browser", browserVersion),
    Triple("androidx", "NavigationEvent", navigationEventVersion),
    Triple("build", "Kotlin", kotlinVersion),
    Triple("build", "Android Gradle Plugin", agpVersion),
    Triple("build", "Gradle", gradle.gradleVersion),
).joinToString(";") { (group, name, version) -> "$group|$name|$version" }

android {
    namespace = "com.hyper.browser"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.hyper.browser"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "DEP_VERSIONS", "\"$dependencyVersions\"")
        // 只兼容 64 位 ARM：打包仅保留 arm64-v8a 的 native 库
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        val keystorePath = System.getenv("MAFARI_KEYSTORE")
        if (keystorePath != null && rootProject.file(keystorePath).exists()) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = System.getenv("MAFARI_STORE_PASS")
                keyAlias = System.getenv("MAFARI_KEY_ALIAS")
                keyPassword = System.getenv("MAFARI_KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-glass-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-glass-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:$miuixVersion")

    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.webkit:webkit:$webkitVersion")
    implementation("androidx.browser:browser:$browserVersion")
    implementation("androidx.navigationevent:navigationevent-compose:$navigationEventVersion")
}
