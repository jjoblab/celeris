// cli — exécutable JVM sans Android ; prouve que la chaîne complète se
// construit sans SDK (critère de sortie M1).
plugins {
    id("celeris.java-application-conventions")
}

dependencies {
    implementation(project(":core-text"))
    implementation(project(":core-syntax"))
    implementation(project(":core-language"))
    implementation(project(":core-intelligence"))
    implementation(project(":core-lsp"))
    implementation(project(":core-ai"))
}

application {
    mainClass.set("jo.celeris.cli.Celeris")
}
