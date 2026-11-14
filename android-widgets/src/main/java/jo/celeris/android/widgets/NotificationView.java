package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte de notification (catégorie G de l'inventaire — la forme
 * restait à décider, la bulle éphémère l'emporte) : l'annonce se pose
 * en titre et message, l'HORLOGE la referme à son heure ({@link #armer}
 * — la queue du FIL UI la porte, jamais l'éditeur), le tap LA PREND la
 * referme ({@link #setSurPrise} — le Runnable de l'hôte seul décide).
 * L'audit Android Studio du 2026-11-14 y pose les ACTIONS de la bulle
 * : l'IDE de référence hyperlie ses actions derrière le message (deux
 * en vue, le reste replié) — la carte les rend à l'encre d'accent
 * derrière un filet, la prise d'un LIEN referme la bulle puis exécute
 * son action (l'ordre de l'IDE : la bulle cède, l'action court — une
 * action qui re-pose n'est jamais tuée par sa propre fermeture).
 *
 * <p>La parité au simulateur (la référence de design, 2026-11-14) y
 * pose la JAUGE et le COMPTE À REBOURS : l'horloge qui coule se VOIT —
 * une piste muette pleine largeur, le remplissage à l'encre d'accent
 * qui fond au fil du temps, et la légende « l'horloge la referme —
 * X,X s » sous la barre. Le TICK (100 ms) vit sur le MÊME fil que
 * l'expiration : armer arme les deux, desarmer coupe les deux, la
 * re-pose réarme sans cumul. Sans horloge armée, la jauge ne se trace
 * pas — la carte ne promet pas un temps qu'elle ne tient pas.</p>
 *
 * <p>PIEGE-27 : jamais de focus — l'annonce s'affiche, elle ne règne
 * pas ; les régimes l'épargnent (frappe, défilement, geste extérieur —
 * elle annonce, elle ne décrit pas le texte), seule son heure la
 * conclut. La carte ne connaît NI l'éditeur, NI le coordinateur :
 * l'expiration rend à l'hôte, qui referme le genre.</p>
 */
public final class NotificationView extends TextRowsPopup {

    /** Une action de l'annonce — le lien de la bulle : son libellé et
     *  ce qu'elle fait. */
    public record Action(String libelle, Runnable action) {
    }

    /** Le pas du tick de la jauge, en millisecondes — le simulateur
     *  anime au centième de seconde, la carte suit. */
    private static final long TICK_MS = 100L;

    /** L'épaisseur de la jauge, en dp — un filet visible, jamais une
     *  barre qui mange la carte. */
    private static final float EPAISSEUR_JAUGE_DP = 2f;

    /** Le titre de l'annonce — vide avant la première pose. */
    private String titre = "";
    /** Les actions posées — vides sans liens. */
    private List<Action> actions = List.of();
    /** Le rappel du tap qui PREND la bulle — null : pas de rappel. */
    private @Nullable Runnable surPrise;
    /** L'horloge de l'annonce — la queue du fil UI la porte. */
    private final Handler horloge = new Handler(Looper.getMainLooper());
    /** La course en cours — null quand l'horloge ne court pas. */
    private @Nullable Runnable course;
    /** Le tick de la jauge — null quand l'horloge ne court pas. */
    private @Nullable Runnable tick;
    /** La durée armée, en millisecondes — 0 : horloge désarmée. */
    private long dureeMs;
    /** Le départ de la course armée, en uptimeMillis. */
    private long departMs;
    /** Le reste courant de l'annonce, en millisecondes (borné). */
    private long resteMs;
    /** La respiration de la jauge, en pixels. */
    private final int ecartJaugePx;
    /** L'épaisseur de la jauge, en pixels. */
    private final float epaisseurJaugePx;
    /** Le rectangle de la jauge — réutilisé à chaque image. */
    private final RectF jauge = new RectF();

    /**
     * Construit la carte sur la palette du thème courant.
     *
     * @param context le contexte de la couche
     * @param palette la palette courante des popups
     */
    public NotificationView(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.ecartJaugePx = Math.round(PopupTokens.ECART_DP * densite);
        this.epaisseurJaugePx = EPAISSEUR_JAUGE_DP * densite;
        setOnPick(this::prendre);
    }

    /**
     * Pose l'annonce : le titre au palier titre, le message au corps.
     *
     * @param titre   le titre de l'annonce, jamais null ni blanc
     * @param message le message de l'annonce
     */
    public final void setAnnonce(String titre, String message) {
        setAnnonce(titre, message, List.of());
    }

    /**
     * Pose l'annonce avec ses actions — l'audit Android Studio : les
     * liens de la bulle suivent le message derrière un filet, à
     * l'encre d'accent.
     *
     * @param titre    le titre de l'annonce, jamais null ni blanc
     * @param message  le message de l'annonce, ses lignes comprises
     * @param actions  les liens de la bulle, vides pour aucune
     */
    public final void setAnnonce(String titre, String message,
            List<Action> actions) {
        this.titre = titre;
        this.actions = List.copyOf(actions);
        List<Row> rangees = new ArrayList<>();
        rangees.add(Row.tier(titre, -1, Tier.TITRE));
        for (String ligne : message.split("\n", -1)) {
            rangees.add(Row.of(ligne));
        }
        boolean premier = true;
        for (Action lien : this.actions) {
            Row rangee = Row.tier(lien.libelle(), palette.accent(),
                    Tier.CORPS);
            rangees.add(premier ? Row.apresFilet(rangee) : rangee);
            premier = false;
        }
        setRows(rangees);
    }

    /** Le titre de l'annonce posée. */
    public final String titre() {
        return titre;
    }

    /**
     * Le rappel du tap qui PREND la bulle — l'annonce ne survit pas au
     * geste qui la lit ; l'hôte y referme le genre.
     *
     * @param surPrise le rappel de la prise, null pour couper
     */
    public final void setSurPrise(@Nullable Runnable surPrise) {
        this.surPrise = surPrise;
    }

    /**
     * Arme l'horloge de l'annonce : à son heure, l'expiration court —
     * la course d'avant se DISSOUT d'abord (la queue du fil ne sert
     * que la dernière annonce). Le TICK de la jauge s'arme avec elle :
     * le reste se sert à chaque pas, la carte se redessine — l'horloge
     * qui coule se voit.
     *
     * @param dureeMs     la durée de vie de l'annonce, en millisecondes
     * @param expiration  la course de fin — l'hôte y referme le genre
     */
    public final void armer(long dureeMs, Runnable expiration) {
        desarmer();
        course = expiration;
        this.dureeMs = Math.max(0L, dureeMs);
        this.departMs = SystemClock.uptimeMillis();
        this.resteMs = this.dureeMs;
        horloge.postDelayed(expiration, this.dureeMs);
        if (this.dureeMs > 0L) {
            tick = this::tic;
            horloge.postDelayed(tick, TICK_MS);
        }
        requestLayout();
        invalidate();
    }

    /** Désarme l'horloge : la course et le tick en cours se
     *  dissolvent, jamais ils ne partent — la fermeture venue
     *  d'ailleurs coupe l'annonce ET sa jauge. */
    public final void desarmer() {
        Runnable courante = course;
        if (courante != null) {
            horloge.removeCallbacks(courante);
        }
        Runnable ticCourant = tick;
        if (ticCourant != null) {
            horloge.removeCallbacks(ticCourant);
        }
        course = null;
        tick = null;
        dureeMs = 0L;
    }

    /**
     * La PROGRESSION de la jauge — 1 à l'armement, 0 à l'expiration ;
     * sans horloge armée, la carte ne promet rien : 1. Les bancs y
     * lisent la coulée sans passer par le rendu.
     */
    public final float progression() {
        if (dureeMs <= 0L) {
            return 1f;
        }
        return resteMs / (float) dureeMs;
    }

    /** Le reste courant de l'annonce, en millisecondes — la légende
     *  de la jauge s'y sert, les bancs y vérifient la coulée. */
    public final long resteMs() {
        return resteMs;
    }

    /**
     * La légende du compte à rebours — « l'horloge la referme — X,X s »,
     * au dixième près, la virgule du terrain. Pure et publique : les
     * bancs y vérifient la phrase sans horloge.
     *
     * @param resteMs le reste, en millisecondes
     * @return la légende de la jauge
     */
    public static String phraseRebours(long resteMs) {
        long borne = Math.max(0L, resteMs);
        // au dixième : 1 234 ms → « 1,2 s » (l'arrondi du dixième)
        long dixiemes = (borne + 50L) / 100L;
        return "l'horloge la referme — " + (dixiemes / 10L) + ","
                + (dixiemes % 10L) + " s";
    }

    /** Le pas de la jauge : le reste se resert au vif, la carte se
     *  redessine — le tick s'arrête à l'épuisement, l'expiration
     *  conclut seule. */
    private void tic() {
        if (dureeMs <= 0L || tick == null) {
            return;
        }
        resteMs = Math.max(0L, Math.min(dureeMs,
                dureeMs - (SystemClock.uptimeMillis() - departMs)));
        invalidate();
        if (resteMs > 0L) {
            horloge.postDelayed(tick, TICK_MS);
        }
    }

    /** La prise d'une rangée : la bulle cède d'abord (l'ordre de
     *  l'IDE — une action qui re-pose n'est jamais tuée par sa propre
     *  fermeture), le LIEN pris court ensuite, la prise du reste ne
     *  referme que. */
    private void prendre(int rang) {
        desarmer();
        Runnable prise = surPrise;
        if (prise != null) {
            prise.run();
        }
        int index = rang - decalage();
        if (index >= 0 && index < actions.size()) {
            actions.get(index).action().run();
        }
    }

    /** Le décalage des liens — le titre et les lignes du message
     *  comptent leurs rangées. */
    private int decalage() {
        return rowCount() - actions.size();
    }

    /** Vrai quand l'horloge court — la jauge et sa légende ne se
     *  tracent que promise. */
    private boolean jaugeArmee() {
        return dureeMs > 0L;
    }

    @Override
    void drawContent(Canvas canvas) {
        super.drawContent(canvas);
        if (!jaugeArmee()) {
            return;
        }
        float y = super.contentHeight() + ecartJaugePx;
        dessinerJauge(canvas, y);
        dessinerLegende(canvas, y + epaisseurJaugePx
                + ecartJaugePx * 0.5f);
    }

    /**
     * La jauge : piste muette pleine largeur (le texte à dix pour cent
     * d'alpha — la piste du simulateur), remplissage à l'encre
     * d'accent au prorata du reste, coins arrondis à la moitié de
     * l'épaisseur — le filet qui coule.
     */
    private void dessinerJauge(Canvas canvas, float y) {
        float largeur = contentWidth();
        bandPaint().setColor((palette.text() & 0x00FFFFFF) | 0x1A000000);
        jauge.set(0f, y, largeur, y + epaisseurJaugePx);
        canvas.drawRoundRect(jauge, epaisseurJaugePx * 0.5f,
                epaisseurJaugePx * 0.5f, bandPaint());
        bandPaint().setColor(palette.accent());
        jauge.set(0f, y, largeur * progression(), y + epaisseurJaugePx);
        canvas.drawRoundRect(jauge, epaisseurJaugePx * 0.5f,
                epaisseurJaugePx * 0.5f, bandPaint());
    }

    /** La légende du compte à rebours — micro-typographie muette,
     *  la phrase de la jauge sous la barre. */
    private void dessinerLegende(Canvas canvas, float y) {
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_MICRO));
        textPaint().setColor(palette.muted());
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        canvas.drawText(phraseRebours(resteMs), 0f,
                y - metriques.top, textPaint());
    }

    @Override
    int contentWidth() {
        float largeur = super.contentWidth();
        if (jaugeArmee()) {
            textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_MICRO));
            largeur = Math.max(largeur, textPaint().measureText(
                    phraseRebours(dureeMs)));
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        int hauteur = super.contentHeight();
        if (jaugeArmee()) {
            textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_MICRO));
            Paint.FontMetrics metriques = textPaint().getFontMetrics();
            hauteur += Math.round(ecartJaugePx + epaisseurJaugePx
                    + ecartJaugePx * 0.5f
                    + (metriques.bottom - metriques.top));
        }
        return hauteur;
    }
}
