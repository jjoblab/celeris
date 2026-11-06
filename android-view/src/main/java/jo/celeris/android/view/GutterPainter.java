package jo.celeris.android.view;

import jo.celeris.core.syntax.TokenKind;

import org.jspecify.annotations.Nullable;

/**
 * Couche de gouttière : les numéros de lignes, alignés à droite dans le
 * champ de la gouttière (D-01 — numéros absolus), et les marques de la
 * coulisse — chevrons de pli (D-07) et marques d'erreur (D-02).
 *
 * <p>La gouttière défile verticalement avec le contenu (chaque numéro
 * suit sa rangée) mais jamais horizontalement : son abscisse ne dépend
 * pas du défilement horizontal. Les numéros passent par la projection
 * (PIEGE-04) — la rangée d'un pli porte le numéro de son en-tête — et
 * les étiquettes mémoïsées n'allouent rien en régime établi
 * (PIEGE-15). Les marques vivent dans la coulisse à gauche du champ de
 * numéros : le chevron replié pointe à droite, le chevron pliable
 * pointe vers le bas, la marque d'erreur se trace à l'encre d'erreur
 * du thème.</p>
 */
public final class GutterPainter extends PainterLayer {

    /** La respiration par défaut de chaque côté du champ de numéros, en pixels. */
    public static final float MARGE_PAR_DEFAUT = 8f;

    /** La source des marques courantes, consultée à chaque image. */
    @FunctionalInterface
    public interface MarksSource {

        /**
         * Les marques de l'image à venir.
         *
         * @return les marques fondues, {@link GutterMarks#VIDE} sans
         *         analyse
         */
        GutterMarks marques();
    }

    /** La largeur courante de la coulisse, servie à chaque image. */
    @FunctionalInterface
    public interface LaneSource {

        /**
         * La largeur de la coulisse des marqueurs, en pixels (0 sans
         * marques).
         *
         * @return la largeur courante
         */
        float laneWidth();
    }

    private final GutterNumbers numeros = new GutterNumbers();
    private final MarksSource marques;
    private final float marge;
    private final LaneSource coulisse;
    private boolean numerosVisibles = true;

    /**
     * Construit la couche de gouttière à respiration par défaut, sans
     * marques.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public GutterPainter(BrushFactory brushes) {
        this(brushes, MARGE_PAR_DEFAUT);
    }

    /**
     * Construit la couche de gouttière à respiration donnée, sans
     * marques.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param padding la respiration de chaque côté du champ, en pixels
     */
    public GutterPainter(BrushFactory brushes, float padding) {
        this(brushes, padding, () -> GutterMarks.VIDE, () -> 0f);
    }

    /**
     * Construit la couche de gouttière avec coulisse de marqueurs.
     *
     * @param brushes    la fabrique de pinceaux (la couche prend le sien)
     * @param padding    la respiration de chaque côté du champ, en pixels
     * @param marques    la source des marques, consultée à chaque image
     * @param markerLane la largeur courante de la coulisse des
     *                   marqueurs, en pixels (0 sans marques) — servie à
     *                   chaque image, la police peut changer
     */
    public GutterPainter(BrushFactory brushes, float padding, MarksSource marques,
            LaneSource markerLane) {
        super(brushes);
        this.marge = padding;
        this.marques = marques;
        this.coulisse = markerLane;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().gutterTextColor();
    }

    /**
     * Affiche ou masque les numéros de lignes (D-01) : masqués, la
     * gouttière ne trace plus d'étiquette — la coulisse des marqueurs
     * (plis, erreurs) reste entière.
     *
     * @param visibles vrai pour tracer les numéros (défaut)
     */
    public void setNumerosVisibles(boolean visibles) {
        numerosVisibles = visibles;
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        EditorMetrics metrics = frame.metrics();
        float charWidth = metrics.charWidth();
        float largeurGouttiere = frame.gutterWidth();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        float baseY = metrics.rowBaseline(premiere) - frame.scrollY();
        float hauteur = metrics.lineHeight();
        GutterMarks marquesImage = marques.marques();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            // A-18 : le numéro ne se pose que sur la première tranche
            // d'une ligne coupée — les rangées de continuation restent
            // muettes.
            if (frame.projection().spanIndexAtRow(rangee) != 0) {
                continue;
            }
            int ligne = frame.projection().firstLineOfRow(rangee);
            float y = baseY + (rangee - premiere) * hauteur;
            float largeurCoulisse = Math.max(0f, coulisse.laneWidth());
            if (largeurCoulisse > 0f) {
                tracerMarques(surface, marquesImage, ligne, y, hauteur,
                        largeurCoulisse, frame);
            }
            if (!numerosVisibles) {
                continue;
            }
            int numero = ligne + 1;
            String etiquette = numeros.label(numero);
            float x = GutterGeometry.numberX(etiquette.length(), largeurGouttiere,
                    charWidth, marge);
            surface.drawText(etiquette, x, y, brush());
        }
    }

    /**
     * Trace les marques d'une ligne dans la coulisse : la marque
     * d'erreur (D-02) puis le chevron de pli (D-07) — la ligne peut
     * porter les deux — puis les marques d'intégration (D-03, D-04,
     * D-05).
     */
    private void tracerMarques(DrawSurface surface, GutterMarks marques, int ligne,
            float y, float hauteur, float coulisse, FrameState frame) {
        float centreX = coulisse / 2f;
        float centreY = y - hauteur / 4f;
        if (marques.erreur(ligne)) {
            brush().setColor(frame.theme().syntaxe().couleur(TokenKind.ERROR));
            float demi = Math.min(coulisse, hauteur) / 6f;
            surface.drawRect(centreX - demi, centreY - demi,
                    centreX + demi, centreY + demi, brush());
        }
        if (marques.pliable(ligne)) {
            tracerPli(surface, marques, ligne, centreX, centreY, coulisse, frame);
        }
        tracerIntegration(surface, marques, ligne, y, hauteur, coulisse, frame);
    }

    /** Le chevron de pli (D-07) : replié regarde à droite, pliable vers le bas. */
    private void tracerPli(DrawSurface surface, GutterMarks marques, int ligne,
            float centreX, float centreY, float coulisse, FrameState frame) {
        float ouverture = coulisse / 4f;
        brush().setColor(frame.theme().gutterTextColor());
        if (marques.replie(ligne)) {
            // chevron replié : la pointe regarde à droite
            surface.drawLine(centreX - ouverture / 2f, centreY - ouverture,
                    centreX + ouverture / 2f, centreY, brush());
            surface.drawLine(centreX + ouverture / 2f, centreY,
                    centreX - ouverture / 2f, centreY + ouverture, brush());
        } else {
            // chevron pliable : la pointe regarde vers le bas
            surface.drawLine(centreX - ouverture, centreY - ouverture / 2f,
                    centreX, centreY + ouverture / 2f, brush());
            surface.drawLine(centreX, centreY + ouverture / 2f,
                    centreX + ouverture, centreY - ouverture / 2f, brush());
        }
    }

    /**
     * Les marques d'intégration (D-03, D-04, D-05) : la barre de
     * modification VCS au bord gauche — modifiée à l'encre de
     * sélection, ajoutée à l'encre de chaîne —, le point d'arrêt au
     * centre de la coulisse — plein et rouge posé, creux et neutre
     * désactivé —, le repère d'exécution par-dessus : la flèche de la
     * ligne courante à l'encre du curseur, le chevron discret des
     * lignes de pile.
     */
    private void tracerIntegration(DrawSurface surface, GutterMarks marques, int ligne,
            float y, float hauteur, float coulisse, FrameState frame) {
        tracerBarreVcs(surface, marques, ligne, y, hauteur, coulisse, frame);
        float centreX = coulisse / 2f;
        float centreY = y - hauteur / 4f;
        tracerArret(surface, marques, ligne, centreX, centreY, coulisse, hauteur, frame);
        tracerExecution(surface, marques, ligne, centreX, centreY, coulisse, frame);
    }

    /** La barre de modification VCS (D-03), plein rang de hauteur. */
    private void tracerBarreVcs(DrawSurface surface, GutterMarks marques, int ligne,
            float y, float hauteur, float coulisse, FrameState frame) {
        MarqueLigne marque = marques.marqueVcs(ligne);
        if (marque == MarqueLigne.INCHANGE) {
            return;
        }
        brush().setColor(marque == MarqueLigne.MODIFIE
                ? frame.theme().selectionColor()
                : frame.theme().syntaxe().couleur(TokenKind.STRING));
        surface.drawRect(0f, y - hauteur, Math.max(2f, coulisse / 6f), y, brush());
    }

    /** Le point d'arrêt (D-04) : carré plein posé, carré creux désactivé. */
    private void tracerArret(DrawSurface surface, GutterMarks marques, int ligne,
            float centreX, float centreY, float coulisse, float hauteur,
            FrameState frame) {
        EtatArret arret = marques.arret(ligne);
        if (arret == EtatArret.AUCUN) {
            return;
        }
        float demi = Math.min(coulisse, hauteur) / 4f;
        if (arret == EtatArret.POSE) {
            brush().setColor(frame.theme().syntaxe().couleur(TokenKind.ERROR));
            surface.drawRect(centreX - demi, centreY - demi,
                    centreX + demi, centreY + demi, brush());
            return;
        }
        brush().setColor(frame.theme().gutterTextColor());
        surface.drawLine(centreX - demi, centreY - demi, centreX + demi, centreY - demi,
                brush());
        surface.drawLine(centreX + demi, centreY - demi, centreX + demi, centreY + demi,
                brush());
        surface.drawLine(centreX + demi, centreY + demi, centreX - demi, centreY + demi,
                brush());
        surface.drawLine(centreX - demi, centreY + demi, centreX - demi, centreY - demi,
                brush());
    }

    /** Le repère d'exécution (D-05) : flèche à la ligne courante, chevron à la pile. */
    private void tracerExecution(DrawSurface surface, GutterMarks marques, int ligne,
            float centreX, float centreY, float coulisse, FrameState frame) {
        @Nullable Integer courante = marques.ligneCourante();
        if (courante != null && courante == ligne) {
            brush().setColor(frame.theme().cursorColor());
            float ouverture = coulisse / 4f;
            surface.drawLine(centreX - ouverture / 2f, centreY - ouverture,
                    centreX + ouverture / 2f, centreY, brush());
            surface.drawLine(centreX + ouverture / 2f, centreY,
                    centreX - ouverture / 2f, centreY + ouverture, brush());
        } else if (marques.dansPile(ligne)) {
            brush().setColor(frame.theme().gutterTextColor());
            float ouverture = coulisse / 6f;
            surface.drawLine(centreX - ouverture, centreY - ouverture,
                    centreX, centreY, brush());
            surface.drawLine(centreX, centreY,
                    centreX - ouverture, centreY + ouverture, brush());
        }
    }
}
