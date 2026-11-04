plugins {
    id("celeris.java-conventions")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.register("saluer") {
    doLast {
        println("Bonjour Gradle KTS")
    }
}
