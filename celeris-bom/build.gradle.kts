// celeris-bom — la plateforme de versions de la bibliothèque (chantier
// B1, revue du 2026-11-17) : les onze modules publiés à la même
// version. L'hôte écrit
// implementation(platform("jo.celeris:celeris-bom:<version>")) puis
// déclare les modules SANS version — la BOM porte les contraintes, le
// module d'agrégation `celeris` porte les dépendances. Les compagnons
// de banc (serveur-lsp-java, demo) n'y vivent pas : directive du
// propriétaire du 2026-11-18, rien d'eux ne paraît sur JitPack.
// Coordonnées et version portées par le build racine (subprojects).
plugins {
    `java-platform`
    `maven-publish`
}

// Les onze modules de l'éditeur : la contrainte aligne chacun sur la
// version de CE build (celle du build racine).
val modulesEditeur = listOf(
    ":core-text",
    ":core-syntax",
    ":core-syntax-grammars",
    ":core-language",
    ":core-intelligence",
    ":core-lsp",
    ":core-ai",
    ":lsp-protocole",
    ":android-view",
    ":android-widgets",
    ":android-host",
)

dependencies {
    constraints {
        for (module in modulesEditeur) {
            api(project(mapOf("path" to module)))
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("celerisBom") {
            from(components["javaPlatform"])
            artifactId = "celeris-bom"
            pom {
                name.set("celeris-bom")
                description.set(
                    "Plateforme de versions des modules celeris (onze modules de l'éditeur).")
                url.set("https://github.com/jjoblab/celeris")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/jjoblab/celeris.git")
                    developerConnection.set("scm:git:git@github.com:jjoblab/celeris.git")
                    url.set("https://github.com/jjoblab/celeris")
                }
            }
        }
    }
}

