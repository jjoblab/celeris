package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * L'assemblage de l'état d'image (ADR-006, PIEGE-25 et PIEGE-26) : la
 * gouttière AVANT le budget de colonnes (le cycle est cassé), suivi de
 * révision, bornage du défilement, fenêtre visible d'analyse — puis
 * l'avis de fenêtre aux popups (M7.5, §7). Le collaborateur tient la
 * vue et lit son état de paquet : la vue reste mince, l'ordre
 * d'assemblage vit ici, testé par la vue entière au banc.
 */
final class FrameAssembler {

    private final EditorView vue;

    FrameAssembler(EditorView vue) {
        this.vue = vue;
    }

    /**
     * Prépare l'état d'image : gouttière AVANT le budget de colonnes
     * (le cycle est cassé, PIEGE-25), suivi de révision, bornage du
     * défilement et fenêtre visible.
     */
    void preparer() {
        Document document = vue.session.document();
        // La gouttière ne dépend QUE de grandeurs stables : lignes du
        // document, métriques, densité et configuration de coulisse —
        // jamais de la projection visuelle ni d'un état d'analyse
        // (PIEGE-25). Le budget de colonnes la lit à l'image COURANTE.
        vue.gouttiereEtat.setCoulisses(
                vue.marquesGouttiere.coulisses(vue.syntaxe.active()));
        vue.largeurGouttiere = vue.gouttiereEtat.suivre(document.lineCount(),
                vue.metrics.charWidth(), vue.densite(), vue.numerosVisibles);
        suivreRevision(document);
        float hauteurContenu = vue.projection.rowCount()
                * vue.metrics.lineHeight();
        vue.scrollY = ScrollGeometry.clamp(vue.scrollY, hauteurContenu,
                vue.getHeight());
        vue.scrollX = ScrollGeometry.clamp(vue.scrollX,
                ScrollGeometry.largeurContenu(vue.projection, vue.metrics),
                ScrollGeometry.largeurVisible(vue.getWidth(),
                        vue.largeurGouttiere));
        int premiere = ScrollGeometry.firstRow(vue.scrollY,
                vue.metrics.lineHeight());
        int visibles = ScrollGeometry.visibleRows(vue.getHeight(),
                vue.metrics.lineHeight());
        vue.syntaxe.fenetre(vue.projection, premiere, visibles);
        vue.etatImage.beginFrame(document, vue.session.selection(),
                vue.projection, document.revision(), vue.metrics, vue.theme,
                premiere, visibles, vue.largeurGouttiere, vue.scrollX,
                vue.scrollY);
        // L'avis de fenêtre (M7.5, §7) : les popups se repositionnent
        // au changement réel — le clignotement ne notifie jamais.
        vue.fenetre.review(vue.scrollX, vue.scrollY, vue.largeurGouttiere,
                vue.metrics.charWidth(), vue.metrics.lineHeight(),
                vue.projection.columns(), vue.getWidth(), vue.getHeight());
    }

    /**
     * Suit la révision du document : reconstruction de la projection,
     * réhabillage de la source de rangées à la publication, re-span
     * du cache en place quand les plages seules changent (PIEGE-26).
     */
    private void suivreRevision(Document document) {
        int colonnesVoulues = vue.retourALaLigne ? budgetColonnes() : 0;
        // La publication arrive sur le fil de travail : la source se
        // réhabille même à révision constante.
        boolean recue = vue.publicationRecue;
        if (recue) {
            vue.publicationRecue = false;
        }
        if (recue || document.revision() != vue.revisionSuivie
                || colonnesVoulues != vue.projection.columns()) {
            if (document.revision() != vue.revisionSuivie) {
                vue.cache.invalidate(vue.revisionSuivie);
            }
            vue.revisionSuivie = document.revision();
            vue.projection = new VisualProjection(document, vue.plis,
                    colonnesVoulues);
            RowSource habillee = vue.syntaxe.habiller(
                    new DocumentRowSource(document, vue.projection, vue.metrics,
                            colonnesVoulues), document);
            vue.sourceLiee.set(habillee);
            if (recue) {
                // PIEGE-26 : la publication ne rejette pas les mesures —
                // seules les rangées dont les plages ont changé se
                // remplacent ; sans coloration, rien à préserver.
                if (habillee instanceof SyntaxRowSource coloree) {
                    vue.cache.respan(document.revision(), coloree::respan);
                } else {
                    vue.cache.invalidate(document.revision());
                }
            }
        }
    }

    /** Le budget de colonnes de la fenêtre, au moins une colonne (PIEGE-08). */
    private int budgetColonnes() {
        float disponible = vue.getWidth() - vue.largeurGouttiere
                - Math.round(EditorView.MARGE_RETOUR_DP * vue.densite());
        if (disponible <= 0f || vue.metrics.charWidth() <= 0f) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(disponible
                / vue.metrics.charWidth()));
    }
}
