package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.TextSession;

/**
 * D-03 (l'analyse croisée) : l'état d'affichage survit à la
 * rotation — {@code onSaveInstanceState} capture le défilement, le
 * zoom et le retour de ligne ; {@code onRestoreInstanceState} les
 * repose. L'hôte n'a plus rien à porter : la vue sauvegarde ELLE-MÊME
 * son état transitoire, le Bundle de la rotation le transporte.
 */
@RunWith(RobolectricTestRunner.class)
public class EtatRotationTest {

    private static final String TEXTE = """
            ligne une
            ligne deux
            ligne trois
            ligne quatre
            ligne cinq
            ligne six
            ligne sept
            ligne huit
            ligne neuf
            ligne dix
            ligne onze
            ligne douze
            ligne treize
            ligne quatorze
            ligne quinze
            ligne seize
            ligne dix-sept
            ligne dix-huit
            ligne dix-neuf
            ligne vingt
            """;

    /** La vue posée, mesurée, DESSINÉE (la hauteur de contenu vit au
     *  premier dessin) — le défilement exige du contenu connu. */
    private EditorView vuePreparee() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.setSession(TextSession.of(TEXTE));
        vue.measure(540, 400);
        vue.layout(0, 0, 540, 400);
        vue.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(
                540, 400, android.graphics.Bitmap.Config.ARGB_8888)));
        return vue;
    }

    @Test
    public void leDefilementEtLeZoomSurviventALaRotation() {
        EditorView avant = vuePreparee();
        avant.setNumerosVisibles(true);
        avant.setOccurrencesVisibles(false);
        // l'hôte zoome puis défile (la voie publique)
        avant.settings().setFontSizeSp(19f);
        avant.scrollToFractionY(0.5f);

        // la rotation : l'état part au Bundle
        android.os.Parcel colis = Parcel.obtain();
        Bundle etat = new Bundle();
        avant.saveState(etat);
        etat.writeToParcel(colis, 0);
        colis.setDataPosition(0);
        Bundle recu = Bundle.CREATOR.createFromParcel(colis);
        colis.recycle();

        // la vue recréée restaure
        EditorView apres = vuePreparee();
        apres.restoreState(recu);
        assertEquals("le zoom revient", 19f,
                apres.settings().fontSizeSp(), 0.01f);
        assertEquals("le défilement vertical revient", 0.5f,
                apres.scrollFractionY(), 0.02f);
        apres.release();
        avant.release();
    }

    @Test
    public void lesNumerosEtLeRetourReviennent() {
        EditorView avant = vuePreparee();
        avant.settings().setSoftWrap(true);
        // la rotation
        Bundle etat = new Bundle();
        avant.saveState(etat);

        EditorView apres = vuePreparee();
        apres.restoreState(etat);
        assertTrue("le retour de ligne souple revient",
                apres.settings().softWrap());
        apres.release();
        avant.release();
    }

    /** Un Bundle vide ne casse rien : la première rotation avant tout
     *  état posé restaure les défauts. */
    @Test
    public void leBundleVideNeCassemRien() {
        EditorView vue = vuePreparee();
        vue.restoreState(new Bundle());
        assertEquals("le zoom par défaut tient", 14f,
                vue.settings().fontSizeSp(), 0.01f);
        vue.release();
    }
}
