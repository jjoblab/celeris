// android-host — AGP appliqué ici (accesseurs typés), réglages communs dans la
// convention celeris.android-* ; aucune version n'est écrite dans ce
// fichier (catalogue unique, ADR-011).
plugins {
    alias(libs.plugins.android.library)
    id("celeris.android-library-conventions")
    // Publication Android différée à l'arrivée du code correspondant
    // (M2/M3, écart n° 5 du résumé M1) : la convention commune exige le
    // composant « java » du plugin JVM, qu'AGP ne fournit pas.
}
dependencies {
    api(project(":core-language"))
}
