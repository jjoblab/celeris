// Convention des modules de bibliothèque Android (android-view,
// android-widgets, android-host) : niveaux d'API de l'ADR-001/011, javac
// strict, Error Prone + NullAway, Checkstyle/PMD aux mêmes plafonds que
// les modules purs, lint bloquant sans baseline. Le plugin AGP est
// appliqué par le module via l'alias du catalogue (accesseurs typés).

import com.android.build.api.dsl.LibraryExtension
import jo.celeris.buildlogic.catalogVersion
import jo.celeris.buildlogic.configureAndroidJavac
import jo.celeris.buildlogic.configureJavadoc
import jo.celeris.buildlogic.configureTestPlatform
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension

plugins {
    checkstyle
    pmd
    id("net.ltgt.errorprone")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure(LibraryExtension::class.java) {
    namespace = "jo.celeris.${project.name.replace('-', '.')}"
    compileSdk = 36 // ADR-001 : dernier niveau stable vérifié
    defaultConfig {
        minSdk = 26 // ADR-011 : portée cumulée mesurée ~99 %
    }
    compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    lint {
        // ADR-011 : lint bloquant, zéro baseline.
        abortOnError = true
    }
}

configureAndroidJavac()
configureJavadoc()
configureTestPlatform()

dependencies {
    "compileOnly"(libs.findLibrary("jspecify").get())
    "testImplementation"(libs.findLibrary("jspecify").get())
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
    "errorprone"(libs.findLibrary("errorprone-core").get())
    "errorprone"(libs.findLibrary("nullaway").get())
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.errorprone {
        error("NullAway")
        option("NullAway:AnnotatedPackages", "jo.celeris")
    }
}

// Mêmes plafonds que les modules purs (ADR-002).
extensions.configure(CheckstyleExtension::class.java) {
    toolVersion = catalogVersion("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
extensions.configure(PmdExtension::class.java) {
    toolVersion = catalogVersion("pmd")
    ruleSetFiles = rootProject.files("config/pmd/pmd-rules.xml")
    isConsoleOutput = true
}

// Sous AGP, les plugins de qualité ne matérialisent aucune tâche
// (constat de la tranche 1 M3) : les portes se posent explicitement
// sur le source set principal — les plafonds 600/50 (Checkstyle) et
// la complexité 15 (PMD) deviennent bloquants, comme sur les modules
// purs (ADR-002).
val sourcePrincipal = fileTree("src/main/java")

val checkstyleAndroid = tasks.register<Checkstyle>("checkstyleAndroid") {
    group = "verification"
    description = "Vérifie le style du source set principal (plafonds 600/50)."
    setSource(sourcePrincipal)
    classpath = files()
}
tasks.named("check") { dependsOn(checkstyleAndroid) }

val pmdAndroid = tasks.register<Pmd>("pmdAndroid") {
    group = "verification"
    description = "Vérifie la complexité du source set principal (plafond 15)."
    setSource(sourcePrincipal)
}
tasks.named("check") { dependsOn(pmdAndroid) }
