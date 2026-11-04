package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Propriétés jqwik de la sélection multiple : l'oracle naïf suit le texte
 * ({@code StringBuilder}) ET les sélections d'avant chaque geste — annuler
 * restitue l'un et l'autre exactement (A-13), refaire les rejoue (ADR-003,
 * ADR-010). La projection à travers une édition est monotone et bornée.
 *
 * <p>La randomisation est déterministe, rejouable à l'identique à partir de
 * la graine (exigence de la mutation). L'horloge progresse au-delà de la
 * fenêtre de coalescence entre chaque frappe : chaque geste est une étape
 * propre, l'oracle empile un état par geste.</p>
 */
class SelectionProprietesTest {

    private static final String TEXTE_INITIAL = "alpha beta\ngamma delta\n";

    /**
     * Une suite de frappes à curseurs aléatoires (un ou plusieurs), annulée
     * puis refaite geste par geste : chaque annulation restitue le texte ET
     * la sélection exacts d'avant la frappe, chaque refaire ceux d'après.
     */
    @Property(tries = 120)
    void frappeAnnulee_restitueTexteEtSelection(@ForAll long graine) {
        Random rng = new Random(graine);
        long[] horloge = {0L};
        TextSession session = TextSession.of(TEXTE_INITIAL, () -> horloge[0]);
        StringBuilder miroir = new StringBuilder(TEXTE_INITIAL);

        // États d'avant chaque geste : texte et sélection.
        List<String> textes = new ArrayList<>();
        textes.add(TEXTE_INITIAL);
        List<MultiSelection> selections = new ArrayList<>();
        selections.add(session.selection());
        List<String> textesApres = new ArrayList<>();
        List<MultiSelection> selectionsApres = new ArrayList<>();

        int gestes = rng.nextInt(20) + 1;
        for (int i = 0; i < gestes; i++) {
            horloge[0] += UndoStack.COALESCE_TIMEOUT_MS + rng.nextInt(100);
            int nombreCurseurs = rng.nextInt(3) + 1;
            int[] positions = new int[nombreCurseurs];
            for (int c = 0; c < nombreCurseurs; c++) {
                positions[c] = rng.nextInt(miroir.length() + 1);
            }
            MultiSelection frappee = MultiSelection.caretsAt(positions);
            session.setSelection(frappee);
            String chunk = String.valueOf((char) ('a' + rng.nextInt(26)));
            textes.add(session.text());
            selections.add(session.selection());
            assertTrue(session.type(chunk), "une frappe a lieu (graine " + graine + ")");
            // Oracle naïf : remplacer chaque plage d'origine par le chunk,
            // en ordre du document.
            miroir = remplacePlages(miroir, frappee, chunk);
            assertEquals(miroir.toString(), session.text(),
                    "l'oracle suit la frappe (graine " + graine + ", geste " + i + ")");
            textesApres.add(session.text());
            selectionsApres.add(session.selection());
        }

        // Chaque annulation restitue texte et sélection d'avant (A-13).
        for (int k = gestes; k >= 1; k--) {
            assertTrue(session.undo());
            assertEquals(textes.get(k), session.text(),
                    "texte d'avant le geste (graine " + graine + ", geste " + (k - 1) + ")");
            assertEquals(selections.get(k), session.selection(),
                    "sélection d'avant le geste (graine " + graine + ", geste " + (k - 1) + ")");
        }

        verifierRefaires(session, textesApres, selectionsApres, graine);
    }

    /** Chaque refaire restitue texte et sélection d'après le geste. */
    private static void verifierRefaires(TextSession session, List<String> textesApres,
            List<MultiSelection> selectionsApres, long graine) {
        for (int k = 0; k < textesApres.size(); k++) {
            assertTrue(session.redo());
            assertEquals(textesApres.get(k), session.text(),
                    "texte d'après le geste (graine " + graine + ", geste " + k + ")");
            assertEquals(selectionsApres.get(k), session.selection(),
                    "sélection d'après le geste (graine " + graine + ", geste " + k + ")");
        }
    }

    /**
     * La projection d'une sélection à travers une édition aléatoire est
     * monotone (les extrémités triées restent triées), bornée par la
     * longueur d'après, et préserve la disjointure après normalisation.
     */
    @Property(tries = 200)
    void projectionMonotone_etBornee(@ForAll long graine) {
        Random rng = new Random(graine);
        String texte = texteAleatoire(rng, 40);
        int longueur = texte.length();
        int debut = rng.nextInt(longueur + 1);
        int fin = debut + rng.nextInt(longueur + 1 - debut);
        int retire = fin - debut;
        String insertion = texteAleatoire(rng, 8);
        EditSpan span = new EditSpan(debut, retire, insertion.length());
        int longueurApres = longueur - retire + insertion.length();

        MultiSelection selection = selectionAleatoire(rng, longueur);
        MultiSelection projetee = selection.project(span);
        assertEquals(selection.caretCount() > 0, projetee.caretCount() > 0);

        int precedent = -1;
        for (SelRange plage : projetee.ranges()) {
            assertTrue(plage.start() >= precedent,
                    "les plages restent triées (graine " + graine + ")");
            assertTrue(plage.end() <= longueurApres,
                    "les plages restent bornées (graine " + graine + ")");
            precedent = plage.end();
        }
        // La projection ne crée jamais de recouvrement : les débuts suivent
        // strictement les fins précédentes après normalisation.
        for (int i = 1; i < projetee.ranges().size(); i++) {
            assertTrue(projetee.ranges().get(i).start() > projetee.ranges().get(i - 1).end(),
                    "plages disjointes (graine " + graine + ")");
        }
    }

    /** Remplace chaque plage normalisée par le même texte, à la main. */
    private static StringBuilder remplacePlages(CharSequence miroir,
            MultiSelection frappee, String chunk) {
        StringBuilder resultat = new StringBuilder();
        int curseur = 0;
        for (SelRange plage : frappee.ranges()) {
            resultat.append(miroir, curseur, plage.start());
            resultat.append(chunk);
            curseur = plage.end();
        }
        resultat.append(miroir, curseur, miroir.length());
        return resultat;
    }

    /** Une sélection aléatoire d'une à quatre plages dans les bornes. */
    private static MultiSelection selectionAleatoire(Random rng, int longueur) {
        int nombre = rng.nextInt(4) + 1;
        List<SelRange> plages = new ArrayList<>(nombre);
        for (int i = 0; i < nombre; i++) {
            int a = rng.nextInt(longueur + 1);
            int b = rng.nextInt(longueur + 1);
            plages.add(a <= b ? new SelRange(a, b) : new SelRange(b, a));
        }
        return MultiSelection.of(plages);
    }

    /** Des lettres, espaces et sauts de ligne — jamais de surrogate isolé. */
    private static String texteAleatoire(Random rng, int longueurMax) {
        int longueur = rng.nextInt(longueurMax + 1);
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            int choix = rng.nextInt(12);
            if (choix < 10) {
                sb.append((char) ('a' + rng.nextInt(26)));
            } else if (choix == 10) {
                sb.append(' ');
            } else {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
