// Convention de l'application de démonstration (demo) : mêmes portes
// qualité que les bibliothèques Android ; non publiée (terrain de jeu).

import com.android.build.api.dsl.ApplicationExtension
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

extensions.configure(ApplicationExtension::class.java) {
    namespace = "jo.celeris.${project.name.replace('-', '.')}"
    compileSdk = 36 // ADR-001 : dernier niveau stable vérifié
    defaultConfig {
        minSdk = 26 // ADR-011
        targetSdk = 36 // exigence du magasin (ADR-001)
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

// Mêmes plafonds que les modules purs (ADR-002) ; nominations du
// plafond relevé des bancs (ADR-019).
extensions.configure(CheckstyleExtension::class.java) {
    toolVersion = catalogVersion("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    configDirectory.set(rootProject.file("config/checkstyle"))
}
extensions.configure(PmdExtension::class.java) {
    toolVersion = catalogVersion("pmd")
    ruleSetFiles = rootProject.files("config/pmd/pmd-rules.xml")
    isConsoleOutput = true
}

// Sous AGP, les plugins de qualité ne matérialisent aucune tâche :
// les portes se posent explicitement sur les sources — main, bancs et
// bancs d'appareil (C11, ADR-019) — les mêmes plafonds que les modules
// purs, bloquants.
val sourcePrincipal = fileTree("src/main/java")
val sourceBancs = fileTree("src/test/java")
val sourceBancsAppareil = fileTree("src/androidTest/java")

val checkstyleAndroid = tasks.register<Checkstyle>("checkstyleAndroid") {
    group = "verification"
    description = "Vérifie le style de main, des bancs et des bancs d'appareil (plafonds 600/800, ADR-019)."
    setSource(listOf(sourcePrincipal, sourceBancs, sourceBancsAppareil))
    classpath = files()
}
tasks.named("check") { dependsOn(checkstyleAndroid) }

val pmdAndroid = tasks.register<Pmd>("pmdAndroid") {
    group = "verification"
    description = "Vérifie la complexité du source set principal (plafond 15)."
    setSource(sourcePrincipal)
}
tasks.named("check") { dependsOn(pmdAndroid) }
