// AGP 9 compiles Kotlin itself, but the Kotlin version loaded here overrides the one it bundles,
// which is how every module ends up on the same compiler.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}
