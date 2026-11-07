package jo.celeris.android.view;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Le collaborateur du zoom au pincement (D-15) : il porte l'état du
 * geste — la taille et l'écartement au départ, l'activité — et
 * applique le palier au facteur cumulé.
 *
 * <p>Le facteur du détecteur est incrémental (PIEGE-24) : le
 * collaborateur le reconstitue cumulé — l'écartement courant divisé
 * par l'écartement au départ du geste — avant de choisir le palier.
 * Le point sous le doigt reste en place : la gouttière se recalcule
 * aux NOUVELLES métriques avant l'ajustement du défilement, et les
 * défilements se bornent à la nouvelle taille du contenu.</p>
 *
 * <p>L'arbitrage avec le panoramique à deux doigts : le panoramique
 * défile jusqu'à ce que le détecteur franchisse son seuil — à
 * l'engagement du pincement, le panoramique s'arrête net, le
 * défilement ne s'applique jamais deux fois, ni par la pan ni par la
 * compensation du point focal.</p>
 */
final class PinchZoomHandler {

    /** L'écartement nul ne divise jamais : le plancher de garde. */
    private static final float ECART_MIN = 1e-6f;

    private final EditorView vue;

    private float tailleAuDepart;
    private float ecartAuDepart;
    private boolean actif;

    /**
     * Construit le collaborateur de la vue donnée — la vue n'est
     * consultée qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire du zoom
     */
    PinchZoomHandler(EditorView vue) {
        this.vue = vue;
    }

    /** Vrai quand un pincement est engagé : le panoramique s'efface. */
    boolean actif() {
        return actif;
    }

    /**
     * L'engagement du geste : taille et écartement de départ se
     * figent, le panoramique s'arrête — le seuil du détecteur est
     * franchi, le pincement prend la main.
     *
     * @param detecteur le détecteur au franchissement du seuil
     */
    void surEngagement(ScaleGestureDetector detecteur) {
        actif = true;
        tailleAuDepart = vue.policeLogique.size();
        ecartAuDepart = detecteur.getCurrentSpan();
        vue.touches.couperPanoramique();
    }

    /**
     * Un cran du geste : le facteur cumulé — écartement courant sur
     * écartement de départ — mène au palier, le point focal tient.
     *
     * @param detecteur le détecteur au cran courant
     */
    void surCran(ScaleGestureDetector detecteur) {
        if (!actif || ecartAuDepart <= ECART_MIN) {
            return;
        }
        surCranCumule(detecteur.getFocusX(), detecteur.getFocusY(),
                detecteur.getCurrentSpan() / ecartAuDepart);
    }

    /**
     * Un cran du geste au facteur cumulé explicite — la voie du banc
     * et de l'hôte (D-15).
     *
     * @param focalX         l'abscisse du point focal, en pixels
     * @param focalY         l'ordonnée du point focal, en pixels
     * @param facteurCumule le facteur cumulé depuis le début du geste
     */
    void surCranCumule(float focalX, float focalY, float facteurCumule) {
        if (!actif) {
            return;
        }
        zoomer(focalX, focalY, facteurCumule);
    }

    /** La fin du geste : le collaborateur se désarme. */
    void surFin() {
        actif = false;
    }

    /**
     * Un cran au-dessus — la voie du raccourci clavier et de la
     * molette (D-15) : le point central de la vue sert de point focal.
     */
    void agrandir() {
        poserTaille(PinchZoom.stepAbove(vue.policeLogique.size()),
                vue.getWidth() / 2f, vue.getHeight() / 2f);
    }

    /** Un cran en dessous, focal au centre de la vue (D-15). */
    void retrecir() {
        poserTaille(PinchZoom.stepBelow(vue.policeLogique.size()),
                vue.getWidth() / 2f, vue.getHeight() / 2f);
    }

    /**
     * La taille par défaut — Ctrl + 0 (D-15) : retour au palier de
     * départ, focal au centre.
     *
     * @param tailleParDefaut la police par défaut de l'hôte, en sp
     */
    void retablir(float tailleParDefaut) {
        poserTaille(tailleParDefaut, vue.getWidth() / 2f, vue.getHeight() / 2f);
    }

    /**
     * La molette avec Ctrl : chaque cran zoome d'un palier — le point
     * sous le curseur de la souris reste en place (D-15).
     *
     * @param event l'événement de mouvement générique
     * @return vrai quand le cran se consomme
     */
    boolean surMolette(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_SCROLL
                || (event.getMetaState() & android.view.KeyEvent.META_CTRL_ON) == 0
                || (event.getSource() & android.view.InputDevice.SOURCE_CLASS_POINTER) == 0) {
            return false;
        }
        float cran = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        if (cran == 0f) {
            return false;
        }
        if (cran > 0f) {
            poserTaille(PinchZoom.stepAbove(vue.policeLogique.size()),
                    event.getX(), event.getY());
        } else {
            poserTaille(PinchZoom.stepBelow(vue.policeLogique.size()),
                    event.getX(), event.getY());
        }
        return true;
    }

    private void poserTaille(float nouvelleTaille, float focalX, float focalY) {
        if (nouvelleTaille == vue.policeLogique.size()) {
            return;
        }
        float ancienCharWidth = vue.metrics.charWidth();
        float ancienLineHeight = vue.metrics.lineHeight();
        float ancienneGouttiere = vue.largeurGouttiere;
        float ancienScrollX = vue.scrollX;
        float ancienScrollY = vue.scrollY;
        vue.setFont(new FontSpec(vue.policeLogique.family(), nouvelleTaille));
        // La gouttière dépend de la police : la colonne se lit à la
        // gouttière d'avant, se pose à celle d'après — le point focal
        // ne dérive pas d'un palier à l'autre.
        float nouvelleGouttiere = largeurGouttierePrevue();
        vue.scrollX = PinchZoom.scrollXKeepingColumn(focalX, ancienneGouttiere,
                nouvelleGouttiere, ancienScrollX, ancienCharWidth,
                vue.metrics.charWidth());
        // La gouttière courante suit le palier : le cran suivant du même
        // geste — sans image entre les événements — lit la gouttière de
        // SON image, pas celle d'avant le premier palier.
        vue.largeurGouttiere = nouvelleGouttiere;
        vue.scrollY = PinchZoom.scrollYKeepingRow(focalY, ancienScrollY,
                ancienLineHeight, vue.metrics.lineHeight());
        bornerDefilement(nouvelleGouttiere);
        vue.invalidate();
    }

    private void zoomer(float focalX, float focalY, float facteurCumule) {
        float cible = PinchZoom.targetSize(tailleAuDepart, facteurCumule);
        float nouvelleTaille = PinchZoom.steppedSize(vue.policeLogique.size(), cible);
        poserTaille(nouvelleTaille, focalX, focalY);
    }

    /**
     * La largeur de gouttière prévue aux métriques courantes — la
     * variante pure du calcul d'image, servie au zoom avant l'ajustement
     * du défilement (D-15), la même que celle de l'image (PIEGE-25).
     */
    private float largeurGouttierePrevue() {
        vue.gouttiereEtat.setCoulisses(
                vue.marquesGouttiere.coulisses(vue.syntaxe.active()));
        return vue.gouttiereEtat.suivre(
                vue.session.document().lineCount(),
                vue.metrics.charWidth(), vue.densite(), vue.numerosVisibles);
    }

    /** Bornage du défilement à la taille du contenu (D-15). */
    private void bornerDefilement(float gouttiere) {
        float hauteurContenu =
                vue.projection.rowCount() * vue.metrics.lineHeight();
        vue.scrollY = ScrollGeometry.clamp(vue.scrollY, hauteurContenu,
                vue.getHeight());
        vue.scrollX = ScrollGeometry.clamp(vue.scrollX,
                ScrollGeometry.largeurContenu(vue.projection, vue.metrics),
                ScrollGeometry.largeurVisible(vue.getWidth(), gouttiere));
    }
}
