import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.zjlovezxy.sharekt"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.zjlovezxy.sharekt"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.4"
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        // debug 包用仓库里带来的固定证书（ci/sharekt-debug.p12.b64，CI 里解码到临时文件），
        // 否则每次 CI 生成的 debug keystore 都不一样，adb install -r 会报签名不匹配，
        // 而且一卸载登录态就没了。
        getByName("debug") {
            val path = System.getenv("SHAREKT_DEBUG_KEYSTORE")
            if (!path.isNullOrBlank() && file(path).exists()) {
                storeFile = file(path)
                storePassword = "sharekt"
                keyAlias = "sharekt"
                keyPassword = "sharekt"
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:auth"))

    implementation(project(":feature:timeline"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:search"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:message"))
    implementation(project(":feature:notification"))
    implementation(project(":feature:publish"))

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.recyclerview)

    testImplementation(libs.junit)
}
