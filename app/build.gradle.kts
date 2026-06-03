import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---- 自动版本号递增 ----
// 每次构建自动 patch+1, versionCode+1
// 手动改版本：编辑 version.properties 里的 major/minor/patch
val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        load(versionPropsFile.inputStream())
    } else {
        setProperty("major", "2")
        setProperty("minor", "0")
        setProperty("patch", "92")
        setProperty("build", "135")
    }
}

val verMajor = versionProps.getProperty("major", "2").toInt()
val verMinor = versionProps.getProperty("minor", "0").toInt()
val verPatch = versionProps.getProperty("patch", "92").toInt() + 1
val verBuild = versionProps.getProperty("build", "135").toInt() + 1

versionProps.setProperty("patch", verPatch.toString())
versionProps.setProperty("build", verBuild.toString())
versionProps.store(versionPropsFile.outputStream(), "Auto-incremented")

android {
    namespace = "com.timememorial.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.timememorial.app"
        minSdk = 26
        targetSdk = 34
        versionCode = verBuild
        versionName = "$verMajor.$verMinor.$verPatch"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.webkit:webkit:1.12.1")

    // 实时毛玻璃模糊效果
    implementation("com.eightbitlab:realtimeblurview:1.2.1")
}