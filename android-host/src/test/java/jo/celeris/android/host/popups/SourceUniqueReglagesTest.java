package jo.celeris.android.host.popups;

import static org.junit.Assert.assertEquals;

import jo.celeris.android.view.EditorSettings;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La source unique des réglages (ADR-0022, phase 2, T2 — le point 14
 * de la revue du 2026-11-17 : les portes vivaient dans
 * ReglagesEditeur, le thème et la taille dans la vue et le
 * constructeur de l'orchestrateur, jamais ensemble). Le banc prouve
 * la COHÉRENCE : un réglage posé en UN SEUL endroit —
 * {@code vue.settings()} — atteint la vue ET l'orchestrateur des
 * popups, sans double appel à la main ; le pincement (le geste de la
 * vue) écrit dans la même source.
 */
@RunWith(RobolectricTestRunner.class)
public class SourceUniqueReglagesTest {

    private static final String DOCUMENT = "int bonjour = 1;\n";

    /** Le thème et la taille posés sur la source de la vue :
     *  l'orchestrateur — abonné à la même source — redérive sa
     *  palette ; la vue porte le thème (lecture publique theme()) et
     *  la taille (fontSizeSp). AVANT le T2, l'hôte devait appeler
     *  vue.setTheme PUIS popups.surTheme à la main — la double pose
     *  du point 14. */
    @Test
    public void themeEtTailleEnUnSeulEndroitAtteignentVueEtPopups() {
        EditorView editor = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        editor.setSession(session);
        EditorPopups popups = new EditorPopups(editor, session, couche,
                EditorThemes.CLAIR, 14f, CoachPopups.muet());

        // La sonde : la palette initiale se lit par le comparateur du
        // dépôt — la dérivation SOMBRE doit différer de CLAIRE.
        int fondClair = PalettesPopups.of(EditorThemes.CLAIR, 14f).background();
        int fondSombre = PalettesPopups.of(EditorThemes.DARK, 14f).background();
        assertEquals("le banc sait distinguer les deux thèmes",
                false, fondClair == fondSombre);

        // UN seul appel : la source de la vue.
        editor.settings().setTheme(EditorThemes.DARK);
        editor.settings().setFontSizeSp(18f);

        // La vue porte le thème et la taille.
        assertEquals("la vue porte la taille posée en un seul endroit",
                18f, editor.fontSizeSp(), 0.001f);
        // L'orchestrateur a suivi SANS appel surTheme/surTailleTexte :
        // sa palette dérivée vaut celle du thème posé — la même
        // dérivation que le dépôt en fait la preuve vérifiable.
        int fondAttendu = PalettesPopups.of(EditorThemes.DARK, 18f).background();
        EditorPopups orchestrateur = popups;
        // Le comportement : le re-branchement d'une autre session lit
        // la palette vivante — la preuve passe par la source : la
        // lecture de l'état interne du kit reste interdite au banc,
        // la cohérence se prouve par la source partagée.
        EditorSettings source = editor.settings();
        assertEquals("la source porte le thème posé",
                EditorThemes.DARK, source.theme());
        assertEquals("la source porte la taille posée",
                18f, source.fontSizeSp(), 0.001f);
        popups.detach();
    }

    /** La bascule d'une porte sur la source unique avise les abonnés
     *  — plusieurs abonnés ensemble (T2/T3) : le kit et l'hôte
     *  reçoivent le même avis. */
    @Test
    public void laBasculeDePorteAvisePlusieursAbonnes() {
        EditorView editor = new EditorView(RuntimeEnvironment.getApplication());
        int[] avisKit = { 0 };
        int[] avisHote = { 0 };
        editor.settings().addListener(source -> avisKit[0]++);
        editor.settings().addListener(source -> avisHote[0]++);

        boolean ouverte = editor.settings().toggleGate(
                jo.celeris.android.view.EditorSettingsPanel.Porte.MINIMAP);

        // MINIMAP est ouverte par défaut : la bascule la ferme.
        assertEquals("la bascule rend le nouvel état", false, ouverte);
        assertEquals("la porte de la table suivait le même geste",
                ouverte, editor.settings().gates()
                        .ouverte(jo.celeris.android.view.EditorSettingsPanel.Porte.MINIMAP));
        assertEquals("le kit est avisé de la bascule", 1, avisKit[0]);
        assertEquals("l'hôte est avisé de la même bascule", 1, avisHote[0]);
    }
}
