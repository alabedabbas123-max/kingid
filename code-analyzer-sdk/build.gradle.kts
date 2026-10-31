plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kingid.codesdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    sourceSets {
        getByName<org.jetbrains.kotlin.gradle.dsl.KotlinSourceSet>("main") {
            java.srcDirs("src/main/java")
        }
    }

    lint {
        disable("MissingPermission", "InvalidPackage")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("androidx.annotation:annotation:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")

    // Kotlin Coroutines — تُستخدم لتشغيل عمليات الفحص في خيوط خلفية.
    // يمكن الاستغناء عنها واستبدالها بـ java.util.concurrent إذا رغبت في تقليل حجم الـ AAR أكثر.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // اختبار
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

android {
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
