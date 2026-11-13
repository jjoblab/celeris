package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

/**
 * La notification de l'éditeur (2026-11-14) : la carte porte son
 * titre et son message, l'horloge (4 000 ms) referme seule — la
 * RÉ-POSE ré-arme sans mourir du retard de l'ancienne (la course du
 * retrait ne se paie jamais), le désarmement retire l'horloge en vol.
 */
@RunWith(RobolectricTestRunner.class)
public class NotificationViewTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private void mesurer(NotificationView vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
    }

    /** La carte se mesure et l'horloge la referme seule — quatre
     *  secondes, l'expiration court une fois. */
    @Test
    public void laCarteVitQuatreSecondesPuisExpire() {
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        AtomicInteger expire = new AtomicInteger();
        vue.setMessage("Serveur de langage",
                "Le mini serveur Java a démarré.", expire::incrementAndGet);
        mesurer(vue);
        assertTrue("la carte se mesure",
                vue.getMeasuredWidth() > 0 && vue.getMeasuredHeight() > 0);
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
        assertEquals("l'expiration court une fois", 1, expire.get());
    }

    /** La ré-pose ré-arme : l'ancienne horloge se retire AVANT que la
     *  neuve ne parte — jamais le retard de la morte ne referme la
     *  neuve (la course du retrait ne se paie jamais deux fois). */
    @Test
    public void laRePoseReArmeSansMourirDuRetard() {
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        AtomicInteger expire = new AtomicInteger();
        vue.setMessage("Sauvegarde", "Fichier écrit.", expire::incrementAndGet);
        vue.setMessage("Serveur", "Diagnostics publiés.",
                expire::incrementAndGet);
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
        assertEquals("seule la DERNIÈRE expiration court", 1, expire.get());
    }

    /** Le désarmement retire l'horloge en vol — la carte fermée
     *  (tap hors cartes, voile, échappement) ne referme rien de retard. */
    @Test
    public void desarmerRetireLhorlogeEnVol() {
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        AtomicInteger expire = new AtomicInteger();
        vue.setMessage("Agent", "Revue terminée.", expire::incrementAndGet);
        vue.desarmer();
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
        assertEquals("aucune expiration ne court", 0, expire.get());
    }
}
