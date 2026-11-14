package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le menu À ÉTAGES des actions de l'éditeur (l'audit du propriétaire
 * du 2026-11-14 : « je veux le séparer en étages ou colonnes qui
 * deviennent visibles contextuellement comme les go to, l'option
 * usages, rename, etc. ») : les actions s'y groupent par SECTIONS —
 * l'en-tête en petites capitales muettes annonce son étage, les
 * rangées cliquables le servent. L'approche de l'IDE de référence :
 * les étages ne vivent que s'ils portent — un groupe VIDE ne se
 * montre jamais (la visibilité contextuelle naît de la donnée, pas
 * d'un drapeau codé en dur), la colonne unique garde le tactile
 * lisible, la carte se referme sur le choix AVANT d'exécuter.
 *
 * <p>Chaque étage porte ses rangées DENSES : hauteur de rangée
 * resserrée au palier titre, pictogramme à l'encre allégée qui
 * précède le libellé, filet entre étages. La navigation clavier
 * déléguée ({@link PopupClavier}) saute les en-têtes — une section
 * ne se prend jamais. PIEGE-27 : jamais de focus.</p>
 */
public final class MenuEtages extends PopupPanel implements PopupClavier {

    /** Une action d'étage : son libellé, SON pictogramme (null :
     *  rangée sans glyphe) et ce qu'elle fait. */
    public record Item(String label, IconeAction picto, Runnable action) {

        /** L'action sans glyphe — l'hôte qui ne pose pas d'icône garde
         *  sa rangée telle quelle. */
        public Item(String label, Runnable action) {
            this(label, null, action);
        }
    }

    /** Un étage du menu : son en-tête (null : le premier étage sans
     *  titre) et ses actions — l'étage VIDE ne se montre jamais. */
    public record Etage(@Nullable String titre, List<Item> actions) {

        /** L'étage sans titre. */
        public static Etage sansTitre(List<Item> actions) {
            return new Etage(null, actions);
        }

        /**
         * L'étage des items de la barre de sélection — la conversion
         * vit ici, l'hôte ne transpose jamais : les actions repliées
         * de la pilule deviennent les rangées de l'étage.
         *
         * @param titre  l'en-tête de l'étage, null pour aucun
         * @param items  les actions de la barre, telles quelles
         * @return l'étage converti
         */
        public static Etage depuis(@Nullable String titre,
                List<SelectionBarView.Item> items) {
            List<Item> actions = new ArrayList<>(items.size());
            for (SelectionBarView.Item item : items) {
                actions.add(new Item(item.label(), item.picto(),
                        item.action()));
            }
            return new Etage(titre, actions);
        }
    }

    /** Les étages courants — vide : la carte ne se montre pas. */
    private List<Etage> etages = List.of();
    /** Les rangées aplaties : null = en-tête d'étage (jamais pris), le
     *  reste sert l'action de l'index. */
    private List<Item> rangees = new ArrayList<>();
    /** Les rangées d'en-tête — vrai quand la rangée d'index annonce un étage. */
    private List<Boolean> entetes = new ArrayList<>();
    /** La hauteur de chaque rangée posée, en pixels. */
    private List<Integer> hauteurs = new ArrayList<>();
    /** L'action surlignée par le clavier délégué — —1 si aucune. */
    private int surligne = -1;
    /** L'écart de respiration entre rangées, en pixels. */
    private final int ecartPx;
    /** La hauteur cible d'une rangée d'action, en pixels. */
    private final int hauteurActionPx;
    private @Nullable IntConsumer onPick;

    public MenuEtages(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.ecartPx = Math.round(PopupTokens.ECART_DP * densite);
        // la rangée d'action de l'IDE de référence : DENSE — le texte
        // au palier titre, deux respirations verticales comprises
        this.hauteurActionPx = Math.round(
                tailleTexte(PopupTokens.TEXTE_TITRE) + 2 * ecartPx);
    }

    /**
     * Pose les étages du menu — les étages VIDES se retirent d'eux-mêmes
     * (la visibilité contextuelle naît de la donnée : un étage sans
     * action ne se montre jamais), le menu sans étage vivant reste
     * vide, l'hôte n'y pose rien.
     *
     * @param groupes les étages, dans l'ordre d'affichage
     */
    public void setEtages(List<Etage> groupes) {
        List<Etage> vivants = new ArrayList<>(groupes.size());
        for (Etage etage : groupes) {
            if (!etage.actions().isEmpty()) {
                vivants.add(etage);
            }
        }
        this.etages = List.copyOf(vivants);
        this.surligne = -1;
        rebatirRangees();
    }

    /** Vrai quand le menu porte au moins une action — l'hôte y
     *  décide de la pose. */
    public boolean aActions() {
        return !rangees.isEmpty();
    }

    /** Rebâtit les rangées aplaties : l'en-tête d'étage vit —1, les
     *  actions suivent — les hauteurs se posent par palier. */
    private void rebatirRangees() {
        rangees = new ArrayList<>();
        entetes = new ArrayList<>();
        hauteurs = new ArrayList<>();
        for (int e = 0; e < etages.size(); e++) {
            Etage etage = etages.get(e);
            if (etage.titre() != null && !etage.titre().isEmpty()) {
                rangees.add(null);
                entetes.add(true);
                hauteurs.add(hauteurEntete());
            }
            for (Item item : etage.actions()) {
                rangees.add(item);
                entetes.add(false);
                hauteurs.add(hauteurActionPx);
            }
        }
        requestLayout();
        invalidate();
    }

    /** La hauteur d'une rangée d'en-tête — le micro-palier respire
     *  moins qu'une action. */
    private int hauteurEntete() {
        return Math.round(tailleTexte(PopupTokens.TEXTE_MICRO) + ecartPx);
    }

    @Override
    void drawContent(Canvas canvas) {
        float y = 0f;
        for (int i = 0; i < rangees.size(); i++) {
            int hauteur = hauteurs.get(i);
            Item item = rangees.get(i);
            if (item == null) {
                dessinerEntete(canvas, etageDe(i).titre(), y, hauteur);
            } else {
                if (i == surligne) {
                    bandPaint().setColor(palette.selection());
                    canvas.drawRect(-paddingPx + 1f, y,
                            contentWidth() + paddingPx - 1f, y + hauteur,
                            bandPaint());
                }
                dessinerAction(canvas, item, y, hauteur);
            }
            y += hauteur;
            // le filet ENTRE étages — jamais après la dernière rangée
            if (estDerniereDeSonEtage(i) && i < rangees.size() - 1) {
                tracerFilet(canvas, y + ecartPx / 2f);
                y += ecartPx;
            }
        }
    }

    /** L'en-tête d'étage : petites capitales à l'encre muette — la
     *  section annonce, elle ne se prend jamais. */
    private void dessinerEntete(Canvas canvas, String titre, float y,
            int hauteur) {
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_MICRO));
        textPaint().setColor(palette.muted());
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        canvas.drawText(titre.toUpperCase(), 0f, y - metriques.top,
                textPaint());
    }

    /** Une rangée d'action : SON pictogramme à l'encre allégée, son
     *  libellé au palier titre. */
    private void dessinerAction(Canvas canvas, Item item, float y,
            int hauteur) {
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        textPaint().setColor(palette.text());
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        float ligne = y + (hauteur + metriques.bottom - metriques.top) / 2f
                - metriques.bottom;
        float rayon = textPaint().getTextSize() * PopupTokens.FACTEUR_PICTO;
        float x = 0f;
        if (item.picto() != null) {
            IconeAction.dessiner(item.picto(), canvas, linePaint(),
                    textPaint(), x + rayon, ligne - textPaint().getTextSize()
                            * 0.35f, rayon,
                    IconeAction.encreAllgeee(palette.muted(), palette.text()));
            x += 2 * rayon + 5f * getResources().getDisplayMetrics().density;
        }
        canvas.drawText(item.label(), x, ligne, textPaint());
    }

    /** L'étage de la rangée d'index donné — l'en-tête y vit. */
    private Etage etageDe(int index) {
        int curseur = 0;
        for (Etage etage : etages) {
            if (etage.titre() != null && !etage.titre().isEmpty()) {
                curseur++;
            }
            curseur += etage.actions().size();
            if (index < curseur) {
                return etage;
            }
        }
        return etages.isEmpty() ? Etage.sansTitre(List.of())
                : etages.get(etages.size() - 1);
    }

    /** Vrai quand la rangée d'index donné ferme son étage — le filet
     *  s'y pose après elle. */
    private boolean estDerniereDeSonEtage(int index) {
        int curseur = 0;
        for (Etage etage : etages) {
            if (etage.titre() != null && !etage.titre().isEmpty()) {
                curseur++;
            }
            curseur += etage.actions().size();
            if (index < curseur) {
                return index == curseur - 1;
            }
        }
        return false;
    }

    @Override
    int contentWidth() {
        float largeur = 0f;
        float densite = getResources().getDisplayMetrics().density;
        for (int i = 0; i < rangees.size(); i++) {
            Item item = rangees.get(i);
            if (item == null) {
                textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_MICRO));
                largeur = Math.max(largeur, textPaint().measureText(
                        etageDe(i).titre().toUpperCase()));
            } else {
                textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
                float picto = item.picto() == null ? 0f
                        : 2 * textPaint().getTextSize()
                                * PopupTokens.FACTEUR_PICTO + 5f * densite;
                largeur = Math.max(largeur, picto
                        + textPaint().measureText(item.label()));
            }
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        int hauteur = 0;
        for (int i = 0; i < rangees.size(); i++) {
            hauteur += hauteurs.get(i);
            if (estDerniereDeSonEtage(i) && i < rangees.size() - 1) {
                hauteur += ecartPx;
            }
        }
        return hauteur;
    }

    /** L'ordonnée du CENTRE de la rangée donnée — les bancs y visent
     *  leur appui. */
    float rowCenterY(int index) {
        if (hauteurs.isEmpty()) {
            return paddingYPx;
        }
        float y = 0f;
        for (int i = 0; i < index && i < hauteurs.size(); i++) {
            y += hauteurs.get(i);
            if (estDerniereDeSonEtage(i) && i < rangees.size() - 1) {
                y += ecartPx;
            }
        }
        return y + hauteurs.get(Math.min(index, hauteurs.size() - 1)) / 2f;
    }

    /** La rangée visée par l'ordonnée, —1 si hors contenu ou sur un
     *  EN-TÊTE — une section ne se prend jamais. */
    private int rowAt(float y) {
        float curseur = 0f;
        for (int i = 0; i < rangees.size(); i++) {
            if (y >= curseur && y < curseur + hauteurs.get(i)) {
                return entetes.get(i) ? -1 : i;
            }
            curseur += hauteurs.get(i);
            if (estDerniereDeSonEtage(i) && i < rangees.size() - 1) {
                curseur += ecartPx;
            }
        }
        return -1;
    }

    /** Le rappel de l'appui sur une action — l'hôte exécute. */
    public void setOnPick(@Nullable IntConsumer rappel) {
        onPick = rappel;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onPick == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = rowAt(event.getY() - paddingYPx);
        if (index >= 0) {
            onPick.accept(index);
            performClick();
            return true;
        }
        return false;
    }

    /** L'action de rangée d'index donné, null s'il n'existe pas ou sur
     *  un en-tête — les bancs y lisent la structure. */
    public @Nullable Item itemAt(int index) {
        if (index < 0 || index >= rangees.size()) {
            return null;
        }
        return rangees.get(index);
    }

    /** L'action surlignée par le clavier délégué, —1 si aucune — les
     *  bancs y vérifient la navigation. */
    public int surbrillance() {
        return surligne;
    }

    @Override
    public boolean surFleche(int pas) {
        // la navigation SAUTE les en-têtes — jamais d'arrêt sur une
        // section : le saut vit en pur chez NavigationEtages, testé
        int cible = NavigationEtages.saut(rangees.size(), surligne, pas,
                entetes::get);
        if (cible < 0) {
            return false;
        }
        surligne = cible;
        invalidate();
        return true;
    }

    @Override
    public boolean surPage(int pas) {
        // le menu décline les pages : ses actions tiennent en peu de
        // rangées — la touche retourne à l'éditeur
        return false;
    }

    @Override
    public boolean surValidation() {
        if (surligne < 0 || surligne >= rangees.size()) {
            return false;
        }
        Item item = rangees.get(surligne);
        if (item == null) {
            return false;
        }
        // la validation clavier prend le MÊME chemin que le doigt
        item.action().run();
        performClick();
        return true;
    }
}
