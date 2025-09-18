plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.node.js).apply(false)
    alias(libs.plugins.ktlint)
}

dependencies {
    ktlintRuleset(libs.ktlint)
}
