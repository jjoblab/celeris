// android-widgets — AGP appliqué ici (accesseurs typés), réglages communs dans la
// convention celeris.android-* ; aucune version n'est écrite dans ce
// fichier (catalogue unique, ADR-011).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-intelligence"))
}
