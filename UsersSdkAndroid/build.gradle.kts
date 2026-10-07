// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

// Server URL the demo apps (app, barberapp) pass to UsersSdk.init(). One value for both apps,
// exposed to them as BuildConfig.USERS_SDK_BASE_URL.
// Override without editing code:  ./gradlew assembleDebug -PusersSdkBaseUrl=http://192.168.1.122:8080/
// or add  usersSdkBaseUrl=...  to ~/.gradle/gradle.properties.
extra["usersSdkBaseUrl"] = providers.gradleProperty("usersSdkBaseUrl")
    .getOrElse("https://<railway-domain>/")
