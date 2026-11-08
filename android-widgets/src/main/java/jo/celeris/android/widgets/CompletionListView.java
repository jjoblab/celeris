package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La liste de complétion (portage du design du dépôt de départ — B-03,
 * D-10) : les candidats en RANGÉES DEUX LIGNES — le badge de genre
 * (carré arrondi teinté à vingt pour cent, glyphe à l'encre du genre)
 * précède le libellé, dont les caractères qui matchent le préfixe tapé
 * passent à l'accent et au gras ; l'identité qualifiée vit SOUS le
 * libellé, en atténué. Le candidat courant se surligne de la teinte de
 * sélection, encadrée à un pixel, sous le clip arrondi de la carte.
 * Le FILTRAGE vit chez l'hôte (la complétion filtre sans se fermer,
 * M7.5 §7) : la liste ne montre que ce qu'on lui pose. PIEGE-27 :
 * jamais de focus, l'éditeur garde le clavier.
 */
public final class CompletionListView extends PopupPanel {

    /** Le nombre maximal de candidats montrés — l'hôte tronque sa fenêtre. */
    public static final int MAX_MONTRES = 8;

    /** Un candidat de complétion : son libellé, son identité qualifiée, son genre. */
    public record Candidat(String libelle, String detail, BadgeGenre.Genre genre) {
    }

    private List<Candidat> candidats = List.of();
    /** Le préfixe tapé — ses caractères passent à l'accent dans le libellé. */
    private String prefix = "";
    /** L'index du candidat courant, —1 si vide. */
    private int highlighted = -1;
    private final float densite;
    private final float rangeePx;
    private final float largeurMaxPx;
    private @Nullable IntConsumer onPick;

    public CompletionListView(Context context, PopupPalette palette) {
        super(context, palette);
        this.densite = getResources().getDisplayMetrics().density;
        this.rangeePx = PopupTokens.HAUTEUR_RANGEE_LISTE_DP * densite;
        this.largeurMaxPx = PopupTokens.LARGEUR_MAX_LISTE_DP * densite
                - 2 * paddingPx;
    }

    /**
     * Pose les candidats — le premier est le candidat courant,
     * surligné.
     *
     * @param items les candidats, déjà filtrés par l'hôte
     */
    public void setItems(List<Candidat> items) {
        this.candidats = List.copyOf(items);
        requestLayout();
        invalidate();
        setHighlighted(items.isEmpty() ? -1 : 0);
    }

    /**
     * Pose le préfixe tapé — les caractères qui matchent dans les
     * libellés passent à l'accent et au gras.
     *
     * @param prefixe le préfixe, vide pour aucun surlignage
     */
    public void setPrefix(String prefixe) {
        this.prefix = prefixe == null ? "" : prefixe;
        invalidate();
    }

    /** Le candidat d'index donné, null s'il n'existe pas. */
    public Candidat candidatAt(int index) {
        return index >= 0 && index < candidats.size()
                ? candidats.get(index) : null;
    }

    /** Le libellé du candidat d'index donné, null s'il n'existe pas. */
    public String itemAt(int index) {
        Candidat candidat = candidatAt(index);
        return candidat == null ? null : candidat.libelle();
    }

    /** Le rappel de l'appui sur une rangée — l'hôte exécute. */
    public void setOnPick(@Nullable IntConsumer rappel) {
        onPick = rappel;
    }

    /** Le candidat courant, surligné de la teinte de sélection. */
    private void setHighlighted(int index) {
        highlighted = index;
        invalidate();
    }

    @Override
    void drawContent(Canvas canvas) {
        for (int i = 0; i < candidats.size(); i++) {
            float haut = i * rangeePx;
            if (i == highlighted) {
                bandPaint().setColor(palette.selection());
                canvas.drawRect(-paddingPx + 1f, haut,
                        contentWidth() + paddingPx - 1f, haut + rangeePx,
                        bandPaint());
            }
            dessinerRangee(canvas, candidats.get(i), haut);
        }
    }

    /**
     * Une rangée DEUX lignes (le motif du dépôt de départ) : badge de
     * genre (carré arrondi teinté à vingt pour cent, glyphe semi-gras
     * à l'encre du genre), libellé sur la première ligne — les
     * caractères du préfixe à l'accent et au gras —, identité qualifiée
     * en atténué sur la seconde.
     */
    private void dessinerRangee(Canvas canvas, Candidat candidat, float haut) {
        BadgeGenre.Meta badge = BadgeGenre.meta(candidat.genre(),
                palette.muted() != 0 && estSombre(), palette.accent());
        float tailleBadge = Math.min(rangeePx - 10f * densite,
                PopupTokens.BADGE_MAX_DP * densite);
        float gaucheBadge = 0f;
        float hautBadge = haut + (rangeePx - tailleBadge) * 0.5f;
        // Fond : teinte du genre à vingt pour cent d'alpha.
        bandPaint().setColor((badge.couleur() & 0x00FFFFFF) | 0x33000000);
        float rayonBadge = PopupTokens.RAYON_PUCE_DP * densite;
        canvas.drawRoundRect(gaucheBadge, hautBadge,
                gaucheBadge + tailleBadge, hautBadge + tailleBadge,
                rayonBadge, rayonBadge, bandPaint());
        // Glyphe centré, semi-gras.
        textPaint().setTextSize(tailleBadge
                * BadgeGenre.facteurTailleGlyphe(badge.glyphe()));
        textPaint().setColor(badge.couleur());
        textPaint().setFakeBoldText(true);
        Paint.FontMetrics badgeFm = textPaint().getFontMetrics();
        float baseGlyphe = hautBadge + tailleBadge * 0.5f
                - (badgeFm.ascent + badgeFm.descent) * 0.5f;
        canvas.drawText(badge.glyphe(), gaucheBadge
                + (tailleBadge - textPaint().measureText(badge.glyphe()))
                        * 0.5f, baseGlyphe, textPaint());
        textPaint().setFakeBoldText(false);
        float xTexte = gaucheBadge + tailleBadge + 6f * densite;
        float largeurTexte = contentWidth() - xTexte;
        // Première ligne : le libellé, runs de match à l'accent.
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        dessinerLibelle(canvas, candidat.libelle(), xTexte,
                haut + rangeePx * 0.45f, largeurTexte);
        // Seconde ligne : l'identité qualifiée, en atténué.
        if (!candidat.detail().isEmpty()) {
            textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_META));
            textPaint().setColor(palette.muted());
            String detail = ellipser(candidat.detail(), largeurTexte);
            canvas.drawText(detail, xTexte, haut + rangeePx * 0.85f,
                    textPaint());
        }
    }

    /** Vrai quand le verre de la palette couvre un thème sombre — le
     *  badge s'y calibre (palette telle quelle, sinon assombrie). */
    private boolean estSombre() {
        return (palette.background() & 0xFFFFFF) < 0x808080;
    }

    /**
     * Le libellé, caractère par caractère : les positions qui matchent
     * le préfixe tapé (sous-séquence, cas ignoré) passent à l'encre
     * d'accent et au gras, les autres gardent l'encre des libellés.
     */
    private void dessinerLibelle(Canvas canvas, String libelle, float x,
            float base, float largeur) {
        boolean[] matche = positionsMatchees(libelle);
        float curseur = x;
        int i = 0;
        while (i < libelle.length() && curseur - x < largeur) {
            boolean estMatch = matche[i];
            int fin = i;
            while (fin + 1 < libelle.length()
                    && matche[fin + 1] == estMatch) {
                fin++;
            }
            String segment = libelle.substring(i, fin + 1);
            textPaint().setColor(estMatch ? palette.accent()
                    : palette.keyword());
            textPaint().setFakeBoldText(estMatch);
            canvas.drawText(segment, curseur, base, textPaint());
            textPaint().setFakeBoldText(false);
            curseur += textPaint().measureText(segment);
            i = fin + 1;
        }
    }

    /**
     * Les positions du libellé qui matchent le préfixe (sous-séquence
     * gloutonne, cas ignoré) — le portage léger du fil flou du dépôt
     * de départ.
     */
    private boolean[] positionsMatchees(String libelle) {
        boolean[] matche = new boolean[libelle.length()];
        if (prefix.isEmpty()) {
            return matche;
        }
        String cible = libelle.toLowerCase();
        String filtre = prefix.toLowerCase();
        int dansFiltre = 0;
        for (int i = 0; i < cible.length() && dansFiltre < filtre.length();
                i++) {
            if (cible.charAt(i) == filtre.charAt(dansFiltre)) {
                matche[i] = true;
                dansFiltre++;
            }
        }
        return matche;
    }

    /** Coupe le texte en queue avec ellipse pour tenir dans la largeur. */
    private String ellipser(String texte, float largeur) {
        if (textPaint().measureText(texte) <= largeur) {
            return texte;
        }
        StringBuilder coupe = new StringBuilder();
        for (int i = 0; i < texte.length(); i++) {
            coupe.append(texte.charAt(i));
            if (textPaint().measureText(coupe + "…") > largeur) {
                coupe.deleteCharAt(coupe.length() - 1);
                break;
            }
        }
        return coupe + "…";
    }

    @Override
    int contentWidth() {
        float largeur = 0f;
        for (Candidat candidat : candidats) {
            textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
            largeur = Math.max(largeur,
                    textPaint().measureText(candidat.libelle()));
            if (!candidat.detail().isEmpty()) {
                textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_META));
                largeur = Math.max(largeur,
                        textPaint().measureText(candidat.detail()));
            }
        }
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        return Math.round(Math.min(largeur, largeurMaxPx));
    }

    @Override
    int contentHeight() {
        return Math.round(candidats.size() * rangeePx);
    }

    /**
     * L'ordonnée du CENTRE de la rangée donnée — les bancs y visent
     * leur appui.
     */
    float rowCenterY(int index) {
        return index * rangeePx + rangeePx / 2f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onPick == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = (int) (event.getY() / rangeePx);
        if (index >= 0 && index < candidats.size()) {
            onPick.accept(index);
            performClick();
            return true;
        }
        return false;
    }
}
