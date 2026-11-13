package jo.celeris.android.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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
 * PIEGE-27 : jamais de focus — l'annonce s'affiche, elle ne règne pas ;
 * les régimes l'épargnent (frappe, défilement, geste extérieur — elle
 * annonce, elle ne décrit pas le texte), seule son heure la conclut.
 *
 * <p>La re-pose réarme l'horloge : {@link #armer} dissout la course
 * d'avant — la queue du fil ne sert que la DERNIÈRE annonce, jamais
 * deux courses ne se cumulent. La carte ne connaît NI l'éditeur, NI le
 * coordinateur : l'expiration rend à l'hôte, qui referme le genre.</p>
 */
public final class NotificationView extends TextRowsPopup {

    /** Une action de l'annonce — le lien de la bulle : son libellé et
     *  ce qu'elle fait. */
    public record Action(String libelle, Runnable action) {
    }

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

    /**
     * Construit la carte sur la palette du thème courant.
     *
     * @param context le contexte de la couche
     * @param palette la palette courante des popups
     */
    public NotificationView(Context context, PopupPalette palette) {
        super(context, palette);
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
     * que la dernière annonce).
     *
     * @param dureeMs     la durée de vie de l'annonce, en millisecondes
     * @param expiration  la course de fin — l'hôte y referme le genre
     */
    public final void armer(long dureeMs, Runnable expiration) {
        desarmer();
        course = expiration;
        horloge.postDelayed(expiration, dureeMs);
    }

    /** Désarme l'horloge : la course en cours se dissout, jamais elle
     *  ne part — la fermeture venue d'ailleurs coupe l'annonce. */
    public final void desarmer() {
        Runnable courante = course;
        if (courante != null) {
            horloge.removeCallbacks(courante);
        }
        course = null;
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
}
