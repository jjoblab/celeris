package jo.celeris.core.syntax;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Chargeur des corpus embarqués (ADR-004) : les fichiers d'exemple vivent
 * dans les ressources de test, lisible par toute classe du module. La
 * réplication déterministe (pour les benchmarks à 50 000 lignes) répète
 * le corpus en renumérotant les déclarations — la graine fixe garantit le
 * même contenu d'un passage à l'autre.
 */
public final class CorpusChargeur {

    private CorpusChargeur() {
    }

    /** Le corpus Java d'exemple (constructions variées, contenu original). */
    public static String exempleJava() {
        return lire("corpus/exemple.java");
    }

    /** Le corpus XML d'exemple. */
    public static String exempleXml() {
        return lire("corpus/exemple.xml");
    }

    /** Le corpus JSON d'exemple. */
    public static String exempleJson() {
        return lire("corpus/exemple.json");
    }

    /** Le corpus Kotlin d'exemple. */
    public static String exempleKotlin() {
        return lire("corpus/exemple.kt");
    }

    /** Le corpus Gradle KTS d'exemple. */
    public static String exempleKts() {
        return lire("corpus/exemple.gradle.kts");
    }

    /** Le corpus Groovy d'exemple. */
    public static String exempleGroovy() {
        return lire("corpus/exemple.groovy");
    }

    /**
     * Réplique un corpus jusqu'à la cible de lignes, en renumérotant les
     * déclarations pour éviter les collisions de noms — déterministe.
     *
     * @param base       le texte du corpus de base
     * @param lignesCible le nombre de lignes visé
     * @return le corpus répliqué, terminé par un saut de ligne
     */
    public static String replique(String base, int lignesCible) {
        StringBuilder sb = new StringBuilder(base.length() * (lignesCible / 10 + 1));
        int tour = 0;
        while (compterLignes(sb) < lignesCible) {
            sb.append(renumerote(base, tour));
            tour++;
        }
        return sb.toString();
    }

    private static String renumerote(String base, int tour) {
        if (tour == 0) {
            return base;
        }
        return base.replace("Exemple", "Exemple" + tour)
                .replace("exemple", "exemple" + tour);
    }

    private static int compterLignes(CharSequence texte) {
        int lignes = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes++;
            }
        }
        return lignes;
    }

    private static String lire(String chemin) {
        try (InputStream flux = CorpusChargeur.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("corpus absent : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("corpus illisible : " + chemin, e);
        }
    }
}
