// celeris — l'artefact agrégateur (chantier B1, revue du 2026-11-17) :
// UNE dépendance pour le cas courant. Le module ne porte AUCUN code —
// le package-info documente l'artefact (Javadoc sans classe le
// réclame, AGENT.md) ; les quatre modules du cas courant y sont en
// api : la vue, les widgets, la couche hôte (façade CelerisEditor,
// popups) et la voie de grammaires. Le LSP embarqué (lsp-protocole)
// reste OPTIONNEL — il porte désucrage et règles R8, l'hôte qui n'en
// veut pas ne le paie pas (ADR-0022). La BOM (celeris-bom) aligne
// les versions ; cet agrégateur porte les dépendances.
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    id("celeris.publishing-conventions")
}

dependencies {
    api(project(":android-view"))
    api(project(":android-widgets"))
    api(project(":android-host"))
    api(project(":core-syntax-grammars"))
}
