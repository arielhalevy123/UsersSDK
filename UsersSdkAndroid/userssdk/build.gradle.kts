plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

// Published coordinates. JitPack serves the artifact as com.github.arielhalevy123:UsersSDK:<git tag>
// and exports the tag in the VERSION environment variable while it builds.
val sdkGroupId = "com.github.arielhalevy123"
val sdkArtifactId = "UsersSDK"
val sdkVersion: String = System.getenv("VERSION")?.removePrefix("v") ?: "1.0.0"

group = sdkGroupId
version = sdkVersion

android {
    namespace = "io.github.arielhalevy123.userssdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Single-variant publishing: consumers get the release AAR plus a sources jar.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Part of the public API: the SDK's fragments extend androidx Fragment and its
    // views use Material components, so consumers need these on their compile classpath.
    api(libs.androidx.appcompat)
    api(libs.material.v1120)

    // Internal implementation details.
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp.v4120)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.view.v204)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = sdkGroupId
                artifactId = sdkArtifactId
                version = sdkVersion

                pom {
                    name.set("UsersSDK")
                    description.set("Android SDK for JWT authentication, user profiles, custom fields and appointments against a UsersSDK server.")
                    url.set("https://github.com/arielhalevy123/UsersSDK")
                    scm {
                        url.set("https://github.com/arielhalevy123/UsersSDK")
                        connection.set("scm:git:https://github.com/arielhalevy123/UsersSDK.git")
                    }
                    developers {
                        developer {
                            id.set("arielhalevy123")
                            name.set("Ariel Halevy")
                        }
                    }
                }
            }
        }
    }
}
