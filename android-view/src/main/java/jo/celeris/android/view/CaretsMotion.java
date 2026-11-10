package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.CamelWord;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.Grapheme;

/**
 * Moteur de déplacement des curseurs (D-17, clavier matériel) : les
 * mouvements horizontaux sautent des clusters de graphèmes entiers
 * (ADR-003) et des mots entiers, la montée et la descente se comptent
 * en RANGÉES visuelles — sous coupures (A-18), jamais en lignes — et
 * les variantes sélection étendent la plage l'ancre figée.
 *
 * <p>Les frontières se cherchent par fenêtre croissante sur le
 * document : jamais le texte entier ne se matérialise pour une touche,
 * et un cluster (ou un mot) plus long que la fenêtre ne se coupe
 * jamais.</p>
 *
 * <p>Le déplacement et la sélection par mots suivent les bossages de
 * chameau (A-04) : « monCompteurDeLignes » se parcourt en quatre
 * mots, la frontière de course de majuscules se lit sur le caractère
 * qui la suit — la fenêtre du recul l'embarque d'un caractère.</p>
 */
public final class CaretsMotion {

    private static final int FENETRE_MOTS = 256;

    private CaretsMotion() {
    }

    /**
     * La sélection après l'action de mouvement donnée.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param selection  la sélection courante
     * @param action     l'action clavier demandée
     * @param pageRows   la hauteur d'une page, en rangées
     * @return la nouvelle sélection
     */
    public static MultiSelection move(Document document, VisualProjection projection,
            MultiSelection selection, EditorAction action, int pageRows) {
        if (action == EditorAction.SELECT_ALL) {
            return MultiSelection.of(new SelRange(0, document.length()));
        }
        boolean etend = estSelection(action);
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        for (SelRange plage : selection.ranges()) {
            int vise = caretVise(document, projection, base(action), plage.caret(),
                    pageRows);
            apres.add(etend ? new SelRange(plage.anchor(), vise)
                    : SelRange.caretAt(vise));
        }
        return MultiSelection.of(apres);
    }

    private static boolean estSelection(EditorAction action) {
        return switch (action) {
            case SELECT_LEFT, SELECT_RIGHT, SELECT_UP, SELECT_DOWN, SELECT_WORD_LEFT,
                    SELECT_WORD_RIGHT, SELECT_LINE_START, SELECT_LINE_END -> true;
            default -> false;
        };
    }

    private static EditorAction base(EditorAction action) {
        return switch (action) {
            case SELECT_LEFT -> EditorAction.MOVE_LEFT;
            case SELECT_RIGHT -> EditorAction.MOVE_RIGHT;
            case SELECT_UP -> EditorAction.MOVE_UP;
            case SELECT_DOWN -> EditorAction.MOVE_DOWN;
            case SELECT_WORD_LEFT -> EditorAction.MOVE_WORD_LEFT;
            case SELECT_WORD_RIGHT -> EditorAction.MOVE_WORD_RIGHT;
            case SELECT_LINE_START -> EditorAction.LINE_START;
            case SELECT_LINE_END -> EditorAction.LINE_END;
            default -> action;
        };
    }

    private static int caretVise(Document document, VisualProjection projection,
            EditorAction mouvement, int curseur, int pageRows) {
        return switch (mouvement) {
            case MOVE_LEFT -> clusterPrecedent(document, curseur);
            case MOVE_RIGHT -> clusterSuivant(document, curseur);
            case MOVE_WORD_LEFT -> motPrecedent(document, curseur);
            case MOVE_WORD_RIGHT -> motSuivant(document, curseur);
            case MOVE_UP, PAGE_UP -> rangeeCible(document, projection, curseur,
                    -saut(mouvement, pageRows));
            case MOVE_DOWN, PAGE_DOWN -> rangeeCible(document, projection, curseur,
                    saut(mouvement, pageRows));
            case LINE_START -> debutRangee(document, projection, curseur);
            case LINE_END -> finRangee(document, projection, curseur);
            case DOC_START -> 0;
            case DOC_END -> document.length();
            default -> curseur;
        };
    }

    /**
     * Le saut de rangées du mouvement : une rangée au clavier, une
     * page entière (hauteur visible) à la page.
     */
    private static int saut(EditorAction mouvement, int pageRows) {
        boolean page = mouvement == EditorAction.PAGE_UP
                || mouvement == EditorAction.PAGE_DOWN;
        return page ? Math.max(1, pageRows) : 1;
    }

    /** Le cluster de graphèmes qui suit l'offset (fenêtre croissante). */
    private static int clusterSuivant(Document document, int offset) {
        if (offset >= document.length()) {
            return document.length();
        }
        int window = 32;
        while (true) {
            int end = Math.min(document.length(), offset + window);
            String chunk = document.subText(offset, end);
            int local = Grapheme.nextBoundary(chunk, 0);
            if (local < chunk.length() || end >= document.length()) {
                return offset + local;
            }
            window *= 4;
        }
    }

    /** Le cluster de graphèmes qui précède l'offset (fenêtre croissante). */
    private static int clusterPrecedent(Document document, int offset) {
        if (offset <= 0) {
            return 0;
        }
        int window = 32;
        while (true) {
            int start = Math.max(0, offset - window);
            String chunk = document.subText(start, offset);
            int local = Grapheme.previousBoundary(chunk, chunk.length());
            if (local > 0 || start == 0) {
                return start + local;
            }
            window *= 4;
        }
    }

    /** La frontière de mot suivante, puis saute les blancs de queue. */
    private static int motSuivant(Document document, int offset) {
        int borne = frontiereMotSuivante(document, offset);
        while (borne < document.length() && Character.isWhitespace(document.charAt(borne))) {
            borne = frontiereMotSuivante(document, borne);
        }
        return borne;
    }

    /** La frontière de mot précédente, puis saute les blancs de tête. */
    private static int motPrecedent(Document document, int offset) {
        int borne = frontiereMotPrecedente(document, offset);
        while (borne > 0 && Character.isWhitespace(document.charAt(borne))) {
            borne = frontiereMotPrecedente(document, borne);
        }
        return borne;
    }

    private static int frontiereMotSuivante(Document document, int offset) {
        int window = FENETRE_MOTS;
        while (true) {
            int end = Math.min(document.length(), offset + window);
            String chunk = document.subText(offset, end);
            int local = CamelWord.nextBoundary(chunk, 0);
            if (local < chunk.length() || end >= document.length()) {
                return offset + local;
            }
            window *= 4;
        }
    }

    private static int frontiereMotPrecedente(Document document, int offset) {
        int window = FENETRE_MOTS;
        while (true) {
            int start = Math.max(0, offset - window);
            // Un caractère au-delà du curseur : la fin de course de
            // majuscules (« HTTPServer ») se prononce sur le caractère
            // qui suit la frontière candidate.
            int end = Math.min(document.length(), offset + 1);
            String chunk = document.subText(start, end);
            int local = CamelWord.previousBoundary(chunk, offset - start);
            if (local > 0 || start == 0) {
                return start + local;
            }
            window *= 4;
        }
    }

    /**
     * L'offset de la rangée cible : la colonne visuelle se conserve,
     * bornée à la tranche de la rangée d'arrivée (A-18).
     */
    private static int rangeeCible(Document document, VisualProjection projection,
            int offset, int delta) {
        int rangee = CaretGeometry.rowOfOffset(document, projection, offset);
        int cible = rangee + delta;
        if (cible < 0) {
            return 0;
        }
        if (cible >= projection.rowCount()) {
            return document.length();
        }
        int ligne = projection.firstLineOfRow(cible);
        int ligneCourante = document.lineForOffset(offset);
        int colonne = offset - document.lineStart(ligneCourante);
        int colonnes = projection.columns();
        int visuelle = colonnes > 0 ? colonne % colonnes : colonne;
        int debutTranche = projection.spanStartAtRow(cible);
        int longueurLigne = document.lineEnd(ligne) - document.lineStart(ligne);
        int finTranche = Math.min(projection.spanEndAtRow(cible), longueurLigne);
        int dansTranche = Math.min(visuelle, Math.max(0, finTranche - debutTranche));
        return document.lineStart(ligne) + debutTranche + dansTranche;
    }

    private static int debutRangee(Document document, VisualProjection projection,
            int offset) {
        int rangee = CaretGeometry.rowOfOffset(document, projection, offset);
        int ligne = document.lineForOffset(offset);
        return document.lineStart(ligne) + projection.spanStartAtRow(rangee);
    }

    private static int finRangee(Document document, VisualProjection projection,
            int offset) {
        int rangee = CaretGeometry.rowOfOffset(document, projection, offset);
        int ligne = document.lineForOffset(offset);
        int longueur = document.lineEnd(ligne) - document.lineStart(ligne);
        return document.lineStart(ligne)
                + Math.min(projection.spanEndAtRow(rangee), longueur);
    }
}
