plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.permabulk.browserenvironment"
  //noinspection GradleDependency
  compileSdk = 34

  defaultConfig {
    applicationId = "com.permabulk.browserenvironment"
    // Horizon OS is Android 14 (API 34)
    minSdk = 34
    //noinspection OldTargetApi,ExpiredTargetSdkVersion
    targetSdk = 34
    versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
    versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    // Quest is arm64 only; skipping other ABIs cuts the APK size a lot
    ndk { abiFilters += "arm64-v8a" }
  }

  // One fixed debug key (committed on purpose) so every CI build installs over the
  // previous one without "signatures do not match" errors. Not a secret: it only
  // signs this sideloaded hobby app.
  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("ci-debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  packaging { resources.excludes.add("META-INF/LICENSE") }

  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.webkit)

  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)

  implementation(libs.meta.spatial.sdk.base)
  implementation(libs.meta.spatial.sdk.vr)
  implementation(libs.meta.spatial.sdk.toolkit)
  implementation(libs.meta.spatial.sdk.compose)
  implementation(libs.meta.spatial.sdk.animation)
}
