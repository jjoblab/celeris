// android-host — AGP appliqué ici (accesseurs typés), réglages communs dans la
// convention celeris.android-* ; aucune version n'est écrite dans ce
// fichier (catalogue unique, ADR-011).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    // ADR-011 (décision du propriétaire du 2026-11-11) : publié comme les
    // modules purs — variante release, sources, Javadoc, SBOM, sommes.
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-language"))
}
