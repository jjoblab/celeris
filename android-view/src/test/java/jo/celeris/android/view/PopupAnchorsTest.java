package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import android.graphics.Rect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * L'ancrage des popups (M7.5, §7) : la vue expose le rectangle du
 * curseur et celui de la sélection en coordonnées de vue, et prévient
 * l'hôte du changement de fenêtre — défilement, police — sans jamais
 * notifier deux fois le même état.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupAnchorsTest {

    private static final String DOCUMENT = "int a = 1;\nint b = 2;\n";

    private EditorTestRig banc() {
        return EditorTestRig.sur(RuntimeEnvironment.getApplication(), DOCUMENT);
    }

    /**
     * Le rectangle du curseur : bord gauche du caractère visé, hauteur
     * de la rangée — l'ancre attendue des popups de complétion.
     */
    @Test
    public void caretBoundsFollowsTheOffset() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        vue.session().setSelection(MultiSelection.of(SelRange.caretAt(0)));
        Rect origine = vue.caretBounds();
        assertEquals(vue.largeurGouttiere, origine.left, 0.5f,
                "le curseur d'origine vit au bord droit de la gouttière");
        assertEquals(vue.metrics.lineHeight(), origine.height(), 0.5f,
                "la hauteur du rectangle est celle de la rangée");

        // Ligne suivante, colonne 3 : l'ancre suit le décalage exact.
        vue.session().setSelection(MultiSelection.of(SelRange.caretAt(
                DOCUMENT.indexOf('b'))));
        Rect vise = vue.caretBounds();
        assertEquals(origine.top + vue.metrics.lineHeight(), vise.top, 0.5f,
                "une rangée plus bas");
        assertEquals(origine.left + 4 * vue.metrics.charWidth(), vise.left,
                0.5f, "quatre colonnes plus à droite (int )");
    }

    /**
     * Le rectangle de la sélection : l'union des extrémités — une plage
     * vide rend le rectangle du curseur, une plage pleine couvre la
     * largeur remplie.
     */
    @Test
    public void selectionBoundsSpansTheRange() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        vue.session().setSelection(MultiSelection.of(SelRange.caretAt(4)));
        Rect vide = vue.selectionBounds();
        assertEquals(vue.caretBounds(), vide,
                "une plage vide rend le rectangle du curseur");

        vue.session().setSelection(MultiSelection.of(new SelRange(4, 9)));
        Rect pleine = vue.selectionBounds();
        assertEquals(5 * vue.metrics.charWidth(), pleine.width(), 1f,
                "cinq colonnes couvertes");
        assertEquals(vue.metrics.lineHeight(), pleine.height(), 0.5f,
                "une seule rangée de haut");
        assertTrue(pleine.width() > 0, "la plage pleine couvre une largeur");
    }

    /**
     * L'avis de fenêtre : le défilement notifie UNE fois par état — le
     * second passage au même état se tait (le clignotement ne notifie
     * pas).
     */
    @Test
    public void viewportChangeNotifiesOncePerState() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        StringBuilder contenu = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            contenu.append("ligne ").append(i).append('\n');
        }
        vue.setSession(jo.celeris.core.text.TextSession.of(
                contenu.toString(), System::nanoTime));
        int[] avis = { 0 };
        vue.setSurViewportChange(() -> avis[0]++);
        banc.image();
        assertEquals(1, avis[0], "le premier état pose l'avis");
        banc.image();
        assertEquals(1, avis[0], "le même état ne re-notifie pas");

        vue.scrollY = 240f;
        banc.image();
        assertEquals(2, avis[0], "le défilement notifie");
        banc.image();
        assertEquals(2, avis[0], "le défilement posé ne re-notifie pas");
    }

    /** Le changement de police notifie — les popups se replacent au zoom. */
    @Test
    public void fontChangeNotifies() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        int[] avis = { 0 };
        vue.setSurViewportChange(() -> avis[0]++);
        banc.image();
        assertEquals(1, avis[0]);
        vue.setFont(new FontSpec("mono", 20f));
        banc.image();
        assertEquals(2, avis[0], "la nouvelle police déplace les ances");
        assertEquals(vue.caretBounds().height(),
                Math.round(vue.metrics.lineHeight()),
                "l'ancre suit les nouvelles métriques");
    }
}
