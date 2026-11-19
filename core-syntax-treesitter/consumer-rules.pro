# La liaison native s'appelle par NOM depuis son code C : R8 ne doit
# ni la renommer, ni la retirer (méthode de l'éditeur de référence,
# ADR-0025).
-keep class com.itsaky.androidide.treesitter.** {
    *;
}
