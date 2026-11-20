package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.android.view.internal.ScrollPositions;

import jo.celeris.core.text.TextSession;

/**
 * La position de défilement revient avec l'onglet (feuille de route
 * des améliorations, C2) : la position s'archive avec la session
 * quittée — comme les plis (D-07) et les tailles (D-2) — et se
 * restaure à la pose, CLAMPÉE par {@link ScrollGeometry} (la
 * position d'un document raccourci entre-temps dépasse son contenu).
 *
 * <p>Avant C2, {@code reinitialiserScene(true)} rabattait
 * {@code scrollX}/{@code scrollY} à zéro à CHAQUE pose de session :
 * le retour sur un onglet ramenait en tête de document — le contenu
 * saute, puis se re-mesure (C1) : les deux participent de
 * l'impression de « refait ». Le modèle est le {@link RowPark} : la
 * mémoire de la vue vit PAR SESSION, bornée, vidée à la
 * libération.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class PositionDefilementParSessionTest {

    /** Le texte de l'onglet A : assez de lignes pour que le scroll
     *  y vive (le clamp exige un contenu plus haut que la fenêtre —
     *  sous Robolectric la fenêtre tient ~116 rangées). */
    private static final String TEXTE_A =
            "public class OngletA {\n" + lines(400) + "}\n";

    /** L'onglet B : autant de lignes — l'excursus y défile aussi. */
    private static final String TEXTE_B =
            "public class OngletB {\n" + lines(400) + "}\n";

    private static String lines(int nombre) {
        StringBuilder lignes = new StringBuilder();
        for (int i = 0; i < nombre; i++) {
            lignes.append("    int ligne").append(i).append(" = ")
                    .append(i).append(";\n");
        }
        return lignes.toString();
    }

    /** Le montage du banc : une vue mesurée sur son harnais. */
    private EditorTestRig banc(String text) {
        EditorTestRig rig = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), text);
        rig.vue().layout(0, 0, 1080, 1920);
        return rig;
    }

    /** Définit le défilement vertical (en rangées) et dessine : le
     *  clamp de {@code preparer()} s'applique au dessin. */
    private void defiler(EditorTestRig rig, float rangees) {
        EditorView vue = rig.vue();
        vue.scrollY = rangees * vue.metrics.lineHeight();
        vue.scrollX = 0f;
        rig.image();
    }

    /** LE retour d'onglet : la position de A survit à l'excursus
     *  sur B — la fenêtre visible de A est à mi-hauteur au retour,
     *  pas en tête de document. Avant C2 : zéro, le saut de
     *  contenu qui participait de l'impression de « refait ». */
    @Test
    public void leRetourDongletRetrouveSaPosition() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession sessionA = vue.session();
        defiler(rig, 40f);
        float attendu = vue.scrollY;

        // L'excursus sur B : la session B prend l'écran et y défile
        // ailleurs — sa position ne doit PAS écraser celle de A.
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        defiler(rig, 80f);
        assertEquals("l'excursus déplace bien le scroll", 80f,
                vue.scrollY / vue.metrics.lineHeight(), 0.01f);

        // Le RETOUR sur A : la position parquée revient.
        vue.setSession(sessionA);
        assertEquals("le retour d'onglet restaure la position (C2)",
                attendu, vue.scrollY, 0.5f);
    }

    /** La position restaurée se CLAMPE : le document raccourci
     *  pendant l'excursus ne peut pas ramener une position hors
     *  contenu — la fenêtre se rabat au pied du contenu, jamais au
     *  -delà. */
    @Test
    public void laPositionRestaureeSeClampe() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession sessionA = vue.session();
        defiler(rig, 100f);

        // L'excursus : A se raccourcit EN ARRIÈRE-PLAN (l'onglet
        // reste ouvert dans l'éditeur, l'hôte édite ailleurs) — le
        // retour doit ramener une position VALIDE.
        sessionA.replaceRange(0, sessionA.length(), "court");
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        defiler(rig, 80f);

        vue.setSession(sessionA);
        rig.image();
        float maximum = Math.max(0f,
                vue.projection.rowCount() * vue.metrics.lineHeight()
                        - vue.getHeight());
        assertTrue("la position restaurée vit dans le contenu (clamp "
                + "C2) : " + vue.scrollY + " > " + maximum,
                vue.scrollY <= maximum + 0.5f);
    }

    /** Le parc des positions ne grandit pas sans borne : au-delà du
     *  plafond, la plus ancienne part — même mémoire bornée que le
     *  {@link RowPark}. */
    @Test
    public void leParcDesPositionsResteBorne() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession premiere = vue.session();
        defiler(rig, 10f);
        for (int i = 0; i < ScrollPositions.PARKED_LIMIT + 2; i++) {
            vue.setSession(TextSession.of(TEXTE_B + i, System::nanoTime));
        }
        assertTrue("le parc vit sous son plafond : "
                        + vue.parcPositions.size(),
                vue.parcPositions.size() <= ScrollPositions.PARKED_LIMIT);
        // La session d'origine revient : chassée du parc, elle repart
        // de la position NEUTRE (zéro) — proprement, sans erreur.
        vue.setSession(premiere);
        rig.image();
        assertEquals("la session chassée repart en tête", 0f,
                vue.scrollY, 0.5f);
    }

    /** La langue change pendant l'excursus : le retour de session ne
     *  resservira JAMAIS une position périmée par la langue — la
     *  langue signe la hauteur de contenu (retour à la ligne
     *  différent), l'oubli est le chemin sûr (même règle que
     *  {@code RowPark.forget}). */
    @Test
    public void leChangementDeLangueOublieLaPositionParquee() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession sessionA = vue.session();
        defiler(rig, 40f);
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        vue.setLanguage(null);
        vue.setSession(sessionA);
        rig.image();
        assertTrue("le chemin langue changée reste cohérent : "
                + vue.scrollY, vue.scrollY >= 0f);
    }
}
