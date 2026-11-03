// Convention de base des modules purs (JVM) : javac strict, Error Prone
// + NullAway, Checkstyle/PMD aux plafonds de l'ADR-002, couverture JaCoCo,
// plateforme de test commune. Les modules Android répliquent ces réglages
// par leurs propres conventions (le plugin java ne s'y applique pas).

import jo.celeris.buildlogic.catalogVersion
import jo.celeris.buildlogic.configureJavadoc
import jo.celeris.buildlogic.configurePureJavac
import jo.celeris.buildlogic.configureTestPlatform
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    checkstyle
    pmd
    jacoco
    id("net.ltgt.errorprone")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// JDK 21 en construction, artefacts de niveau 17, avertissements bloquants
// (ADR-001) ; Javadoc et plateforme de test communes.
configurePureJavac()
configureJavadoc()
configureTestPlatform()

dependencies {
    "compileOnly"(libs.findLibrary("jspecify").get())
    "testImplementation"(libs.findLibrary("jspecify").get())
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
    "testImplementation"(libs.findLibrary("jqwik").get())
    "testImplementation"(libs.findLibrary("archunit-junit5").get())
    "errorprone"(libs.findLibrary("errorprone-core").get())
    "errorprone"(libs.findLibrary("nullaway").get())
}

// Nullité vérifiée dès la première classe (ADR-009 : JSpecify + NullAway).
tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.errorprone {
        error("NullAway")
        option("NullAway:AnnotatedPackages", "jo.celeris")
    }
}

// Checkstyle — plafonds ADR-002 : 600 lignes par fichier, 50 par méthode.
extensions.configure(CheckstyleExtension::class.java) {
    toolVersion = catalogVersion("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}

// PMD — complexité cyclomatique 15 et plafonds de longueur (ADR-002).
extensions.configure(PmdExtension::class.java) {
    toolVersion = catalogVersion("pmd")
    ruleSetFiles = rootProject.files("config/pmd/pmd-rules.xml")
    isConsoleOutput = true
}

// Couverture de lignes >= 90 % sur les modules purs (ADR-010).
extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = catalogVersion("jacoco")
}
tasks.withType<JacocoCoverageVerification>().configureEach {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
