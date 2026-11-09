import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun baseUrlField(value: String, release: Boolean): String {
    if (release && value.isEmpty()) return "\"\""
    val uri = URI(value)
    require(uri.host != null && uri.userInfo == null && uri.query == null &&
        uri.fragment == null && value.endsWith("/")) { "BASE_URL debe ser absoluta y terminar en /" }
    require(uri.scheme == "https" || (!release && uri.scheme == "http")) {
        "Release requiere HTTPS"
    }
    return "\"" + value + "\""
}

android {
    namespace = "com.upchiapas.ricetta"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.upchiapas.ricetta"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", baseUrlField(
                providers.gradleProperty("ricetta.debugBaseUrl")
                    .getOrElse("http://10.0.2.2:8000/"), false))
        }
        release {
            buildConfigField("String", "BASE_URL", baseUrlField(
                providers.gradleProperty("ricetta.releaseBaseUrl").getOrElse(""), true))


            optimization {
                enable = false
            }


        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.all { it.testLogging.events("passed", "failed", "skipped") }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}