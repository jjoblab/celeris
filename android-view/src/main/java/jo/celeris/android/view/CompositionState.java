package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.Grapheme;
import jo.celeris.core.text.SelRange;

import org.jspecify.annotations.Nullable;

/**
 * État de composition IME : la machine à états qui calcule les plans
 * d'édition de la composition, la logique pure de l'IME (ADR-006) —
 * la colle {@link EditorInputConnection} applique, elle ne décide pas.
 *
 * <p>Semantique du curseur des textes composés et commités, vérifiée
 * sur les sources de la plateforme : une position positive est
 * relative à la fin du texte moins un (la valeur 1 pose le curseur
 * juste après le texte), une position nulle ou négative est relative
 * au début. La composition vit sur la plage primaire : une frappe
 * composée remplace la plage composée courante — jamais un append —
 * et le commit consomme la composition, ou la sélection à défaut.</p>
 *
 * <p>Discipline Unicode du registre : les régions de composition et
 * les retours arrière s'alignent aux frontières de clusters de
 * graphèmes — jamais une demi-paire de substitution, jamais un emoji
 * lié par sélecteur amputé. La machine calcule sur des fenêtres
 * croissantes autour de la borne visée : le coût est borné par la
 * longueur du cluster, jamais par le document.</p>
 */
public final class CompositionState {

    private boolean composing;
    private int spanStart;
    private int spanEnd;

    /**
     * Un plan d'édition calculé par la machine : la plage à remplacer,
     * l'insertion, le curseur résultant ({@code -1} : ne pas toucher à
     * la sélection) et l'état de composition qui s'ensuit.
     *
     * @param replaceStart  le début de la plage remplacée, en offsets
     * @param replaceEnd    la fin de la plage remplacée, en offsets
     * @param insertion     le texte inséré
     * @param caret         le curseur résultant, ou -1 pour le laisser
     * @param composingAfter vrai si la composition reste ouverte
     * @param spanStart     le début de la plage composée qui s'ensuit
     * @param spanEnd       la fin de la plage composée qui s'ensuit
     */
    public record Plan(int replaceStart, int replaceEnd, String insertion,
            int caret, boolean composingAfter, int spanStart, int spanEnd) {

        /** Vrai si le plan porte une édition de texte. */
        public boolean hasReplacement() {
            return replaceStart != replaceEnd || !insertion.isEmpty();
        }
    }

    /** Vrai si une composition est ouverte. */
    public boolean isComposing() {
        return composing;
    }

    /** Le début de la plage composée, 0 si aucune composition. */
    public int composingStart() {
        return composing ? spanStart : 0;
    }

    /** La fin de la plage composée, 0 si aucune composition. */
    public int composingEnd() {
        return composing ? spanEnd : 0;
    }

    /**
     * Le plan d'une frappe composée : la plage composée courante se
     * remplace — à défaut, la sélection primaire ouvre la composition.
     *
     * @param document  l'instantané de document courant
     * @param primary   la plage de sélection primaire
     * @param text      le texte composé
     * @param newCursor la position du curseur demandée par l'IME
     * @return le plan à appliquer
     */
    public Plan planComposing(Document document, SelRange primary, String text,
            int newCursor) {
        int debut = composing ? spanStart : primary.start();
        int fin = composing ? spanEnd : primary.end();
        return plan(debut, fin, text, newCursor, true, document);
    }

    /**
     * Le plan d'un commit : la composition se consomme — à défaut la
     * sélection primaire — puis se clôt.
     *
     * @param document  l'instantané de document courant
     * @param primary   la plage de sélection primaire
     * @param text      le texte commité
     * @param newCursor la position du curseur demandée par l'IME
     * @return le plan à appliquer
     */
    public Plan planCommit(Document document, SelRange primary, String text,
            int newCursor) {
        int debut = composing ? spanStart : primary.start();
        int fin = composing ? spanEnd : primary.end();
        return plan(debut, fin, text, newCursor, false, document);
    }

    /**
     * Le plan d'une région de composition : les bornes se limitent au
     * document, s'échangent si inversées, puis s'alignent aux clusters
     * de graphèmes — la région absorbe le cluster entamé. Aucune
     * édition de texte, le curseur ne bouge pas.
     *
     * @param document l'instantané de document courant
     * @param start    le début demandé, en offsets
     * @param end      la fin demandée, en offsets
     * @return le plan à adopter
     */
    public Plan planRegion(Document document, int start, int end) {
        int s = borne(start, document.length());
        int e = borne(end, document.length());
        if (s > e) {
            int permutage = s;
            s = e;
            e = permutage;
        }
        int aligne = debutDeCluster(document, s);
        e = finDeCluster(document, e);
        return new Plan(aligne, aligne, "", -1, true, aligne, e);
    }

    /**
     * Le plan du retour arrière DANS la composition : retire le
     * dernier cluster de la plage composée, entier.
     *
     * @param document l'instantané de document courant
     * @return le plan à appliquer, ou null si la plage est vide
     */
    public @Nullable Plan planClusterBackspace(Document document) {
        if (!composing || spanStart >= spanEnd) {
            return null;
        }
        int fin = spanEnd;
        // Le cluster qui FINIT à la borne : sa frontière de début est
        // strictement avant elle — debutDeCluster à la borne même
        // renverrait la borne (elle est une frontière).
        int debut = debutDeCluster(document, fin - 1);
        return new Plan(debut, fin, "", debut, true, spanStart, debut);
    }

    /**
     * Adopte l'état qui s'ensuit d'un plan appliqué.
     *
     * @param plan le plan appliqué
     */
    public void adopt(Plan plan) {
        composing = plan.composingAfter();
        spanStart = plan.spanStart();
        spanEnd = plan.spanEnd();
    }

    /** Clôt la composition sans toucher au texte. */
    public void finish() {
        composing = false;
        spanStart = 0;
        spanEnd = 0;
    }

    private Plan plan(int debut, int fin, String text, int newCursor,
            boolean composingApres, Document document) {
        int nouvelleFin = debut + text.length();
        int nouvelleLongueur = document.length() - (fin - debut) + text.length();
        int curseur;
        if (newCursor > 0) {
            curseur = nouvelleFin + newCursor - 1;
        } else {
            curseur = debut + newCursor;
        }
        curseur = Math.max(0, Math.min(curseur, nouvelleLongueur));
        int nouveauDebut = composingApres ? debut : 0;
        int nouvelleFinSpan = composingApres ? nouvelleFin : 0;
        return new Plan(debut, fin, text, curseur, composingApres,
                nouveauDebut, nouvelleFinSpan);
    }

    private static int borne(int valeur, long maximum) {
        return (int) Math.max(0, Math.min(valeur, maximum));
    }

    /**
     * La frontière de cluster à ou avant l'offset : l'offset s'il est
     * déjà une frontière, sinon le début du cluster qui le porte. La
     * fenêtre symétrique autour de l'offset double tant que la
     * frontière fuit le bord — le coût est borné par le cluster,
     * jamais par le document. Visibilité paquet : l'entourage IME
     * ({@link SurroundingTexts}) s'aligne par la même machine.
     */
    static int debutDeCluster(Document document, int offset) {
        if (offset <= 0) {
            return 0;
        }
        int fenetre = 64;
        for (;;) {
            int bas = Math.max(0, offset - fenetre);
            int haut = Math.min(document.length(), offset + fenetre);
            int candidate = -1;
            for (int frontiere : frontieresFiables(document, bas, haut)) {
                if (frontiere <= offset) {
                    candidate = frontiere;
                }
            }
            if (candidate >= 0) {
                return candidate;
            }
            if (bas == 0 && haut == document.length()) {
                return 0;
            }
            fenetre *= 2;
        }
    }

    /**
     * La frontière de cluster à ou après l'offset : l'offset s'il est
     * déjà une frontière, sinon la fin du cluster qui le porte.
     * Visibilité paquet : l'entourage IME s'aligne par la même
     * machine.
     */
    static int finDeCluster(Document document, int offset) {
        int longueur = document.length();
        if (offset >= longueur) {
            return longueur;
        }
        int fenetre = 64;
        for (;;) {
            int bas = Math.max(0, offset - fenetre);
            int haut = Math.min(longueur, offset + fenetre);
            for (int frontiere : frontieresFiables(document, bas, haut)) {
                if (frontiere >= offset) {
                    return frontiere;
                }
            }
            if (bas == 0 && haut == longueur) {
                return longueur;
            }
            fenetre *= 2;
        }
    }

    /**
     * Les frontières de graphèmes de la fenêtre, hors des bords
     * coupés : la segmentation d'une fenêtre qui démarre ou s'arrête
     * au milieu d'un cluster y est fausse aux bords — on ne garde que
     * l'intérieur, fiable.
     */
    private static List<Integer> frontieresFiables(Document document, int bas,
            int haut) {
        if (haut <= bas) {
            return List.of();
        }
        List<Integer> brutes = Grapheme.boundaries(document.subText(bas, haut));
        List<Integer> fiables = new ArrayList<>(brutes.size());
        for (int i = 0; i < brutes.size(); i++) {
            if (i == 0 && bas > 0) {
                continue;
            }
            if (i == brutes.size() - 1 && haut < document.length()) {
                continue;
            }
            fiables.add(bas + brutes.get(i));
        }
        return fiables;
    }
}
