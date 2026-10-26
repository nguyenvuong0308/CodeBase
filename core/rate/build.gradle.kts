plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.core.rate"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    flavorDimensions.add("root")

    productFlavors {
        create("dev") {}
        create("prod") {}
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    /*animation*/
    implementation(libs.lottie)

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.flexbox)
    implementation(libs.review.ktx)

    implementation(project(":core:baseui"))
    implementation(project(":core:utilities"))

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}