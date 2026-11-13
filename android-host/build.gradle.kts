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
    // L'étage de composition de l'hôte (le déménagement des popups,
    // décision du propriétaire du 2026-11-14 : les fonctionnalités
    // vivent dans les modules de l'éditeur, la démonstration ne teste
    // plus qu'elle-même) : l'orchestration des popups — PopupsEditeur,
    // DocAncre, BordDroit — voit la vue (géométrie et déclencheurs),
    // le kit des popups (cartes et manager) et la frontière LSP.
    api(project(":android-view"))
    api(project(":android-widgets"))
    api(project(":core-lsp"))
}
