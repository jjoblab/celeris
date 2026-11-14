package jo.celeris.android.view;

import jo.celeris.core.syntax.TokenKind;

import org.jspecify.annotations.Nullable;

/**
 * Couche de gouttière : le voile TRANSLUCIDE du fond (M7.5,
 * redressement — la gouttière du dépôt de départ se teintait ainsi),
 * les numéros de lignes, alignés à droite dans le champ de la
 * gouttière AVANT la bande de plis (D-01 — numéros absolus), et les
 * marques — chevrons de pli (D-07) dans la BANDE DE PLIS au bord
 * droit de la gouttière, contre le code qu'ils replient ; marques
 * d'erreur (D-02) et intégrations (D-03 à D-05, C-11) dans la coulisse
 * des marqueurs au bord gauche.
 *
 * <p>La gouttière défile verticalement avec le contenu (chaque numéro
 * suit sa rangée) mais jamais horizontalement : son abscisse ne dépend
 * pas du défilement horizontal. Les numéros passent par la projection
 * (PIEGE-04) — la rangée d'un pli porte le numéro de son en-tête — et
 * les étiquettes mémoïsées n'allouent rien en régime établi
 * (PIEGE-15).</p>
 *
 * <p>La CELLULE cohérente (M7.5, redressement) : chaque marque vit
 * dans la même cellule carrée — le côté le plus étroit de sa bande et
 * de la hauteur de rangée — centrée au milieu de la rangée. Le chevron
 * de pli se rend TRIANGLE PLEIN, discret mais lisible (la reprise du
 * design : un chevron en contour fin se voyait à peine) ; le point
 * d'arrêt se rend CERCLE PLEIN posé, CERCLE CREUX désactivé ; la
 * marque d'erreur se rend point plein à l'encre d'erreur ; le repère
 * d'exécution se rend flèche pleine par-dessus l'arrêt. Les pastilles
 * de couleur (C-05) peignent dans la respiration à droite du champ,
 * à la valeur résolue ; les repères {@code @Preview} (C-11) peignent
 * dans la coulisse — l'œil du composable, le rendu appartient à
 * l'hôte.</p>
 *
 * <p>Le numéro de la ligne COURANTE se lit en GRAISSE, à l'encre du
 * texte de base — l'œil s'y pose, le compte des lignes s'y oriente.</p>
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

    /** La largeur courante d'une bande de gouttière, servie à chaque image. */
    @FunctionalInterface
    public interface LaneSource {

        /**
         * La largeur de la bande, en pixels (0 sans contenu).
         *
         * @return la largeur courante
         */
        float laneWidth();
    }

    /** La pastille de couleur d'une ligne, servie à chaque image (C-05). */
    @FunctionalInterface
    public interface PastilleSource {

        /**
         * La couleur résolue de la ligne, 0 si elle n'en porte pas.
         *
         * @param ligne la ligne, numérotée à partir de zéro
         * @return la couleur ARGB, 0 sans pastille
         */
        int pastille(int ligne);
    }

    /** Le repère {@code @Preview} d'une ligne, servi à chaque image (C-11). */
    @FunctionalInterface
    public interface RepereSource {

        /**
         * Vrai si la ligne porte un repère {@code @Preview}.
         *
         * @param ligne la ligne, numérotée à partir de zéro
         * @return vrai si la ligne porte un repère
         */
        boolean repere(int ligne);
    }

    /** L'ampoule d'intentions d'une ligne, servie à chaque image
     *  (l'audit Android Studio du 2026-11-14). */
    @FunctionalInterface
    public interface AmpouleSource {

        /**
         * Vrai si la ligne porte l'ampoule d'intentions.
         *
         * @param ligne la ligne, numérotée à partir de zéro
         * @return vrai si la ligne porte l'ampoule
         */
        boolean ampoule(int ligne);
    }

    /** Le jaune de l'ampoule de l'IDE de référence — la couleur de
     *  l'ICÔNE, pas du thème : l'ampoule reste jaune sur tout thème,
     *  comme dans l'IDE (le simulateur de référence porte #FFCB6B). */
    private static final int ENCRE_AMPOULE = 0xFFFFCB6B;

    private static final LaneSource SANS_PLIS = () -> 0f;

    private final GutterNumbers numeros = new GutterNumbers();
    private final MarksSource marques;
    private float marge;
    private final LaneSource coulisse;
    /** La bande de plis au bord DROIT de la gouttière (D-07). */
    private final LaneSource bandePli;
    private final PastilleSource pastilles;
    private final RepereSource reperes;
    /** L'ampoule d'intentions de chaque ligne — muette sans branchement. */
    private AmpouleSource ampoules = ligne -> false;
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
     *                   marqueurs à gauche, en pixels (0 sans marques) —
     *                   servie à chaque image, la police peut changer
     */
    public GutterPainter(BrushFactory brushes, float padding, MarksSource marques,
            LaneSource markerLane) {
        this(brushes, padding, marques, markerLane, SANS_PLIS, ligne -> 0,
                ligne -> false);
    }

    /**
     * Construit la couche de gouttière complète : coulisse de marqueurs,
     * pastilles de couleur (C-05) et repères {@code @Preview} (C-11).
     *
     * @param brushes    la fabrique de pinceaux (la couche prend le sien)
     * @param padding    la respiration de chaque côté du champ, en pixels
     * @param marques    la source des marques, consultée à chaque image
     * @param markerLane la largeur courante de la coulisse des
     *                   marqueurs à gauche, en pixels (0 sans marques)
     * @param pastilles  la pastille de chaque ligne (C-05), 0 sans
     * @param reperes    le repère {@code @Preview} de chaque ligne (C-11)
     */
    public GutterPainter(BrushFactory brushes, float padding, MarksSource marques,
            LaneSource markerLane, PastilleSource pastilles, RepereSource reperes) {
        this(brushes, padding, marques, markerLane, SANS_PLIS, pastilles, reperes);
    }

    /**
     * Construit la couche de gouttière entière : coulisse de marqueurs
     * à gauche, bande de plis à droite, pastilles et repères compris.
     *
     * @param brushes    la fabrique de pinceaux (la couche prend le sien)
     * @param padding    la respiration de chaque côté du champ, en pixels
     * @param marques    la source des marques, consultée à chaque image
     * @param markerLane la largeur courante de la coulisse des
     *                   marqueurs à gauche, en pixels (0 sans marques)
     * @param foldLane   la largeur courante de la bande de plis à
     *                   droite, en pixels (0 sans syntaxe)
     * @param pastilles  la pastille de chaque ligne (C-05), 0 sans
     * @param reperes    le repère {@code @Preview} de chaque ligne (C-11)
     */
    public GutterPainter(BrushFactory brushes, float padding, MarksSource marques,
            LaneSource markerLane, LaneSource foldLane, PastilleSource pastilles,
            RepereSource reperes) {
        super(brushes);
        this.marge = padding;
        this.marques = marques;
        this.coulisse = markerLane;
        this.bandePli = foldLane;
        this.pastilles = pastilles;
        this.reperes = reperes;
    }

    /**
     * Monte la gouttière complète de la vue — le montage vivait dans le
     * constructeur de l'{@link EditorView}, il déménage ici (plafond
     * ADR-002, le geste de M7.5) : coulisse de marqueurs à gauche, bande
     * de plis à droite, pastilles de couleur (C-05) et repères
     * {@code @Preview} (C-11), la respiration suit l'état de gouttière
     * de la vue.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param vue     la vue dont la gouttière se monte
     * @return la couche montée, branchée sur l'état de la vue
     */
    static GutterPainter monter(BrushFactory brushes, EditorView vue) {
        GutterPainter couche = new GutterPainter(brushes,
                vue.gouttiereEtat.margePx(),
                () -> vue.marquesGouttiere.marques(vue.syntaxe.marques()),
                vue.gouttiereEtat::coulisseCourante,
                vue.gouttiereEtat::bandePliCourante,
                vue.marquesGouttiere::pastille,
                vue.marquesGouttiere::reperePreview);
        couche.brancherAmpoules(vue.marquesGouttiere::ampoule);
        return couche;
    }

    /**
     * Branche la source des ampoules d'intentions — le montage vivant
     * chez l'{@link EditorView} y câble la coulisse de l'hôte ; la
     * source muette ne trace rien (jamais d'ampoule devinée).
     *
     * @param source la source des lignes porteuses de l'ampoule
     */
    public void brancherAmpoules(AmpouleSource source) {
        ampoules = source == null ? ligne -> false : source;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().gutterTextColor();
    }

    /**
     * Affiche ou masque les numéros de lignes (D-01) : masqués, la
     * gouttière ne trace plus d'étiquette — la coulisse des marqueurs
     * et la bande de plis restent entières.
     *
     * @param visibles vrai pour tracer les numéros (défaut)
     */
    public void setNumerosVisibles(boolean visibles) {
        numerosVisibles = visibles;
    }

    /**
     * Change la respiration de chaque côté du champ de numéros (D-19) :
     * la classe de largeur de fenêtre la décide — le compact resserre,
     * l'étendu respire.
     *
     * @param padding la nouvelle respiration, en pixels
     */
    public void setMarge(float padding) {
        marge = padding;
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
        tracerVoile(surface, frame, largeurGouttiere, hauteur,
                derniere - premiere);
        GutterMarks marquesImage = marques.marques();
        int ligneCourante = ligneCouranteDe(frame);
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
            float largeurPli = Math.max(0f, bandePli.laneWidth());
            if (largeurPli > 0f) {
                tracerChevron(surface, marquesImage, ligne, y, hauteur,
                        largeurGouttiere, largeurPli, frame);
            }
            tracerPastille(surface, ligne, y, hauteur, largeurGouttiere,
                    largeurPli);
            if (!numerosVisibles) {
                continue;
            }
            tracerNumero(surface, frame, ligne, y, largeurGouttiere,
                    largeurPli, charWidth, ligneCourante);
        }
    }

    /**
     * La ligne du curseur primaire — le numéro courant s'y graisse ;
     * sans curseur (sélection vide de plages), la première ligne sert
     * de repère nul.
     */
    private static int ligneCouranteDe(FrameState frame) {
        var plages = frame.selection().ranges();
        if (plages.isEmpty()) {
            return -1;
        }
        return frame.document().lineForOffset(plages.get(0).caret());
    }

    /**
     * Le numéro d'une ligne : aligné à droite AVANT la bande de plis,
     * en GRAISSE à l'encre du texte de base sur la ligne courante —
     * l'œil s'y pose ; la graisse et la couleur se reposent après le
     * tracé (PIEGE-01 : rien ne fuit vers l'étiquette suivante).
     */
    private void tracerNumero(DrawSurface surface, FrameState frame, int ligne,
            float y, float largeurGouttiere, float bandePli, float charWidth,
            int ligneCourante) {
        int numero = ligne + 1;
        String etiquette = numeros.label(numero);
        float x = GutterGeometry.numberX(etiquette.length(), largeurGouttiere,
                bandePli, charWidth, marge);
        if (ligne == ligneCourante) {
            brush().setColor(frame.theme().baseTextColor());
            brush().setStyle(true, false, false);
            surface.drawText(etiquette, x, y, brush());
            brush().setStyle(false, false, false);
            brush().setColor(frame.theme().gutterTextColor());
            return;
        }
        surface.drawText(etiquette, x, y, brush());
    }

    /**
     * Le voile du fond de gouttière (M7.5, redressement) : un rectangle
     * TRANSLUCIDE posé sur toute la hauteur visible — un thème sans
     * voile (alpha nul) n'en trace aucun, la gouttière vit sur le fond
     * de la zone de texte.
     */
    private void tracerVoile(DrawSurface surface, FrameState frame,
            float largeurGouttiere, float hauteur, int rangeesVisibles) {
        int voile = frame.theme().gutterBackground();
        if ((voile >>> 24) == 0 || largeurGouttiere <= 0f) {
            return;
        }
        brush().setColor(voile);
        surface.drawRect(0f, -hauteur, largeurGouttiere,
                (rangeesVisibles + 1) * hauteur, brush());
    }

    /**
     * La pastille de couleur (C-05) : un carré posé dans la respiration
     * à droite du champ de numéros, AVANT la bande de plis, à la valeur
     * résolue du littéral — la couleur de la ressource, pas celle d'un
     * thème.
     */
    private void tracerPastille(DrawSurface surface, int ligne, float y,
            float hauteur, float largeurGouttiere, float bandePli) {
        int couleur = pastilles.pastille(ligne);
        if (couleur == 0) {
            return;
        }
        float bord = Math.max(1f, marge / 4f);
        float cote = marge - 2f * bord;
        float gauche = largeurGouttiere - bandePli - bord - cote;
        float haut = y - hauteur + (hauteur - cote) / 2f;
        brush().setColor(couleur);
        surface.drawRect(gauche, haut, gauche + cote, haut + cote, brush());
    }

    /**
     * Les repères {@code @Preview} (C-11) : l'œil du composable dans la
     * coulisse — un losange plein dans la cellule, à l'encre du texte
     * de gouttière, le rendu du composé reste délégué à l'hôte.
     */
    private void tracerRepere(DrawSurface surface, int ligne, float centreX,
            float centreY, float cellule, FrameState frame) {
        if (!reperes.repere(ligne)) {
            return;
        }
        brush().setColor(frame.theme().gutterTextColor());
        float demi = cellule / 4f;
        surface.drawLine(centreX - demi, centreY, centreX, centreY - demi, brush());
        surface.drawLine(centreX, centreY - demi, centreX + demi, centreY, brush());
        surface.drawLine(centreX + demi, centreY, centreX, centreY + demi, brush());
        surface.drawLine(centreX, centreY + demi, centreX - demi, centreY, brush());
    }

    /**
     * Trace les marques d'une ligne dans la COULISSE à gauche — toutes
     * y vivent dans la MÊME cellule carrée, centrée au milieu de la
     * rangée (M7.5, redressement : taille et position cohérentes pour
     * chaque marque) : la marque d'erreur (D-02) puis les marques
     * d'intégration (D-03, D-04, D-05) et le repère {@code @Preview}
     * (C-11) — la priorité se juge au tracé, l'arrêt et le repère
     * d'exécution y priment sur le point d'erreur.
     */
    private void tracerMarques(DrawSurface surface, GutterMarks marques, int ligne,
            float y, float hauteur, float coulisse, FrameState frame) {
        float cellule = Math.min(coulisse, hauteur);
        float centreX = coulisse / 2f;
        // le MILIEU VISUEL de la rangée : la rangée couvre
        // [y - ascent, y + descent], le centre vit à y - ascent + h/2
        float centreY = y - frame.metrics().ascent() + hauteur / 2f;
        // l'AMPOULE d'intentions PRIME sur le point d'erreur (le
        // comportement de l'IDE de référence : au caret qui porte un
        // diagnostic, l'ampoule remplace le marqueur — son tap ouvre
        // le menu des intentions)
        if (ampoules.ampoule(ligne)) {
            dessinerAmpoule(surface, centreX, centreY, cellule);
        } else if (marques.erreur(ligne)) {
            // le point d'erreur : un disque plein à l'encre d'erreur —
            // la marque se voit, elle ne se devine plus
            brush().setColor(frame.theme().syntaxe().couleur(TokenKind.ERROR));
            surface.drawCircle(centreX, centreY, cellule / 5f, true, brush());
        }
        tracerIntegration(surface, marques, ligne, centreX, centreY, cellule,
                frame);
        tracerRepere(surface, ligne, centreX, centreY, cellule, frame);
    }

    /**
     * L'ampoule d'intentions (l'audit Android Studio : le petit icône
     * jaune de l'IDE de référence) : le DISQUE du verre et son CULOT
     * dessous, dans la cellule cohérente — le glyphe iconique, à
     * l'encre de l'icône (jaune sur tout thème, comme dans l'IDE).
     */
    private void dessinerAmpoule(DrawSurface surface, float centreX,
            float centreY, float cellule) {
        brush().setColor(ENCRE_AMPOULE);
        float rayon = cellule * 0.28f;
        // le verre de l'ampoule : un disque plein, légèrement haut
        surface.drawCircle(centreX, centreY - rayon * 0.2f, rayon, true,
                brush());
        // le culot : le petit rectangulaire sous le verre
        surface.drawRect(centreX - rayon * 0.4f, centreY + rayon * 0.55f,
                centreX + rayon * 0.4f, centreY + rayon * 1.05f, brush());
    }

    /**
     * Le chevron de pli (D-07) dans la BANDE DE PLIS au bord droit de
     * la gouttière, contre le code qu'il replie : DEUX TRAITS PLEINS
     * en chevron — l'audit du propriétaire du 2026-11-14 : « l'icône
     * pli/dépli… il est trop foncé, améliore-la » — le triangle plein
     * de la reprise se voit trop ; le chevron en trait fin se lit
     * sans crier. Replié, la pointe regarde à droite ; pliable, elle
     * regarde en bas. Le glyphe prend sa cellule, la cible du doigt
     * comprise.
     */
    private void tracerChevron(DrawSurface surface, GutterMarks marques, int ligne,
            float y, float hauteur, float largeurGouttiere, float bandePli,
            FrameState frame) {
        if (!marques.pliable(ligne)) {
            return;
        }
        float cellule = Math.min(bandePli, hauteur);
        // le chevron vit au centre de la bande, à droite du dernier
        // chiffre — le pli se lit contre le code qu'il replie
        float centreX = largeurGouttiere - bandePli / 2f;
        float centreY = y - frame.metrics().ascent() + hauteur / 2f;
        brush().setColor(attenuer(frame.theme().gutterTextColor(), 0.6f));
        // le trait FIN du chevron — l'épaisseur d'un filet, jamais la
        // graisse d'un triangle plein
        brush().setStrokeWidth(Math.max(1.25f, cellule / 14f));
        float demi = cellule * 0.22f;
        // replié : la pointe regarde à droite › ; pliable : en bas ⌄ —
        // deux traits joints au sommet racontent l'état
        if (marques.replie(ligne)) {
            surface.drawLine(centreX - demi, centreY - demi, centreX + demi,
                    centreY, brush());
            surface.drawLine(centreX - demi, centreY + demi, centreX + demi,
                    centreY, brush());
        } else {
            surface.drawLine(centreX - demi, centreY - demi, centreX,
                    centreY + demi, brush());
            surface.drawLine(centreX + demi, centreY - demi, centreX,
                    centreY + demi, brush());
        }
    }

    /**
     * Les marques d'intégration (D-03, D-04, D-05) : la barre de
     * modification VCS au bord gauche — modifiée à l'encre de
     * sélection, ajoutée à l'encre de chaîne —, le point d'arrêt dans
     * la cellule — CERCLE PLEIN posé à l'encre d'erreur, CERCLE CREUX
     * neutre désactivé —, le repère d'exécution par-dessus : la FLÈCHE
     * PLEINE de la ligne courante à l'encre du curseur, le chevron
     * discret des lignes de pile.
     */
    private void tracerIntegration(DrawSurface surface, GutterMarks marques,
            int ligne, float centreX, float centreY, float cellule,
            FrameState frame) {
        tracerBarreVcs(surface, marques, ligne, centreY, cellule, frame);
        tracerArret(surface, marques, ligne, centreX, centreY, cellule, frame);
        tracerExecution(surface, marques, ligne, centreX, centreY, cellule,
                frame);
    }

    /**
     * La barre de modification VCS (D-03) : au bord gauche de la
     * coulisse, plein rang de hauteur.
     */
    private void tracerBarreVcs(DrawSurface surface, GutterMarks marques, int ligne,
            float centreY, float cellule, FrameState frame) {
        MarqueLigne marque = marques.marqueVcs(ligne);
        if (marque == MarqueLigne.INCHANGE) {
            return;
        }
        brush().setColor(marque == MarqueLigne.MODIFIE
                ? frame.theme().selectionColor()
                : frame.theme().syntaxe().couleur(TokenKind.STRING));
        float haut = centreY - cellule / 2f;
        surface.drawRect(0f, haut, Math.max(2f, cellule / 6f),
                haut + cellule, brush());
    }

    /**
     * Le point d'arrêt (D-04) : CERCLE PLEIN à l'encre d'erreur posé,
     * CERCLE CREUX à l'encre de gouttière désactivé — dans la cellule
     * cohérente, le tiers du côté pour rayon.
     */
    private void tracerArret(DrawSurface surface, GutterMarks marques, int ligne,
            float centreX, float centreY, float cellule, FrameState frame) {
        EtatArret arret = marques.arret(ligne);
        if (arret == EtatArret.AUCUN) {
            return;
        }
        float rayon = cellule / 3f;
        if (arret == EtatArret.POSE) {
            brush().setColor(frame.theme().syntaxe().couleur(TokenKind.ERROR));
            surface.drawCircle(centreX, centreY, rayon, true, brush());
            return;
        }
        brush().setColor(frame.theme().gutterTextColor());
        brush().setStrokeWidth(Math.max(1.5f, cellule / 12f));
        surface.drawCircle(centreX, centreY, rayon, false, brush());
    }

    /**
     * Le repère d'exécution (D-05) : FLÈCHE PLEINE à la ligne courante,
     * chevron discret à la pile — dans la cellule cohérente.
     */
    private void tracerExecution(DrawSurface surface, GutterMarks marques, int ligne,
            float centreX, float centreY, float cellule, FrameState frame) {
        @Nullable Integer courante = marques.ligneCourante();
        if (courante != null && courante == ligne) {
            brush().setColor(frame.theme().cursorColor());
            surface.drawTriangle(centreX, centreY, cellule / 3f, false, brush());
        } else if (marques.dansPile(ligne)) {
            brush().setColor(frame.theme().gutterTextColor());
            float ouverture = cellule / 6f;
            surface.drawLine(centreX - ouverture, centreY - ouverture,
                    centreX, centreY, brush());
            surface.drawLine(centreX, centreY,
                    centreX - ouverture, centreY + ouverture, brush());
        }
    }

    /** L'atténuation d'une couleur ARGB — le chevron se lit sans crier. */
    private static int attenuer(int couleur, float facteur) {
        int a = (int) (((couleur >>> 24) & 0xFF)
                * Math.max(0f, Math.min(1f, facteur)));
        return (a << 24) | (couleur & 0x00FFFFFF);
    }
}
