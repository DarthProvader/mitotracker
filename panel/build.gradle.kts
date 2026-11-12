import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The panel's authentication value is not part of the repository. Put it in local.properties as
// `mitolight.authPassword=...`, or export MITOLIGHT_AUTH_PASSWORD.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val authPassword: String = localProperties.getProperty("mitolight.authPassword")
    ?: providers.environmentVariable("MITOLIGHT_AUTH_PASSWORD").orNull
    ?: ""

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "mitolight.panel"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        buildConfigField("String", "AUTH_PASSWORD", "\"$authPassword\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    api(project(":protocol"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kable.core)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
