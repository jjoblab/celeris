package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Couche de chrome des guides d'indentation (A-17 relue, H-09 — la
 * structure sert le trait) : les lignes verticales qui alignent les
 * BLOCS du document — un guide par accolade appariée (ou par région
 * d'indentation pour les langages à décalage), tracé à la colonne de
 * la ligne qui ouvre le bloc, DEVANT son premier caractère, de la
 * ligne APRÈS l'accolade ouvrante à la ligne AVANT la fermante — les
 * lignes vides du corps se joignent au trait, un seul segment continu
 * par bloc. Jamais un trait par cran de blancs : le guide suit la
 * structure, une accolade dans une chaîne n'ouvre rien.
 *
 * <p>La PORTÉE ACTIVE se met en évidence (le retour terrain du
 * 2026-11-19 : « le fil vertical devant la méthode s'illumine aussi
 * après le { et avant le } ») : le guide du BLOC QUI CONTIENT LE
 * CARET se trace à l'encre du CURSEUR, un peu plus épais — le guide
 * des accolades jumelles du caret (celui du bloc de l'accolade
 * appariée) prend la même lumière. Les autres guides restent une
 * ombre de structure : un seul accent par image, la structure se
 * lit sans crier.</p>
 *
 * <p>La table vit dans le pont de syntaxe — elle porte SA révision,
 * le trait ne se tire que collé à l'instantané affiché : jamais la
 * structure d'une autre révision ne s'aligne sur les rangées courantes
 * (PIEGE-20). La projection règle les rangées à l'image (PIEGE-04) :
 * un guide traverse le pli replié, un bloc entier sous un pli ne se
 * trace pas ; une ouvrante coupée par le retour à la ligne descend le
 * départ du trait sous la rangée qui porte le crochet. Aucune
 * allocation dans la boucle (PIEGE-15) : la table se lit à
 * l'indice.</p>
 */
public final class IndentGuides extends PainterLayer {

    /** La source des guides structurels courants, consultée à chaque image. */
    @FunctionalInterface
    public interface GuidesSource {

        /**
         * La table des guides de l'image à venir.
         *
         * @return la table, {@link GuidesStructurels#VIDE} sans
         *         structure à jour
         */
        GuidesStructurels guides();
    }

    /**
     * L'alpha du guide — une ombre de structure : lisible sans lire,
     * l'encre du texte reste seule à parler.
     */
    private static final int ALPHA = 0x2B;

    /** La source des guides courants. */
    private final GuidesSource source;

    /**
     * Construit la couche des guides.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param source  la source des guides structurels courants
     */
    public IndentGuides(BrushFactory brushes, GuidesSource source) {
        super(brushes);
        this.source = source;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'ombre de structure de l'encre de base : le guide aligne,
        // il ne lit pas — la transparence vit dans le canal alpha
        return (frame.theme().baseTextColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        GuidesStructurels table = source.guides();
        if (table.revision() != frame.document().revision()) {
            return;
        }
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int colonnes = projection.columns();
        int premiereLigne = projection.firstLineOfRow(frame.firstRow());
        int derniereLigne = projection.lastLineOfRow(
                frame.firstRow() + frame.visibleRows() - 1);
        float hautFenetre = metrics.rowTop(frame.firstRow()) - frame.scrollY();
        float basFenetre = metrics.rowTop(frame.firstRow() + frame.visibleRows())
                - frame.scrollY();
        brush().setStrokeWidth(Math.max(1f, metrics.charWidth() * 0.06f));
        // La portée active : le caret principal éclaire le bloc qui le
        // porte (et les accolades jumelles) — les autres guides
        // restent une ombre.
        int accentCouleur = -1;
        float accentEpaisseur = 0f;
        int[] porteesActives = porteesActives(frame, table);
        if (porteesActives.length > 0) {
            accentCouleur = (frame.theme().cursorColor() & 0x00FFFFFF)
                    | (0xCC << 24);
            accentEpaisseur = Math.max(2f, metrics.charWidth() * 0.11f);
        }
        for (int i = 0; i < table.nombre(); i++) {
            GuidesStructurels.Guide guide = table.guide(i);
            if (guide.ligneOuverture() > derniereLigne) {
                break;
            }
            if (guide.ligneApresFin() > premiereLigne) {
                boolean actif = contient(porteesActives, i);
                brush().setColor(actif ? accentCouleur : baseColor(frame));
                brush().setStrokeWidth(actif ? accentEpaisseur
                        : Math.max(1f, metrics.charWidth() * 0.06f));
                tracerGuide(surface, frame, metrics, projection, colonnes,
                        guide, hautFenetre, basFenetre);
            }
        }
        // la couleur d'ombre revient pour la prochaine image
        brush().setColor(baseColor(frame));
        brush().setStrokeWidth(Math.max(1f, metrics.charWidth() * 0.06f));
    }

    /**
     * Les indices des guides ACCENTUÉS de l'image : ceux des blocs qui
     * ENTOURENT le caret principal (le plus profond l'illumine, ses
     * parents aussi — la chaîne de portée se lit d'un coup d'œil), et
     * celui du bloc de l'accolade APPARIÉE quand le caret touche son
     * ouvrante ou sa fermante.
     *
     * @param frame l'état d'image courant (le caret s'y lit)
     * @param table la table des guides de l'image
     * @return les indices distincts, vide sans accent
     */
    private static int[] porteesActives(FrameState frame,
            GuidesStructurels table) {
        MultiSelection selection = frame.selection();
        java.util.List<SelRange> plages = selection.ranges();
        if (plages.size() != 1) {
            return new int[0];
        }
        int caret = plages.get(0).start();
        Document document = frame.document();
        if (caret > document.length()) {
            return new int[0];
        }
        int ligneCaret = document.lineForOffset(caret);
        int colonneCaret = caret - document.lineStart(ligneCaret);
        java.util.List<Integer> actifs = new java.util.ArrayList<>(4);
        // La PORTÉE IMMÉDIATE d'abord : le bloc le plus profond dont le
        // caret vit le corps (le guide « de la méthode », pas celui de
        // toute la classe) — un seul accent, la structure se lit sans
        // crier (la sémantique du dépôt de départ).
        int immediate = -1;
        for (int i = 0; i < table.nombre(); i++) {
            GuidesStructurels.Guide guide = table.guide(i);
            boolean dedans = ligneCaret > guide.ligneOuverture()
                    && ligneCaret < guide.ligneApresFin();
            if (dedans && colonneCaret > guide.colonne()
                    && (immediate < 0 || guide.colonne() > table.guide(
                            immediate).colonne())) {
                immediate = i;
            }
        }
        if (immediate >= 0) {
            actifs.add(immediate);
        }
        ajouterJumelles(actifs, table, ligneCaret, colonneCaret);
        int[] sortie = new int[actifs.size()];
        for (int i = 0; i < sortie.length; i++) {
            sortie[i] = actifs.get(i);
        }
        return sortie;
    }

    /**
     * Les accolades jumelles : les blocs dont le caret TOUCHE
     * l'ouvrante ou la fermante — leurs guides s'illuminent aussi.
     */
    private static void ajouterJumelles(java.util.List<Integer> actifs,
            GuidesStructurels table, int ligneCaret, int colonneCaret) {
        for (int i = 0; i < table.nombre(); i++) {
            GuidesStructurels.Guide guide = table.guide(i);
            boolean surOuvrante = ligneCaret == guide.ligneOuverture()
                    && colonneCaret >= guide.colonneOuvrante()
                    && colonneCaret <= guide.colonneOuvrante() + 1;
            boolean surFermante = ligneCaret == guide.ligneApresFin()
                    && colonneCaret >= guide.colonneFermante()
                    && colonneCaret <= guide.colonneFermante() + 1;
            if ((surOuvrante || surFermante) && !actifs.contains(i)) {
                actifs.add(i);
            }
        }
    }

    /** Le tableau contient-il l'indice. */
    private static boolean contient(int[] table, int indice) {
        for (int valeur : table) {
            if (valeur == indice) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trace le guide d'un bloc : un seul segment vertical, du bas de
     * la rangée qui porte l'ouvrante au haut de la rangée qui porte
     * la fermante — rogné aux bords de la fenêtre quand le bloc
     * déborde, muet quand rien ne reste à couvrir.
     */
    private void tracerGuide(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, VisualProjection projection, int colonnes,
            GuidesStructurels.Guide guide, float hautFenetre, float basFenetre) {
        int rangneeOuvrante = projection.rowOfLine(guide.ligneOuverture());
        int rangneeFermante = projection.rowOfLine(guide.ligneApresFin());
        if (colonnes > 0) {
            rangneeOuvrante += guide.colonneOuvrante() / colonnes;
            rangneeFermante += guide.colonneFermante() / colonnes;
        }
        if (rangneeFermante <= rangneeOuvrante) {
            return;
        }
        float haut = metrics.rowTop(rangneeOuvrante) + metrics.lineHeight()
                - frame.scrollY();
        float bas = metrics.rowTop(rangneeFermante) - frame.scrollY();
        haut = Math.max(haut, hautFenetre);
        bas = Math.min(bas, basFenetre);
        if (bas <= haut) {
            return;
        }
        float x = frame.gutterWidth()
                + guide.colonne() * metrics.charWidth() - frame.scrollX();
        surface.drawLine(x, haut, x, bas, brush());
    }
}
