// core-syntax-grammars — la voie de grammaires pour la longue traîne
// (ADR-004) : les langages P2 et le reste des P1 se colorent et se
// replient par grammaire déclarative au format TextMate réel (ADR-014),
// sans analyseur dédié.
plugins {
    id("celeris.java-library-conventions")
    id("celeris.publishing-conventions")
}
dependencies {
    api(project(":core-syntax"))
    api(project(":core-language"))
    // Moteur des expressions des grammaires (ADR-014 : joni, portage
    // Java d'Oniguruma) — détail interne : les types du module ne le
    // laissent pas fuir, implementation suffit, la dépendance ride
    // jusqu'aux consommateurs au runtime par le POM.
    implementation(libs.joni)
    implementation(libs.jcodings)
}
