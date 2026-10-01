plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.seekerspark.game"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.seekerspark.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (System.getenv("SPARK_KEYSTORE") != null) {
            create("submission") {
                storeFile = file(System.getenv("SPARK_KEYSTORE"))
                storePassword = System.getenv("SPARK_KEY_PASSWORD")
                keyAlias = "seeker-spark"
                keyPassword = System.getenv("SPARK_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release { isMinifyEnabled = false }
    }
    if (System.getenv("SPARK_KEYSTORE") != null) {
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("submission")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF" }
}
dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.7")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
