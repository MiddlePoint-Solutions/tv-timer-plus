import org.gradle.kotlin.dsl.ktlintRuleset

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint)
}

dependencies {
    ktlintRuleset(libs.ktlint)
}
