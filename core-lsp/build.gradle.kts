// core-lsp — les réglages communs vivent dans build-logic (ADR-002) ; ce
// fichier ne déclare que la nature du module et ses dépendances.
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-language"))
}
