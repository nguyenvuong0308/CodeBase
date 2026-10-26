plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt) // For Dagger Hilt
/*    alias(libs.plugins.kotlin.parcelize) // For Kotlin Parcelize*/
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "com.core.preference"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}


publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate{
                from(components["release"])
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(project(":core:utilities"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // Google Architecture Component
    implementation (libs.androidx.lifecycle.process)

    //Hilt
    implementation (libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation (libs.androidx.exifinterface)
    implementation(libs.gson)
    implementation (libs.androidx.security.crypto)
}