package jo.celeris.android.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La notification de l'éditeur (2026-11-14) : une carte éphémère au
 * coin bas-droit de la scène — le TITRE au palier titre, le MESSAGE à
 * l'encre du texte. Ni la frappe ni le défilement ne la ferment (le
 * texte ne la regarde pas), le tap hors cartes la chasse, l'horloge la
 * referme seule : {@link #setMessage} ARME l'expiration (4 000 ms) et
 * la ré-arme — la carte re-posée ne meurt jamais du retard de
 * l'ancienne ({@link #desarmer} retire l'horloge en vol, jamais la
 * course du retrait ne se paie). L'ancre vit chez
 * {@link PopupAnchor#bottomRightCorner}.
 */
public final class NotificationView extends TextRowsPopup {

    /** La durée de vie d'une notification, en millisecondes. */
    public static final long DUREE_MS = 4000L;

    /** L'horloge du fil de scène — l'expiration y vole en retard. */
    private final Handler horloge;
    /** L'expiration armée — null quand aucune horloge ne vole. */
    private @Nullable Runnable expiration;

    public NotificationView(Context context, PopupPalette palette) {
        super(context, palette);
        horloge = new Handler(Looper.getMainLooper());
    }

    /**
     * Pose le titre et le message, ARME l'expiration : la carte vit
     * quatre secondes, l'ancienne horloge se retire AVANT que la
     * neuve ne parte (la ré-pose ne meurt jamais du retard de la
     * morte).
     *
     * @param titre     le titre de l'avis, au palier titre
     * @param message   le message de l'avis, à l'encre du texte
     * @param expirer   la course de l'expiration — la fermeture y vit
     */
    public void setMessage(String titre, String message, Runnable expirer) {
        List<Row> rangees = new ArrayList<>();
        rangees.add(Row.tier(titre, -1, Tier.TITRE));
        for (String ligne : message.split("\n", -1)) {
            rangees.add(Row.of(ligne));
        }
        setRows(rangees);
        desarmer();
        expiration = expirer;
        horloge.postDelayed(expirer, DUREE_MS);
    }

    /**
     * L'horloge se désarme : l'expiration en vol se retire — la carte
     * fermée (tap hors cartes, voile, échappement) ne referme rien de
     * retard, la carte re-posée vit SA durée.
     */
    public void desarmer() {
        if (expiration != null) {
            horloge.removeCallbacks(expiration);
            expiration = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // la carte quitte la scène : l'horloge en vol se retire — le
        // peek retardé n'arme plus sur une carte fermée (P6, la course
        // du retrait différé ne se paie jamais deux fois)
        desarmer();
        super.onDetachedFromWindow();
    }
}
