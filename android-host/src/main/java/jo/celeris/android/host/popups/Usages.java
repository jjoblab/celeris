package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.host.R;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.ReferencesView;

import jo.celeris.core.intelligence.IndexSymboles;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Les usages du symbole au curseur (la seconde surface portée de la
 * exemple d'intégration à l'orchestrateur, 2026-11-14) : le serveur de langage
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

    private final EditorView editor;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final ReferencesView carte;
    /** La session de langage branchée — les références y passent. */
    private @Nullable LanguageSession server;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";
    /** Le langage du fichier ouvert — le repli local sans serveur y
     *  lit sa syntaxe. */
    private final Supplier<@Nullable LanguageSupport> language;

    /**
     * Construit les usages sur le coordinateur des popups : la carte
     * naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editor la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     * @param language le langage du fichier ouvert, null sans syntaxe
     */
    Usages(EditorView editor, TextSession session, PopupManager popups,
            int margePx, PopupPalette palette,
            Supplier<@Nullable LanguageSupport> language) {
        this.editor = editor;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.language = language;
        this.carte = new ReferencesView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.REFERENCES, carte);
    }

    /**
     * Branche la session de langage — les références du symbole y
     * passent au fil du protocole.
     *
     * @param server la session démarrée, null pour le silence
     * @param uri     l'uri du document ouvert
     */
    void connectServer(@Nullable LanguageSession server, String uri) {
        this.server = server;
        this.uriDocument = uri;
    }

    /** La palette tourne : la carte des usages la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la carte des usages du symbole au curseur : le serveur sert
     * les lieux par le fil du protocole, la réponse se pose sur le fil
     * UI (l'ancre y vit). SANS serveur, le repli local sert les
     * occurrences du document ouvert — l'index du dépôt y lit la
     * syntaxe de la langue courante (la carte ne se tait plus hors
     * LSP : le mot sous le curseur se liste quand même, cliquable,
     * jamais l'inventé). Sans symbole au curseur ni syntaxe, la carte
     * ne se pose pas — aucune fausse intégration.
     */
    void open() {
        if (ContenuPopups.symboleA(session.document(), caret()) == null) {
            return;
        }
        if (server != null) {
            server.references(uriDocument, session.document(), caret())
                    .whenComplete((lieux, echec) -> editor.post(
                            () -> place(lieux)));
            return;
        }
        placeLocalement();
    }

    /** Le repli local : les occurrences du document ouvert par
     *  l'index du dépôt, posées immédiatement — le fil UI tient déjà,
     *  la mesure ne paie qu'une passe d'analyse. */
    private void placeLocalement() {
        String symbole = ContenuPopups.symboleA(session.document(),
                caret());
        LanguageSupport support = language.get();
        if (support == null || support.syntax() == null) {
            return;
        }
        List<Integer> offsets = usagesLocaux(session.document(),
                support.syntax(), symbole);
        if (offsets.isEmpty()) {
            return;
        }
        Document document = session.document();
        List<ReferencesView.Occurrence> occurrences =
                new ArrayList<>(offsets.size());
        for (Integer offset : offsets) {
            int cible = offset;
            occurrences.add(new ReferencesView.Occurrence(
                    libelleDe(document, cible), () -> sauter(cible)));
        }
        carte.setTitre(popups.couche().getContext().getString(
                R.string.celeris_titre_usages, occurrences.size(),
                symbole));
        carte.setOccurrences(occurrences);
        Rect cursor = editor.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                cursor.left, cursor.bottom, size(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.show(PopupController.PopupKind.REFERENCES,
                placee.x(), placee.y());
    }

    /**
     * Les usages du symbole dans le document ouvert, sans serveur :
     * l'index des symboles du dépôt y passe une fois — l'analyse
     * pleine de la syntaxe posée, les occurrences du document s'y
     * lisent ({@code IdentifierScan} n'indexe que les identificateurs
     * : les mots réservés ne se listent jamais). Pur, sans état : la
     * carte locale ne retient rien entre deux ouvertures.
     *
     * @param document le document ouvert
     * @param syntaxe  la syntaxe de la langue du fichier
     * @param symbole  le symbole cherché au curseur
     * @return les offsets des occurrences, croissants
     */
    static List<Integer> usagesLocaux(Document document,
            jo.celeris.core.syntax.LanguageSyntax syntaxe, String symbole) {
        IndexSymboles index = IndexSymboles.vide();
        index.remplacer("", document,
                jo.celeris.core.syntax.SyntaxService.of(syntaxe)
                        .analyseFull(document));
        List<Integer> offsets = new ArrayList<>();
        for (IndexSymboles.Usage usage : index.usages(symbole)) {
            offsets.add(usage.offset());
        }
        java.util.Collections.sort(offsets);
        return offsets;
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
    private void place(@Nullable List<LocationLsp> lieux) {
        if (lieux == null || lieux.isEmpty()) {
            return;
        }
        Document document = session.document();
        List<ReferencesView.Occurrence> occurrences =
                new ArrayList<>(lieux.size());
        for (LocationLsp lieu : lieux) {
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
                R.string.celeris_titre_usages, lieux.size(),
                ContenuPopups.symboleA(document, caret())));
        carte.setOccurrences(occurrences);
        Rect cursor = editor.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                cursor.left, cursor.bottom, size(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.show(PopupController.PopupKind.REFERENCES,
                placee.x(), placee.y());
    }

    /** Le saut d'usage : la carte se referme, le curseur porte au lieu —
     *  l'éditeur y révèle sa cible (le caret suit sa sélection). */
    private void sauter(int offset) {
        popups.close(PopupController.PopupKind.REFERENCES);
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
    static int offsetDe(LocationLsp lieu, String uriDocument, Document document) {
        if (lieu.range() == null || !uriDocument.equals(lieu.uri())) {
            return -1;
        }
        int line = lieu.range().start().line();
        if (line < 0 || line >= document.lineCount()) {
            return -1;
        }
        int column = Math.max(0, lieu.range().start().column());
        int offset = document.lineStart(line) + column;
        return Math.min(offset, document.length());
    }

    /** Le libellé de l'occurrence ouverte : « L12 · l'extrait de la
     *  ligne, rogné » — le lieu se lit sans quitter la carte. */
    static String libelleDe(Document document, int offset) {
        int line = document.lineForOffset(offset);
        String extrait = document.lineText(line).strip();
        if (extrait.length() > EXTRAIT_MAX) {
            extrait = extrait.substring(0, EXTRAIT_MAX) + "…";
        }
        return "L" + (line + 1) + " · " + extrait;
    }

    /** Le libellé de l'occurrence ÉTRANGÈRE (l'audit Android Studio :
     *  l'IDE de référence montre les usages de tous les fichiers) — le
     *  nom court du fichier du serveur et la ligne se lisent, le lieu
     *  ne se visite pas. */
    static String libelleEtrangere(LocationLsp lieu) {
        String nom = lieu.uri();
        int barre = nom.lastIndexOf('/');
        if (barre >= 0) {
            nom = nom.substring(barre + 1);
        }
        return lieu.range() == null ? nom
                : nom + " · L" + (lieu.range().start().line() + 1);
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre size() {
        return popups.size(PopupController.PopupKind.REFERENCES);
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> ranges = session.selection().ranges();
        return ranges.isEmpty() ? 0 : ranges.get(0).caret();
    }
}
