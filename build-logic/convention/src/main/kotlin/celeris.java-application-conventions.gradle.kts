// Convention des applications JVM (cli) : mêmes portes qualité que les
// bibliothèques, sans manifeste d'API ni publication (la cli est un
// exécutable, pas un artefact de bibliothèque — ADR-009).

plugins {
    application
    id("celeris.java-conventions")
}
