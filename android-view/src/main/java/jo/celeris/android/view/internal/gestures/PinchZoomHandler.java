package jo.celeris.android.view.internal.gestures;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.PinchZoom;
import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.internal.access.EditorAccess;

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
public final class PinchZoomHandler {

    /** L'écartement nul ne divise jamais : le plancher de garde. */
    private static final float MIN_GAP = 1e-6f;

    private final EditorAccess acces;

    private float startSize;
    private float startGap;
    private boolean actif;

    /**
     * Construit le collaborateur de la vue donnée — la vue n'est
     * consultée qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire du zoom
     */
    public PinchZoomHandler(EditorAccess acces) {
        this.acces = acces;
    }

    /** Vrai quand un pincement est engagé : le panoramique s'efface. */
    public boolean actif() {
        return actif;
    }

    /**
     * L'engagement du geste : taille et écartement de départ se
     * figent, le panoramique s'arrête — le seuil du détecteur est
     * franchi, le pincement prend la main.
     *
     * @param detecteur le détecteur au franchissement du seuil
     */
    public void surEngagement(ScaleGestureDetector detecteur) {
        actif = true;
        startSize = acces.policeLogique().size();
        startGap = detecteur.getCurrentSpan();
        acces.touches().couperPanoramique();
    }

    /**
     * Un cran du geste : le facteur cumulé — écartement courant sur
     * écartement de départ — mène au palier, le point focal tient.
     *
     * @param detecteur le détecteur au cran courant
     */
    public void surCran(ScaleGestureDetector detecteur) {
        if (!actif || startGap <= MIN_GAP) {
            return;
        }
        surCranCumule(detecteur.getFocusX(), detecteur.getFocusY(),
                detecteur.getCurrentSpan() / startGap);
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
    public void surFin() {
        actif = false;
    }

    /**
     * Un cran au-dessus — la voie du raccourci clavier et de la
     * molette (D-15) : le point central de la vue sert de point focal.
     */
    public void agrandir() {
        setSize(PinchZoom.stepAbove(acces.policeLogique().size()),
                acces.getWidth() / 2f, acces.getHeight() / 2f);
    }

    /** Un cran en dessous, focal au centre de la vue (D-15). */
    public void retrecir() {
        setSize(PinchZoom.stepBelow(acces.policeLogique().size()),
                acces.getWidth() / 2f, acces.getHeight() / 2f);
    }

    /**
     * La taille par défaut — Ctrl + 0 (D-15) : retour au palier de
     * départ, focal au centre.
     *
     * @param tailleParDefaut la police par défaut de l'hôte, en sp
     */
    public void retablir(float tailleParDefaut) {
        setSize(tailleParDefaut, acces.getWidth() / 2f, acces.getHeight() / 2f);
    }

    /**
     * La molette avec Ctrl : chaque cran zoome d'un palier — le point
     * sous le curseur de la souris reste en place (D-15).
     *
     * @param event l'événement de mouvement générique
     * @return vrai quand le cran se consomme
     */
    public boolean surMolette(MotionEvent event) {
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
            setSize(PinchZoom.stepAbove(acces.policeLogique().size()),
                    event.getX(), event.getY());
        } else {
            setSize(PinchZoom.stepBelow(acces.policeLogique().size()),
                    event.getX(), event.getY());
        }
        return true;
    }

    private void setSize(float nouvelleTaille, float focalX, float focalY) {
        if (nouvelleTaille == acces.policeLogique().size()) {
            return;
        }
        float ancienCharWidth = acces.metrics().charWidth();
        float ancienLineHeight = acces.metrics().lineHeight();
        float ancienneGouttiere = acces.largeurGouttiere();
        float ancienScrollX = acces.scrollX();
        float ancienScrollY = acces.scrollY();
        acces.reglagesUniques().setFont(
                new FontSpec(acces.policeLogique().family(), nouvelleTaille));
        // La gouttière dépend de la police : la colonne se lit à la
        // gouttière d'avant, se pose à celle d'après — le point focal
        // ne dérive pas d'un palier à l'autre.
        float nouvelleGouttiere = expectedGutterWidth();
        acces.setScrollX(PinchZoom.scrollXKeepingColumn(focalX, ancienneGouttiere,
                nouvelleGouttiere, ancienScrollX, ancienCharWidth,
                acces.metrics().charWidth()));
        // La gouttière courante suit le palier : le cran suivant du même
        // geste — sans image entre les événements — lit la gouttière de
        // SON image, pas celle d'avant le premier palier.
        acces.setLargeurGouttiere(nouvelleGouttiere);
        acces.setScrollY(PinchZoom.scrollYKeepingRow(focalY, ancienScrollY,
                ancienLineHeight, acces.metrics().lineHeight()));
        clampScroll(nouvelleGouttiere);
        acces.invalidate();
    }

    private void zoomer(float focalX, float focalY, float facteurCumule) {
        float target = PinchZoom.targetSize(startSize, facteurCumule);
        float nouvelleTaille = PinchZoom.steppedSize(acces.policeLogique().size(), target);
        setSize(nouvelleTaille, focalX, focalY);
    }

    /**
     * La largeur de gouttière prévue aux métriques courantes — la
     * variante pure du calcul d'image, servie au zoom avant l'ajustement
     * du défilement (D-15), la même que celle de l'image (PIEGE-25).
     */
    private float expectedGutterWidth() {
        acces.gutterState().setCoulisses(
                acces.gutterMarks().lanes(acces.syntaxe().active()));
        return acces.gutterState().suivre(
                acces.session().document().lineCount(),
                acces.metrics().charWidth(), acces.densite(), acces.visibleNumbers());
    }

    /** Bornage du défilement à la taille du contenu (D-15). */
    private void clampScroll(float gutter) {
        float contentHeight =
                acces.projection().rowCount() * acces.metrics().lineHeight();
        acces.setScrollY(ScrollGeometry.clamp(acces.scrollY(), contentHeight,
                acces.getHeight()));
        acces.setScrollX(ScrollGeometry.clamp(acces.scrollX(),
                ScrollGeometry.contentWidth(acces.projection(), acces.metrics()),
                ScrollGeometry.visibleWidth(acces.getWidth(), gutter)));
    }
}
