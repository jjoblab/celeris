package jo.celeris.android.view;

import jo.celeris.android.view.internal.ScrollPositions.Position;
import jo.celeris.core.text.Document;

import android.graphics.Canvas;

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
     * Dessine l'image entière (chantier C9 — le corps de {@code onDraw}
     * vit chez son collaborateur) : fond, état préparé, couches, chrome
     * — la surface s'attache au canvas et s'en détache, l'état d'image
     * se prépare AVANT le tracé. La LOUPE du dragage (D-16) y
     * photographie la frame : la vue rend dans la bitmap retenue, le
     * canvas reçoit la photographie entière, puis la bulle y sert sa
     * zone agrandie — la loupe montre la VRAIE frame (voile de
     * sélection, curseur, poignées), pas une redite simplifiée.
     *
     * @param canvas le canvas de l'image
     */
    void draw(android.graphics.Canvas canvas) {
        if (vue.magnifier.active()) {
            Canvas toile = vue.magnifier.toileDe(vue.getWidth(), vue.getHeight());
            if (toile != null) {
                // la photographie : la frame s'y rend entière, la vue
                // reçoit la photographie pleine — puis la bulle y sert
                // sa zone agrandie, au-dessus du doigt
                drawImage(toile);
                android.graphics.Bitmap photographie =
                        vue.magnifier.photographie();
                canvas.drawBitmap(photographie, 0f, 0f, null);
                vue.magnifier.trace(canvas, photographie, vue.theme,
                        vue.getWidth(), vue.getHeight(), vue.densite());
                return;
            }
        }
        drawImage(canvas);
    }

    /** Le corps de l'image, sur le canvas donné (vue ou photographie). */
    private void drawImage(android.graphics.Canvas canvas) {
        canvas.drawColor(vue.theme.backgroundColor());
        preparer();
        vue.surface.attach(canvas);
        vue.trace.drawFrame(vue.surface, vue.etatImage);
        vue.diagnostics.surImageTracee();
        vue.pinceauChrome.setFont(vue.metrics.font());
        vue.chrome.trace(vue.surface, vue.pinceauChrome, vue.theme,
                vue.metrics, vue.etatImage.document(),
                vue.projection, vue.session.selection().ranges(),
                vue.largeurGouttiere, vue.scrollX, vue.scrollY,
                vue.curseurClavier.visible());
        vue.surface.detach();
    }

    /**
     * Réinitialise la scène (chantier C9 — le corps des poseurs de
     * session et de langage vit chez son assemblage) : suivi de
     * révision, plafond d'eau de la gouttière (PIEGE-25), diagnostics
     * — le défilement se rabat quand la session change. Le cache ne
     * s'y vide PLUS (C1, feuille des améliorations) : la bascule de
     * stockage vit chez setSession (le parc des mises en page par
     * session), le retour d'onglet traverse ce chemin avec ses
     * rangées INTACTES — c'est le correctif du retard ressenti.
     *
     * @param scroll vrai pour rabattre aussi le défilement
     */
    void reinitialiserScene(boolean scroll) {
        reinitialiserScene(scroll, null);
    }

    /**
     * La variante C2 (feuille des améliorations) : la pose de session
     * ne rabat PLUS le défilement à zéro — la position de l'onglet
     * qui arrive se RESTAURE (parquée sous sa session), le clamp de
     * {@code preparer()} la rabat dans le contenu au premier dessin
     * (un document raccourci en arrière-plan). La position null
     * rabat à zéro comme avant (langue, montage initial).
     *
     * @param scroll   vrai pour toucher au défilement
     * @param position la position restaurée de la session posée,
     *                 null pour la neutre (zéro, zéro)
     */
    void reinitialiserScene(boolean scroll,
            @org.jspecify.annotations.Nullable Position position) {
        vue.revisionSuivie = Long.MIN_VALUE;
        vue.gutterState.reinitialiser();
        vue.diagnostics.reinitialiser();
        if (scroll) {
            if (position != null) {
                vue.scrollX = position.scrollX();
                vue.scrollY = position.scrollY();
            } else {
                vue.scrollX = 0f;
                vue.scrollY = 0f;
            }
        }
        vue.invalidate();
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
        vue.gutterState.setCoulisses(
                vue.gutterMarks.lanes(vue.syntaxe.active()));
        vue.largeurGouttiere = vue.gutterState.suivre(document.lineCount(),
                vue.metrics.charWidth(), vue.densite(), vue.visibleNumbers);
        suivreRevision(document);
        float contentHeight = vue.projection.rowCount()
                * vue.metrics.lineHeight();
        vue.scrollY = ScrollGeometry.clamp(vue.scrollY, contentHeight,
                vue.getHeight());
        vue.scrollX = ScrollGeometry.clamp(vue.scrollX,
                ScrollGeometry.contentWidth(vue.projection, vue.metrics),
                ScrollGeometry.visibleWidth(vue.getWidth(),
                        vue.largeurGouttiere));
        int premiere = ScrollGeometry.firstRow(vue.scrollY,
                vue.metrics.lineHeight());
        int visibles = ScrollGeometry.visibleRows(vue.getHeight(),
                vue.metrics.lineHeight());
        vue.syntaxe.window(vue.projection, premiere, visibles);
        vue.etatImage.beginFrame(document, vue.session.selection(),
                vue.projection, document.revision(), vue.metrics, vue.theme,
                premiere, visibles, vue.largeurGouttiere, vue.scrollX,
                vue.scrollY, vue.getWidth());
        // L'attente de couleur (M7.5, §9) : une publication existe —
        // une rangée sans couleur se comptera comme décolorée.
        vue.etatImage.awaitColor(vue.syntaxe.expectedColor());
        // L'avis de fenêtre (M7.5, §7) : les popups se repositionnent
        // au changement réel — le clignotement ne notifie jamais.
        vue.window.review(vue.scrollX, vue.scrollY, vue.largeurGouttiere,
                vue.metrics.charWidth(), vue.metrics.lineHeight(),
                vue.projection.columns(), vue.getWidth(), vue.getHeight());
        // Le préfetch de viewport (B-03) : la marge du viewport se
        // préchauffe au calme de 150 ms — chaque frame reprogramme au
        // viewport courant, le fling ne paie plus la mesure.
        if (visibles > 0) {
            vue.prefetch.programmer(premiere, visibles);
        }
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
                splice(document);
            }
            vue.revisionSuivie = document.revision();
            // B-05 : la projection s'épisse quand seules les lignes
            // ont bougé (bornes propres, budget et plis inchangés) —
            // jamais un balayage complet des longueurs à chaque
            // frappe ; sinon la construction fraîche.
            if (colonnesVoulues == vue.projection.columns()) {
                vue.projection = vue.projection.splice(document,
                        Math.max(0, vue.derniereEditionPremiereLigne),
                        vue.derniereEditionDeltaLignes);
            } else {
                vue.projection = new VisualProjection(document, vue.folds,
                        colonnesVoulues);
            }
            RowSource habillee = vue.syntaxe.wrap(
                    new DocumentRowSource(document, vue.projection, vue.metrics,
                            colonnesVoulues), document);
            vue.sourceLiee.set(habillee);
            if (recue) {
                // PIEGE-26 : la publication ne rejette pas les mesures —
                // seules les rangées dont les plages ont changé se
                // remplacent ; sans coloration, rien à préserver. Les
                // rangées composées de pli se recomposent entières —
                // le texte plié a pu changer d'une publication à
                // l'autre.
                if (habillee instanceof FoldedRowSource composee) {
                    vue.cache.respan(document.revision(), composee::respan);
                } else if (habillee instanceof SyntaxRowSource coloree) {
                    vue.cache.respan(document.revision(), coloree::respan);
                } else {
                    vue.cache.invalidate(document.revision());
                }
            }
        }
    }

    /**
     * Épisse le cache de rangées au changement de révision (B-01,
     * l'analyse croisée) : une frappe ne rejette plus TOUT le cache —
     * les rangées hors de la zone touchée survivent par identité,
     * re-timbrées à la nouvelle révision. Les bornes viennent de
     * l'écouteur d'annonce (première ligne touchée, décalage de
     * lignes) ; sans bornes fiables (édition composite, rejeu
     * d'annulation en plusieurs pas), l'invalidation totale reste la
     * voie sûre — jamais une épissure aux bornes faussées.
     */
    private void splice(Document document) {
        int premiereLigne = vue.derniereEditionPremiereLigne;
        int deltaLignes = vue.derniereEditionDeltaLignes;
        // Une seule édition continue, bornes propres : épisser. Le
        // décalage de lignes doit réconcilier les deux documents.
        if (premiereLigne >= 0 && premiereLigne != Integer.MAX_VALUE
                && vue.projection.lineCount() + deltaLignes
                        == document.lineCount()) {
            // La zone morte couvre les lignes touchées : de la
            // première ligne de l'édition à la première ligne
            // épargnée (ancienne numérotation). Une FUSION (delta
            // négatif) y compte les lignes avalées : la rangée de la
            // ligne qui disparaît doit mourir avec celle de la ligne
            // fusionnée — sinon le décalage la pose SUR la rangée de
            // la fusion, périmée (texte vide), et la ligne entière
            // disparaît de l'image jusqu'à la frappe suivante (le
            // retour de terrain du 2026-11-20 : « public class
            // MaClasse { » invisible après la remontée du caret).
            // Une insertion ne touche que la ligne coupée — toutes
            // ses tranches meurent sous coupure.
            int premiereRangee = vue.projection.rowOfLine(premiereLigne);
            int ligneApresZone = Math.min(
                    premiereLigne + 1 + Math.max(0, -deltaLignes),
                    vue.projection.lineCount());
            int rangeeFinZone = ligneApresZone == vue.projection.lineCount()
                    ? vue.projection.rowCount()
                    : vue.projection.rowOfLine(ligneApresZone);
            int rangeesMortes = Math.max(1, rangeeFinZone - premiereRangee);
            // Le décalage de rangées : sous coupure, chaque ligne
            // ajoutée ajoute ses tranches — le nombre de rangées de
            // la ligne APRÈS la zone (ancienne numérotation) ne peut
            // pas se lire avant la reconstruction ; le décalage sûr
            // est le delta de lignes (une rangée par ligne sans
            // coupure ; sous coupure la re-mesure rattrape).
            vue.cache.splice(vue.revisionSuivie, document.revision(),
                    premiereRangee, rangeesMortes, deltaLignes,
                    deltaLignes);
        } else {
            vue.cache.invalidate(vue.revisionSuivie);
        }
        vue.derniereEditionPremiereLigne = Integer.MAX_VALUE;
        vue.derniereEditionDeltaLignes = 0;
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
