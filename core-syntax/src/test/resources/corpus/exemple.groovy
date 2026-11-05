// Script de construction Gradle — dialecte Groovy (corpus d'exemple celeris).
// Constructions variées pour l'analyseur : gabarits, chaînes triples,
// fermetures nichées, commentaires de bloc et documentation.

plugins {
    id 'java-library'
    id 'com.exemple.conventions' version '1.2.0'
}

/* Un commentaire de bloc
   qui couvre deux lignes. */
def versionExemple = '2.1.0'
group = 'io.celeris.exemple'
description = "Corpus d'exemple du dialecte Groovy (révision ${versionExemple})"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation 'org.jspecify:jspecify:1.0.0'
    testImplementation platform("org.junit:junit-bom:6.1.3")
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.register('resumeExemple') {
    group = 'documentation'
    description = "Assemble le résumé de la révision ${versionExemple}"
    doLast {
        def lignes = new File('RESUME-EXEMPLE.md').readLines()
        println "lignes : ${lignes.size()}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
    maxParallelForks = 2
    testLogging {
        events 'passed', 'failed'
    }
}

/** Compte les déclarations de premier niveau du script d'exemple. */
def compterDeclarationsExemple(File fichier) {
    def contenu = fichier.getText('UTF-8')
    /* une fermeture passée en argument, nichée dans l'appel */
    return contenu.readLines().count { ligne ->
        ligne.trim().startsWith('def ')
    }
}

class EtatExemple {
    int total
    String nom = 'exemple'

    def avance() {
        total++
        println "${nom} : ${total}"
    }

    def remetANeuf() {
        total = 0
    }
}

ext {
    corpusRevision = 'tranche-7'
    gabaritLong = """résumé multi-lignes
de la révision ${versionExemple}"""
    chaîneBrute = '''texte libre
sur plusieurs lignes'''
}

compterDeclarationsExemple(new File('build.gradle'))
