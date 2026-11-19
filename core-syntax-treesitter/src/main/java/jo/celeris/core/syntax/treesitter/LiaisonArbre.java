package jo.celeris.core.syntax.treesitter;

import com.itsaky.androidide.treesitter.TSInputEdit;
import com.itsaky.androidide.treesitter.TSLanguage;
import com.itsaky.androidide.treesitter.TSNode;
import com.itsaky.androidide.treesitter.TSPoint;
import com.itsaky.androidide.treesitter.TSTree;
import com.itsaky.androidide.treesitter.TSTreeCursor;
import com.itsaky.androidide.treesitter.TreeSitter;

/**
 * Le porteur de la liaison native du niveau 2 (ADR-020, révisé par
 * l'ADR-0025) : la liaison AndroidIDE vit par Maven — le cœur
 * ({@code TreeSitter.loadLibrary()}) puis les grammaires (leurs blocs
 * statiques, via {@link GrammaireNatif}). Sur JVM sans Android, le
 * chargement échoue proprement : {@link #presente()} rend false, les
 * fabriques rendent null, les bancs natifs se sautent — les paquets
 * servent leur repli déclaré.
 *
 * <p>Les descripteurs natifs vivent ici, jamais ailleurs : l'arbre
 * aplati se matérialise en Java (le jalon de l'ADR-004 — jamais de
 * natif dans le chemin de rendu), l'arbre natif ne survit qu'entre
 * deux éditions de la voie bornée. Les positions de la liaison sont
 * en unités UTF-16 doublées (« JS units ») : elles se divisent par
 * deux au passage — l'arbre aplati et les éditions portent des
 * offsets CARACTÈRES du modèle du dépôt (ADR-0025).</p>
 */
final class LiaisonArbre {

    /** Vrai quand la liaison s'est chargée — faux sur JVM sans Android. */
    private static final boolean CHARGEE = charger();

    /** La liaison se charge-t-elle ? */
    static boolean presente() {
        return CHARGEE;
    }

    /** Charge le cœur de la liaison — les grammaires suivent leurs
     *  blocs statiques ; l'échec (JVM sans Android) rend false. */
    private static boolean charger() {
        try {
            TreeSitter.loadLibrary();
            return true;
        } catch (Throwable echec) {
            return false;
        }
    }

    private LiaisonArbre() {
    }

    /** L'arbre entier aplati, six entiers par nœud en préordre : la
     *  position du nom de symbole dans la table du langage, le début,
     *  la fin (exclusifs), la ligne, la colonne, le nombre d'enfants
     *  — tout en CARACTÈRES du modèle. La matérialisation Java
     *  reconstruit la hiérarchie de la pile des enfants. */
    static int[] aplatir(TSTree arbre, TSLanguage langue) {
        String[] noms = nomsDeLangue(langue);
        java.util.List<Integer> tampon = new java.util.ArrayList<>(6 * 1024);
        TSTreeCursor curseur = TSTreeCursor.create(arbre.getRootNode());
        boolean descendre = true;
        while (descendre) {
            TSNode noeud = curseur.getCurrentNode();
            tampon.add(indexDeNom(noms, noeud.getGrammarType()));
            tampon.add(noeud.getStartByte() / 2);
            tampon.add(noeud.getEndByte() / 2);
            TSPoint point = noeud.getStartPoint();
            tampon.add(point.getRow());
            tampon.add(point.getColumn() / 2);
            tampon.add(noeud.getChildCount());
            if (curseur.gotoFirstChild()) {
                continue;
            }
            while (!curseur.gotoNextSibling()) {
                if (!curseur.gotoParent()) {
                    descendre = false;
                    break;
                }
            }
        }
        curseur.close();
        int[] resultat = new int[tampon.size()];
        for (int i = 0; i < resultat.length; i++) {
            resultat[i] = tampon.get(i);
        }
        return resultat;
    }

    /** La table des noms de symboles du langage, dans l'ordre. */
    static String[] nomsDeLangue(TSLanguage langue) {
        int nombre = langue.getSymbolCount();
        String[] noms = new String[nombre];
        for (int i = 0; i < nombre; i++) {
            noms[i] = langue.getSymbolName(i);
        }
        return noms;
    }

    /** L'index d'un nom de symbole dans la table — l'inconnu reste
     *  dehors (sorte identificateur). */
    private static int indexDeNom(String[] noms, String nom) {
        for (int i = 0; i < noms.length; i++) {
            if (noms[i].equals(nom)) {
                return i;
            }
        }
        return noms.length;
    }

    /** L'édition incrémentale : bornes et points avant/après, en
     *  CARACTÈRES du modèle — la liaison veut ses unités doublées. */
    static TSInputEdit edition(int debut, int ancienneFin, int nouvelleFin,
            int ligne, int colonne, int ancienneFinLigne,
            int ancienneFinColonne, int nouvelleFinLigne,
            int nouvelleFinColonne) {
        return TSInputEdit.create(debut * 2, ancienneFin * 2,
                nouvelleFin * 2, TSPoint.create(ligne, colonne * 2),
                TSPoint.create(ancienneFinLigne, ancienneFinColonne * 2),
                TSPoint.create(nouvelleFinLigne, nouvelleFinColonne * 2));
    }
}
