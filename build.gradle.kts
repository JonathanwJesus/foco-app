// Arquivo de build do projeto (raiz). Os plugins são aplicados no módulo :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
