package jo.celeris.android.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte de notification (catégorie G de l'inventaire — la forme
 * restait à décider, la bulle éphémère l'emporte) : l'annonce se pose
 * en titre et message, l'HORLOGE la referme à son heure ({@link #armer}
 * — la queue du FIL UI la porte, jamais l'éditeur), le tap LA PREND la
 * referme ({@link #setSurPrise} — le Runnable de l'hôte seul décide).
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

    /** Le titre de l'annonce — vide avant la première pose. */
    private String titre = "";
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
    }

    /**
     * Pose l'annonce : le titre au palier titre, le message au corps.
     *
     * @param titre   le titre de l'annonce, jamais null ni blanc
     * @param message le message de l'annonce
     */
    public final void setAnnonce(String titre, String message) {
        this.titre = titre;
        setRows(List.of(
                Row.tier(titre, -1, Tier.TITRE),
                Row.of(message)));
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
        setOnPick(surPrise == null ? null : rang -> {
            desarmer();
            surPrise.run();
        });
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
}
