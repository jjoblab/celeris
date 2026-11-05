package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Suivi des plis à l'édition (A-15) : le pli manuel survit à l'édition
 * voisine — il se décale quand le document bouge ailleurs, il s'étend
 * quand son corps s'épaissit, il meurt quand sa ligne d'en-tête
 * disparaît ou que son corps se vide.
 *
 * <p>Le remappage suit la monotonie de l'indexation des lignes : les
 * lignes détruites tuent les en-têtes qu'elles portent, les lignes
 * survivantes se décalent du différentiel net de sauts de ligne, le
 * corps d'un pli chevauchant la zone touchée s'étend ou se rétracte du
 * même différentiel. Le rejeu de l'annulation traverse la même voie
 * (PIEGE-07) : l'éditon inverse remappe les plis comme toute autre
 * édition.</p>
 *
 * <p>Pur et testable sans SDK : le document AVANT l'éditon accompagne
 * chaque notification — le suivi garde le même instantané de référence
 * que le coordinateur d'analyse pour ses sites d'édition. Le modèle
 * replié ({@link FoldModel}) reste la seule source de la projection :
 * le suivi ne déplace que ses bornes, jamais l'état replié ; le
 * remappage alloue hors du chemin de dessin, à l'éditon seulement.</p>
 */
public final class FoldTracker {

    private final FoldModel modele;

    /** Construit le suivi sur un modèle de plis vierge. */
    public FoldTracker() {
        this(new FoldModel());
    }

    /**
     * Construit le suivi sur un modèle donné.
     *
     * @param modele le modèle de plis suivi
     */
    public FoldTracker(FoldModel modele) {
        this.modele = modele;
    }

    /** Le modèle de plis suivi — la projection le lit en direct. */
    public FoldModel modele() {
        return modele;
    }

    /**
     * Replie les lignes après l'en-tête donné (délégation au modèle).
     *
     * @param debut la ligne d'en-tête, qui reste visible
     * @param fin   la dernière ligne cachée
     */
    public void fold(int debut, int fin) {
        modele.fold(debut, fin);
    }

    /**
     * Déplie le pli porté par l'en-tête donné (délégation au modèle).
     *
     * @param debut la ligne d'en-tête du pli à déplier
     */
    public void unfold(int debut) {
        modele.unfold(debut);
    }

    /** Oublie tous les plis — changement de document. */
    public void clear() {
        while (!modele.plis().isEmpty()) {
            modele.unfold(modele.plis().get(0).debut());
        }
    }

    /**
     * Remappe les plis après une édition.
     *
     * @param avant    le document AVANT l'éditon
     * @param start    le début de la plage remplacée, dans l'état pré-éditon
     * @param end      la fin (exclusive) de la plage remplacée
     * @param removed  le texte qui occupait la plage
     * @param inserted le texte qui la remplace
     */
    public void surEdition(Document avant, int start, int end, String removed,
            String inserted) {
        if (modele.plis().isEmpty()) {
            return;
        }
        int delta = sautsDeLigne(inserted) - sautsDeLigne(removed);
        int ligne = avant.lineForOffset(start);
        int premiereDecalee;
        int premiereDetruite;
        int derniereDetruite;
        if (start == end) {
            // Insertion pure : rien n'est détruit ; posée en fin de
            // contenu de ligne, elle s'attache après — l'en-tête de la
            // ligne n'y bouge pas.
            premiereDecalee = start == avant.lineEnd(ligne) ? ligne + 1 : ligne;
            premiereDetruite = Integer.MAX_VALUE;
            derniereDetruite = -1;
        } else {
            derniereDetruite = avant.lineForOffset(end - 1);
            premiereDetruite = ligne;
            premiereDecalee = derniereDetruite + 1;
        }
        List<int[]> anciens = new ArrayList<>();
        for (FoldModel.Pli pli : modele.plis()) {
            anciens.add(new int[] {pli.debut(), pli.fin()});
        }
        clear();
        for (int[] ancien : anciens) {
            int[] nouveau = remapper(ancien[0], ancien[1], premiereDecalee,
                    premiereDetruite, derniereDetruite, delta);
            if (nouveau != null && repliable(nouveau[0], nouveau[1])) {
                modele.fold(nouveau[0], nouveau[1]);
            }
        }
    }

    /**
     * Vrai quand la plage remappée se replie : croissante et sans
     * chevauchement avec un pli déjà remappé — le monotonie du
     * remappage le garantit d'ordinaire, la garde reste défensive.
     */
    private boolean repliable(int debut, int fin) {
        if (fin <= debut) {
            return false;
        }
        for (FoldModel.Pli pli : modele.plis()) {
            if (debut <= pli.fin() && pli.debut() <= fin) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remappe un pli : null quand il meurt, ses nouvelles bornes sinon.
     */
    private static int[] remapper(int debut, int fin, int premiereDecalee,
            int premiereDetruite, int derniereDetruite, int delta) {
        if (debut >= premiereDecalee) {
            // l'en-tête survit, décalé : tout le pli suit.
            return new int[] {debut + delta, fin + delta};
        }
        if (debut >= premiereDetruite && debut <= derniereDetruite) {
            // la ligne d'en-tête est détruite : le pli meurt.
            return null;
        }
        int premiereTouchee = Math.min(premiereDecalee, premiereDetruite);
        if (fin < premiereTouchee) {
            // l'édition passe sous le pli : il ne bouge pas.
            return new int[] {debut, fin};
        }
        int nouvelleFin = fin + delta;
        if (nouvelleFin <= debut) {
            // le corps s'est vidé : le pli meurt.
            return null;
        }
        return new int[] {debut, nouvelleFin};
    }

    private static int sautsDeLigne(String texte) {
        int nombre = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                nombre++;
            }
        }
        return nombre;
    }
}
