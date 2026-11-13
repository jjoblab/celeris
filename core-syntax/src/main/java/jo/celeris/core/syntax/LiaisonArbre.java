package jo.celeris.core.syntax;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Le porteur de la liaison native du niveau 2 (ADR-020) : la
 * bibliothèque partagée s'y charge si elle existe — construite par
 * {@code natif/construire}, OPTATIVE et hors de {@code check} (la
 * chaîne de build du dépôt reste pure). Absente, le niveau 2 ne se
 * déclare pas : les paquets retombent sur leur repli, aucun
 * consommateur ne change.
 *
 * <p>Les descripteurs natifs vivent ici, jamais ailleurs : l'arbre
 * aplati se matérialise en Java (le jalon de l'ADR-004 — jamais de
 * natif dans le chemin de rendu), l'arbre natif ne survit qu'entre
 * deux éditions de la voie bornée.</p>
 */
final class LiaisonArbre {

    /** Vrai quand la bibliothèque partagée s'est chargée. */
    private static final boolean CHARGEE = charger();

    /** La bibliothèque se charge-t-elle ? */
    static boolean presente() {
        return CHARGEE;
    }

    /** La charge des candidats, dans l'ordre : la propriété de l'hôte,
     *  le répertoire de build du module, la racine du dépôt. */
    private static boolean charger() {
        String imposee = System.getProperty("celeris.natif.lib");
        if (imposee != null) {
            System.load(imposee);
            return true;
        }
        for (Path candidate : new Path[] {
                Path.of("build", "natif", "libliaison.so"),
                Path.of("core-syntax", "build", "natif", "libliaison.so")}) {
            if (Files.exists(candidate)) {
                System.load(candidate.toAbsolutePath().toString());
                return true;
            }
        }
        return false;
    }

    private LiaisonArbre() {
    }

    /** Crée l'analyseur d'un langage — 0 json, 1 python, 2 javascript. */
    static native long parserNew(int langue);

    /** La table des noms de symboles du langage, dans l'ordre. */
    static native String[] symboles(long parser);

    /** Analyse complète ; rend l'arbre, 0 à l'échec. */
    static native long parse(long parser, byte[] source, int longueur);

    /** Re-parse incrémental depuis l'ancien arbre édité ; 0 à l'échec. */
    static native long reparse(long parser, long ancien, byte[] source,
            int longueur);

    /** Édite l'arbre : les bornes et points, avant puis après. */
    static native void treeEdit(long arbre, int debutOctet, int ancienneFin,
            int nouvelleFin, int ligne, int colonne, int ancienneFinLigne,
            int ancienneFinColonne, int nouvelleFinLigne,
            int nouvelleFinColonne);

    /** L'arbre entier aplati, six entiers par nœud en préordre : le
     *  symbole, l'octet de début, l'octet de fin, la ligne, la colonne
     *  (en octets), le nombre d'enfants. */
    static native int[] aplatir(long arbre);

    /** Vrai quand l'arbre porte une erreur de grammaire. */
    static native boolean hasError(long arbre);

    /** Libère l'arbre. */
    static native void treeFree(long arbre);

    /** Libère l'analyseur. */
    static native void parserFree(long parser);
}
