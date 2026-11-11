package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import android.view.DragEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.DragEventBuilder;

import org.jspecify.annotations.Nullable;

/**
 * Le glisser-déposer (chantier C13, D-20) : le dépôt de texte s'insère au
 * point de chute par la voie de transaction (ADR-003), le dépôt de
 * fichier part à l'hôte par le rappel — la vue n'ouvre jamais (le
 * scénario de la matrice : dépôt de fichier → ouverture). Le texte a la
 * précédence : un dépôt qui porte les deux insère le texte, l'uri suit
 * le rappel. Le geste vide refuse : la vue n'accepte que ce qu'elle sait
 * déposer. Le banc passe par la voie publique — le vrai DragEvent,
 * distribué par la vue.
 */
@RunWith(RobolectricTestRunner.class)
public class GlisserDeposerTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Un vrai DragEvent du geste, distribué par la vue. */
    private static DragEvent geste(int action, float x, float y,
            @Nullable ClipData pressePapiers) {
        return DragEventBuilder.newBuilder().setAction(action).setX(x).setY(y)
                .setClipData(pressePapiers).build();
    }

    /**
     * Le banc attaché : la vue vit dans la fenêtre d'une activité — le
     * chemin du geste passe par le vrai dispatch de la vue.
     */
    private EditorTestRig bancAttache(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(contexte, contenu);
        Activity hote = ActivityController.of(new Activity()).setup().get();
        hote.setContentView(banc.vue());
        banc.image();
        return banc;
    }

    /** Le dépôt de texte s'insère au point de chute — une transaction,
     *  l'annulation la défait. */
    @Test
    public void leDepotDeTexteSinsereAuPointDeChute() {
        EditorTestRig banc = bancAttache("aaaa\nbbbb\n");
        EditorView vue = banc.vue();
        // le point de chute : la rangée 1, deux colonnes après le bord
        float x = vue.largeurGouttiere + 2 * vue.metrics.charWidth();
        float y = vue.metrics.lineHeight() + vue.metrics.lineHeight() / 2f;
        assertTrue("le dépôt se consomme", vue.dispatchDragEvent(
                geste(DragEvent.ACTION_DROP, x, y,
                        ClipData.newPlainText("texte", "XY"))));
        assertEquals("le texte tombe au point visé", "aaaa\nbbXYbb\n",
                vue.session().document().text());
        vue.session().undo();
        assertEquals("l'annulation défait le dépôt", "aaaa\nbbbb\n",
                vue.session().document().text());
    }

    /** Le dépôt de fichier part à l'hôte — le scénario de la matrice :
     *  dépôt de fichier → ouverture, l'hôte décide, le document tient. */
    @Test
    public void leDepotDeFichierVaALhote() {
        EditorTestRig banc = bancAttache("aaaa\n");
        EditorView vue = banc.vue();
        List<String> recues = new ArrayList<>();
        vue.setSurFichierDepose(recues::add);
        ClipData pressePapiers = new ClipData("fichier",
                new String[] {"application/octet-stream"},
                new ClipData.Item(Uri.parse("content://media/externe/42")));
        assertTrue("le dépôt se consomme", vue.dispatchDragEvent(
                geste(DragEvent.ACTION_DROP, 10f, 10f, pressePapiers)));
        assertEquals("l'uri part à l'hôte par le rappel",
                List.of("content://media/externe/42"), recues);
        assertEquals("le document ne bouge pas", "aaaa\n",
                vue.session().document().text());
    }

    /** Le texte a la précédence : le dépôt qui porte les deux insère le
     *  texte ET envoie l'uri. */
    @Test
    public void leTexteAPrecedenceSurLeFichier() {
        EditorTestRig banc = bancAttache("aaaa\n");
        EditorView vue = banc.vue();
        List<String> recues = new ArrayList<>();
        vue.setSurFichierDepose(recues::add);
        ClipData pressePapiers = new ClipData("double",
                new String[] {"text/plain"}, new ClipData.Item("XY"));
        pressePapiers.addItem(
                new ClipData.Item(Uri.parse("content://media/externe/1")));
        assertTrue("le dépôt se consomme", vue.dispatchDragEvent(
                geste(DragEvent.ACTION_DROP, vue.largeurGouttiere,
                        10f, pressePapiers)));
        assertEquals("le texte s'insère au point de chute", "XYaaaa\n",
                vue.session().document().text());
        assertEquals("l'uri suit le rappel",
                List.of("content://media/externe/1"), recues);
    }

    /** Le geste vide refuse : la vue n'accepte que ce qu'elle sait
     *  déposer, rien ne se pose. */
    @Test
    public void leGesteVideRefuse() {
        EditorTestRig banc = bancAttache("aaaa\n");
        EditorView vue = banc.vue();
        // le geste sans presse-papiers ne s'accepte pas
        assertFalse("rien à déposer, le geste ne s'accepte pas",
                vue.dispatchDragEvent(geste(DragEvent.ACTION_DRAG_STARTED,
                        0f, 0f, null)));
        // le geste de texte s'accepte
        assertTrue("le texte se dépose, le geste s'accepte",
                vue.dispatchDragEvent(geste(DragEvent.ACTION_DRAG_STARTED,
                        0f, 0f, ClipData.newPlainText("texte", "XY"))));
        // le dépôt vide ne pose rien
        assertFalse("le dépôt vide ne pose rien", vue.dispatchDragEvent(
                geste(DragEvent.ACTION_DROP, 10f, 10f,
                        ClipData.newPlainText("vide", ""))));
        assertEquals("le document tient", "aaaa\n",
                vue.session().document().text());
    }

    /** La lecture seule refuse le texte déposé — rien ne bouge, l'uri
     *  sans rappel se tait. */
    @Test
    public void laLectureSeuleRefuseLeDepotDeTexte() {
        EditorTestRig banc = bancAttache("aaaa\n");
        EditorView vue = banc.vue();
        vue.session().setReadOnly(true);
        assertFalse("la lecture seule refuse le texte", vue.dispatchDragEvent(
                geste(DragEvent.ACTION_DROP, 10f, 10f,
                        ClipData.newPlainText("texte", "XY"))));
        assertEquals("le document tient", "aaaa\n",
                vue.session().document().text());
    }
}
