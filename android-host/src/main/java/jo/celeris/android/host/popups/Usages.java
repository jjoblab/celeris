package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.host.R;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.ReferencesView;

import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Les usages du symbole au curseur (la seconde surface portée de la
 * démonstration à l'orchestrateur, 2026-11-14) : le serveur de langage
 * sert les lieux par le fil du protocole ({@code references}), le
 * satellite les transpose en occurrences cliquables — l'audit Android
 * Studio y compte comme l'IDE de référence : l'en-tête porte le compte
 * du SERVEUR (les usages de tous les fichiers), l'occurrence du
 * document ouvert se prend et SAUTE, l'ÉTRANGÈRE vit muette (le nom
 * de son fichier se lit, le lieu ne se visite pas, jamais il ne se
 * devine — l'éditeur embarqué n'ouvre qu'un document). La carte se
 * pose sous le curseur, bornée ; les régimes suivent seuls (frappe,
 * défilement, tap hors cartes, exclusions des menus — le manager tient
 * tout, le satellite déclenche).
 *
 * <p>La frontière tient : la carte ne connaît NI le serveur, NI
 * l'éditeur — le satellite sert d'intermédiaire ; sans symbole au
 * curseur ou sans serveur branché, la carte ne se pose pas (aucune
 * fausse intégration).</p>
 */
final class Usages {

    /** L'extrait d'une occurrence se rogne à cette longueur — la carte
     *  reste lisible, jamais une rangée qui mange la couche. */
    private static final int EXTRAIT_MAX = 48;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final ReferencesView carte;
    /** La session de langage branchée — les références y passent. */
    private @Nullable LanguageSession serveur;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";

    /**
     * Construit les usages sur le coordinateur des popups : la carte
     * naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    Usages(EditorView editeur, TextSession session, PopupManager popups,
            int margePx, PopupPalette palette) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new ReferencesView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.REFERENCES, carte);
    }

    /**
     * Branche la session de langage — les références du symbole y
     * passent au fil du protocole.
     *
     * @param serveur la session démarrée, null pour le silence
     * @param uri     l'uri du document ouvert
     */
    void brancherServeur(@Nullable LanguageSession serveur, String uri) {
        this.serveur = serveur;
        this.uriDocument = uri;
    }

    /** La palette tourne : la carte des usages la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la carte des usages du symbole au curseur : le serveur sert
     * les lieux par le fil du protocole, la réponse se pose sur le fil
     * UI (l'ancre y vit). Sans symbole au curseur ou sans serveur
     * branché, la carte ne se pose pas — aucune fausse intégration.
     */
    void ouvrir() {
        if (serveur == null
                || ContenuPopups.symboleA(session.document(), caret()) == null) {
            return;
        }
        serveur.references(uriDocument, session.document(), caret())
                .whenComplete((lieux, echec) -> editeur.post(
                        () -> poser(lieux)));
    }

    /**
     * Pose la carte des usages : les lieux du document ouvert
     * deviennent des occurrences cliquables, les étrangères muettes —
     * l'en-tête porte le compte du serveur (l'audit Android Studio :
     * l'IDE de référence compte TOUS les usages, jamais le seul
     * document ouvert). La réponse vide ou échue ne pose rien.
     *
     * @param lieux les lieux servis par le serveur
     */
    private void poser(@Nullable List<LieuLsp> lieux) {
        if (lieux == null || lieux.isEmpty()) {
            return;
        }
        Document document = session.document();
        List<ReferencesView.Occurrence> occurrences =
                new ArrayList<>(lieux.size());
        for (LieuLsp lieu : lieux) {
            int offset = offsetDe(lieu, uriDocument, document);
            if (offset >= 0) {
                occurrences.add(new ReferencesView.Occurrence(
                        libelleDe(document, offset), () -> sauter(offset)));
            } else {
                occurrences.add(new ReferencesView.Occurrence(
                        libelleEtrangere(lieu), null));
            }
        }
        carte.setTitre(popups.couche().getContext().getString(
                R.string.titre_usages, lieux.size(),
                ContenuPopups.symboleA(document, caret())));
        carte.setOccurrences(occurrences);
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom, taille(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.REFERENCES,
                placee.x(), placee.y());
    }

    /** Le saut d'usage : la carte se referme, le curseur porte au lieu —
     *  l'éditeur y révèle sa cible (le caret suit sa sélection). */
    private void sauter(int offset) {
        popups.fermer(PopupController.PopupKind.REFERENCES);
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
    }

    /**
     * L'offset du lieu dans le document ouvert — le début de sa plage
     * y vient (la ligne du document, la colonne de la ligne) ; le lieu
     * étranger au document (l'uri ne répond pas) ou sans plage rend
     * —1, jamais deviné.
     *
     * @param lieu         le lieu servi par le serveur
     * @param uriDocument  l'uri du document ouvert
     * @param document     le document ouvert
     * @return l'offset du début de plage, —1 pour l'étranger
     */
    static int offsetDe(LieuLsp lieu, String uriDocument, Document document) {
        if (lieu.plage() == null || !uriDocument.equals(lieu.uri())) {
            return -1;
        }
        int ligne = lieu.plage().debut().ligne();
        if (ligne < 0 || ligne >= document.lineCount()) {
            return -1;
        }
        int colonne = Math.max(0, lieu.plage().debut().colonne());
        int offset = document.lineStart(ligne) + colonne;
        return Math.min(offset, document.length());
    }

    /** Le libellé de l'occurrence ouverte : « L12 · l'extrait de la
     *  ligne, rogné » — le lieu se lit sans quitter la carte. */
    static String libelleDe(Document document, int offset) {
        int ligne = document.lineForOffset(offset);
        String extrait = document.lineText(ligne).strip();
        if (extrait.length() > EXTRAIT_MAX) {
            extrait = extrait.substring(0, EXTRAIT_MAX) + "…";
        }
        return "L" + (ligne + 1) + " · " + extrait;
    }

    /** Le libellé de l'occurrence ÉTRANGÈRE (l'audit Android Studio :
     *  l'IDE de référence montre les usages de tous les fichiers) — le
     *  nom court du fichier du serveur et la ligne se lisent, le lieu
     *  ne se visite pas. */
    static String libelleEtrangere(LieuLsp lieu) {
        String nom = lieu.uri();
        int barre = nom.lastIndexOf('/');
        if (barre >= 0) {
            nom = nom.substring(barre + 1);
        }
        return lieu.plage() == null ? nom
                : nom + " · L" + (lieu.plage().debut().ligne() + 1);
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.REFERENCES);
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
