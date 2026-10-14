plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.core.baseui"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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
    kotlinOptions {
        jvmTarget = "17"
    }

    flavorDimensions.add("root")

    productFlavors {
        create("dev") {

        }
        create("prod") {

        }
    }

    buildFeatures {
        viewBinding = true
    }


}

dependencies {

    implementation (project(":core:utilities"))
    implementation (project(":core:ads"))
    implementation (project(":core:config"))
    implementation (project(":core:preference"))
    implementation (project(":core:analytics"))
    implementation (project(":core:dimens"))
    implementation (project(":core:billing"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Android
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.androidx.activity.ktx)
    implementation (libs.material)

    // Moshi
    implementation (libs.moshi.kotlin)
    implementation (libs.moshi)
    implementation (libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    //Hilt
    implementation (libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Google play service
    implementation (libs.ads.mobile.sdk)

    // Mediation
    implementation (libs.lottie)

    implementation(libs.billing.ktx)
}

