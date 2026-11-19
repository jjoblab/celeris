package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.InlayHintMark;

import jo.celeris.core.text.Document;

/**
 * Les indices en ligne (inlay hints) de l'éditeur : le satellite
 * transpose le document en indices — le type du « var » et le nom des
 * paramètres muets se lisent en fin de ligne utile ou devant leur
 * argument, à l'encre muette italique, jamais dans le flux (le
 * simulateur de référence porte le même dessin). La vue ne connaît
 * ni langage ni serveur : le moteur décide, la couche trace ; l'édition
 * relance la pose, la couche suit.
 */
public final class InlayHints {

    /** La vue dont la couche d'indices se pose. */
    private final EditorView editor;

    /**
     * Construit le satellite des indices — le document courant y sert
     * sa première lecture : les indices naissent posés, les éditions
     * relancent la pose.
     *
     * @param editor  la vue dont la couche d'indices se pose
     * @param document le document ouvert
     */
    public InlayHints(EditorView editor, Document document) {
        this.editor = editor;
        refresh(document);
    }

    /**
     * Repose les indices du document — l'édition y relance la pose,
     * le contenu neuf efface les indices d'avant ; la bascule des
     * réglages y relit la porte (fermée, la vue retire les indices
     * d'elle-même).
     *
     * @param document le document ouvert
     */
    public void refresh(Document document) {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(document);
        editor.setInlayHints(inlayHints);
    }
}
