plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.android.room)
}

android {
    namespace = "com.codebasetemplate"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        applicationId = "com.highsecure.base.code"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    bundle {
        language {
            // Specifies that the app bundle should not support
            // configuration APKs for language resources. These
            // resources are instead packaged with each base and
            // dynamic feature APK.
            enableSplit = false
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
    // Define product flavors
    flavorDimensions.add("root")
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
        }
        create("prod") {

        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Android
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)

    implementation(project(":core:ads"))
    implementation(project(":core:analytics"))
    implementation(project(":core:baseui"))
    implementation(project(":core:config"))
    implementation(project(":core:preference"))
    implementation(project(":core:utilities"))
    implementation(project(":core:billing"))
    implementation(project(":core:dimens"))
    implementation(project(":core:rate"))
    implementation(project(":core:startflow"))

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)


    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.sessions)

    // Google play service
    implementation(libs.play.services.ads)

    // in app
    implementation(libs.billing.ktx)

    /*Lottie animation*/
    implementation(libs.lottie)

    /*gson parse json*/
    implementation(libs.gson)

    implementation(libs.androidx.core.splashscreen)


    //Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit call Api
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)

    implementation(libs.kotlin.reflect)

    // Moshi
    implementation (libs.moshi.kotlin)
    implementation (libs.moshi)
    implementation (libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    // This dependency is downloaded from the Google’s Maven repository.
    // So, make sure you also include that repository in your project's build.gradle file.
    implementation(libs.app.update)

    // For Kotlin users also add the Kotlin extensions library for Play In-App Update:
    implementation(libs.app.update.ktx)

    //Adjust
//    implementation(libs.adjust.android)
//    implementation(libs.installreferrer)
    // Add the following if you are using the Adjust SDK inside web views on your app
//    implementation(libs.sdk.adjust.android.webbridge)
//    implementation(libs.play.services.ads.identifier)
}
