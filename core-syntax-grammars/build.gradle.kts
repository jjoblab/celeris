// core-syntax-grammars — la voie de grammaires pour la longue traîne
// (ADR-004) : les langages P2 et le reste des P1 se colorent et se
// replient par grammaire déclarative, sans analyseur dédié.
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-syntax"))
    api(project(":core-language"))
}
