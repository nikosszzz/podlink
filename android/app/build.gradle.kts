import java.util.Properties

val appVersionName = rootProject.extra["appVersionName"] as String
val appVersionCode = rootProject.extra["appVersionCode"] as Int

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localPropsFile = rootProject.file("local.properties")
val props = Properties().apply {
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use(::load)
    }
}

val releaseSigningAvailable = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
).all { props[it]?.toString()?.isNotBlank() == true }

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

android {
    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(props["RELEASE_STORE_FILE"] as String)
                storePassword = props["RELEASE_STORE_PASSWORD"] as String
                keyAlias = props["RELEASE_KEY_ALIAS"] as String
                keyPassword = props["RELEASE_KEY_PASSWORD"] as String
            }
        }
    }

    namespace = "dev.podlink"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.podlink"
        minSdk = 33
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(
                if (releaseSigningAvailable) "release" else "debug"
            )
        }
        debug {
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    sourceSets {
        getByName("main") {
            res.directories += "src/main/res-apple"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenapibypass)
    implementation(libs.shevery.api)
    implementation(libs.shevery.provider)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
