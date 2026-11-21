plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.javier.eqsound"
    compileSdk = 34
    defaultConfig { applicationId = "com.javier.eqsound"; minSdk = 24; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
