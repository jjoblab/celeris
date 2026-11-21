package jo.celeris.android.view.internal.frame;

import jo.celeris.android.view.internal.ScrollPositions.Position;
import jo.celeris.core.text.Document;
import jo.celeris.android.view.SemanticRowSource;
import jo.celeris.android.view.DocumentRowSource;
import jo.celeris.android.view.FoldedRowSource;
import jo.celeris.android.view.RowSource;
import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.SyntaxRowSource;
import jo.celeris.android.view.VisualProjection;
import jo.celeris.android.view.internal.access.EditorAccess;

import android.graphics.Canvas;

/**
 * L'assemblage de l'état d'image (ADR-006, PIEGE-25 et PIEGE-26) : la
 * gouttière AVANT le budget de colonnes (le cycle est cassé), suivi de
 * révision, bornage du défilement, fenêtre visible d'analyse — puis
 * l'avis de fenêtre aux popups (M7.5, §7). Le collaborateur tient la
 * vue et lit son état de paquet : la vue reste mince, l'ordre
 * d'assemblage vit ici, testé par la vue entière au banc.
 */
public final class FrameAssembler {

    private final EditorAccess acces;

    public FrameAssembler(EditorAccess acces) {
        this.acces = acces;
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
    public void draw(android.graphics.Canvas canvas) {
        if (acces.magnifier().active()) {
            Canvas toile = acces.magnifier().toileDe(acces.getWidth(), acces.getHeight());
            if (toile != null) {
                // la photographie : la frame s'y rend entière, la vue
                // reçoit la photographie pleine — puis la bulle y sert
                // sa zone agrandie, au-dessus du doigt
                drawImage(toile);
                android.graphics.Bitmap photographie =
                        acces.magnifier().photographie();
                canvas.drawBitmap(photographie, 0f, 0f, null);
                acces.magnifier().trace(canvas, photographie, acces.theme(),
                        acces.getWidth(), acces.getHeight(), acces.densite());
                return;
            }
        }
        drawImage(canvas);
    }

    /** Le corps de l'image, sur le canvas donné (vue ou photographie). */
    private void drawImage(android.graphics.Canvas canvas) {
        canvas.drawColor(acces.theme().backgroundColor());
        preparer();
        acces.surface().attach(canvas);
        acces.trace().drawFrame(acces.surface(), acces.etatImage());
        acces.diagnostics().surImageTracee();
        acces.pinceauChrome().setFont(acces.metrics().font());
        acces.chrome().trace(acces.surface(), acces.pinceauChrome(), acces.theme(),
                acces.metrics(), acces.etatImage().document(),
                acces.projection(), acces.session().selection().ranges(),
                acces.largeurGouttiere(), acces.scrollX(), acces.scrollY(),
                acces.curseurClavier().visible());
        acces.surface().detach();
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
    public void reinitialiserScene(boolean scroll) {
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
    public void reinitialiserScene(boolean scroll,
            @org.jspecify.annotations.Nullable Position position) {
        acces.setRevisionSuivie(Long.MIN_VALUE);
        acces.gutterState().reinitialiser();
        acces.diagnostics().reinitialiser();
        if (scroll) {
            if (position != null) {
                acces.setScrollX(position.scrollX());
                acces.setScrollY(position.scrollY());
            } else {
                acces.setScrollX(0f);
                acces.setScrollY(0f);
            }
        }
        acces.invalidate();
    }

    /**
     * Prépare l'état d'image : gouttière AVANT le budget de colonnes
     * (le cycle est cassé, PIEGE-25), suivi de révision, bornage du
     * défilement et fenêtre visible.
     */
    public void preparer() {
        Document document = acces.session().document();
        // La gouttière ne dépend QUE de grandeurs stables : lignes du
        // document, métriques, densité et configuration de coulisse —
        // jamais de la projection visuelle ni d'un état d'analyse
        // (PIEGE-25). Le budget de colonnes la lit à l'image COURANTE.
        acces.gutterState().setCoulisses(
                acces.gutterMarks().lanes(acces.syntaxe().active()));
        acces.setLargeurGouttiere(acces.gutterState().suivre(document.lineCount(),
                acces.metrics().charWidth(), acces.densite(), acces.visibleNumbers()));
        suivreRevision(document);
        float contentHeight = acces.projection().rowCount()
                * acces.metrics().lineHeight();
        acces.setScrollY(ScrollGeometry.clamp(acces.scrollY(), contentHeight,
                acces.getHeight()));
        acces.setScrollX(ScrollGeometry.clamp(acces.scrollX(),
                ScrollGeometry.contentWidth(acces.projection(), acces.metrics()),
                ScrollGeometry.visibleWidth(acces.getWidth(),
                        acces.largeurGouttiere())));
        int premiere = ScrollGeometry.firstRow(acces.scrollY(),
                acces.metrics().lineHeight());
        int visibles = ScrollGeometry.visibleRows(acces.getHeight(),
                acces.metrics().lineHeight());
        acces.syntaxe().window(acces.projection(), premiere, visibles);
        acces.etatImage().beginFrame(document, acces.session().selection(),
                acces.projection(), document.revision(), acces.metrics(), acces.theme(),
                premiere, visibles, acces.largeurGouttiere(), acces.scrollX(),
                acces.scrollY(), acces.getWidth());
        // L'attente de couleur (M7.5, §9) : une publication existe —
        // une rangée sans couleur se comptera comme décolorée.
        acces.etatImage().awaitColor(acces.syntaxe().expectedColor());
        // L'avis de fenêtre (M7.5, §7) : les popups se repositionnent
        // au changement réel — le clignotement ne notifie jamais.
        acces.window().review(acces.scrollX(), acces.scrollY(), acces.largeurGouttiere(),
                acces.metrics().charWidth(), acces.metrics().lineHeight(),
                acces.projection().columns(), acces.getWidth(), acces.getHeight());
        // Le préfetch de viewport (B-03) : la marge du viewport se
        // préchauffe au calme de 150 ms — chaque frame reprogramme au
        // viewport courant, le fling ne paie plus la mesure.
        if (visibles > 0) {
            acces.prefetch().programmer(premiere, visibles);
        }
    }

    /**
     * Suit la révision du document : reconstruction de la projection,
     * réhabillage de la source de rangées à la publication, re-span
     * du cache en place quand les plages seules changent (PIEGE-26).
     */
    private void suivreRevision(Document document) {
        int colonnesVoulues = acces.retourALaLigne() ? budgetColonnes() : 0;
        // La publication arrive sur le fil de travail : la source se
        // réhabille même à révision constante.
        boolean recue = acces.publicationRecue();
        if (recue) {
            acces.setPublicationRecue(false);
        }
        if (recue || document.revision() != acces.revisionSuivie()
                || colonnesVoulues != acces.projection().columns()) {
            if (document.revision() != acces.revisionSuivie()) {
                splice(document);
            }
            acces.setRevisionSuivie(document.revision());
            // B-05 : la projection s'épisse quand seules les lignes
            // ont bougé (bornes propres, budget et plis inchangés) —
            // jamais un balayage complet des longueurs à chaque
            // frappe ; sinon la construction fraîche.
            if (colonnesVoulues == acces.projection().columns()) {
                acces.setProjection(acces.projection().splice(document,
                        Math.max(0, acces.derniereEditionPremiereLigne()),
                        acces.derniereEditionDeltaLignes()));
            } else {
                acces.setProjection(new VisualProjection(document, acces.folds(),
                        colonnesVoulues));
            }
            RowSource habillee = acces.syntaxe().wrap(
                    new DocumentRowSource(document, acces.projection(), acces.metrics(),
                            colonnesVoulues), document);
            acces.sourceLiee().set(habillee);
            if (recue) {
                respanner(habillee, document);
            }
        }
    }


    /**
     * Le respan du cache à la publication (PIEGE-26) : la publication
     * ne rejette pas les mesures — seules les rangées dont les plages
     * ont changé se remplacent ; sans coloration, rien à préserver.
     * Les rangées composées de pli se recomposent entières — le texte
     * plié a pu changer d'une publication à l'autre. La surcouche
     * sémantique (ADR-0027) se respanne pareil : ses plages raffinées
     * remplacent celles d'avant, la mesure survit.
     */
    private void respanner(RowSource habillee, Document document) {
        if (habillee instanceof SemanticRowSource raffinee) {
            acces.cache().respan(document.revision(), raffinee::respan);
        } else if (habillee instanceof FoldedRowSource composee) {
            acces.cache().respan(document.revision(), composee::respan);
        } else if (habillee instanceof SyntaxRowSource coloree) {
            acces.cache().respan(document.revision(), coloree::respan);
        } else {
            acces.cache().invalidate(document.revision());
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
        int premiereLigne = acces.derniereEditionPremiereLigne();
        int deltaLignes = acces.derniereEditionDeltaLignes();
        // Une seule édition continue, bornes propres : épisser. Le
        // décalage de lignes doit réconcilier les deux documents.
        if (premiereLigne >= 0 && premiereLigne != Integer.MAX_VALUE
                && acces.projection().lineCount() + deltaLignes
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
            int premiereRangee = acces.projection().rowOfLine(premiereLigne);
            int ligneApresZone = Math.min(
                    premiereLigne + 1 + Math.max(0, -deltaLignes),
                    acces.projection().lineCount());
            int rangeeFinZone = ligneApresZone == acces.projection().lineCount()
                    ? acces.projection().rowCount()
                    : acces.projection().rowOfLine(ligneApresZone);
            int rangeesMortes = Math.max(1, rangeeFinZone - premiereRangee);
            // Le décalage de rangées : sous coupure, chaque ligne
            // ajoutée ajoute ses tranches — le nombre de rangées de
            // la ligne APRÈS la zone (ancienne numérotation) ne peut
            // pas se lire avant la reconstruction ; le décalage sûr
            // est le delta de lignes (une rangée par ligne sans
            // coupure ; sous coupure la re-mesure rattrape).
            acces.cache().splice(acces.revisionSuivie(), document.revision(),
                    premiereRangee, rangeesMortes, deltaLignes,
                    deltaLignes);
        } else {
            acces.cache().invalidate(acces.revisionSuivie());
        }
        acces.setDerniereEditionPremiereLigne(Integer.MAX_VALUE);
        acces.setDerniereEditionDeltaLignes(0);
    }

    /** Le budget de colonnes de la fenêtre, au moins une colonne (PIEGE-08). */
    private int budgetColonnes() {
        float disponible = acces.getWidth() - acces.largeurGouttiere()
                - Math.round(EditorAccess.MARGE_RETOUR_DP * acces.densite());
        if (disponible <= 0f || acces.metrics().charWidth() <= 0f) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(disponible
                / acces.metrics().charWidth()));
    }
}
