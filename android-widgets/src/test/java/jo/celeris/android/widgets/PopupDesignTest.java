package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Les contrats de design du kit de popups (portage du dépôt de
 * départ) : le VERRE se dérive du fond de l'éditeur — alpha 0xDB sur
 * thème sombre, 0xE0 sur thème clair, liseré blanc à dix pour cent ou
 * noir à huit pour cent —, les jetons gardent leurs valeurs (rayons,
 * respirations, échelle typographique), les badges portent leurs
 * glyphes et leur palette calibrée, la signature découpe ses plages de
 * paramètres, la bande porte ses gravités.
 */
public class PopupDesignTest {

    /** Le verre d'un thème sombre : fond à alpha 0xDB, liseré blanc à 10 %. */
    @Test
    public void verreSombreDeriveFondEtLisere() {
        PopupPalette palette = PopupPalette.verre(0xFF1E1E1E, 0xFFD4D4D4,
                0xFF858585, 0xFF264F78, 0xFFDCDCAA, 0xFF569CD6, 14f);
        assertEquals("le fond garde l'éditeur à alpha 0xDB",
                0xDB1E1E1E, palette.background());
        assertEquals("le liseré est blanc à dix pour cent",
                0x1AFFFFFF, palette.border());
        assertEquals("les encres suivent le thème", 0xFFD4D4D4,
                palette.text());
        assertEquals("la teinte de sélection suit", 0xFF264F78,
                palette.selection());
        assertEquals("l'accent suit", 0xFFDCDCAA, palette.accent());
        assertEquals("la gravité erreur du dépôt de départ",
                0xFFF44747, palette.error());
        assertEquals("la gravité avertissement du dépôt de départ",
                0xFFCCA700, palette.warning());
    }

    /** Le verre d'un thème clair : fond à alpha 0xE0, liseré noir à 8 %. */
    @Test
    public void verreClairDeriveFondEtLisere() {
        PopupPalette palette = PopupPalette.verre(0xFFFFFFFF, 0xFF000000,
                0xFF808080, 0xFFADD6FF, 0xFF795E26, 0xFF0000FF, 14f);
        assertEquals("le fond garde l'éditeur à alpha 0xE0",
                0xE0FFFFFF, palette.background());
        assertEquals("le liseré est noir à huit pour cent",
                0x14000000, palette.border());
        assertEquals("la gravité erreur du thème clair du dépôt de départ",
                0xFFFF0000, palette.error());
        assertEquals("la gravité avertissement du thème clair",
                0xFFCC7800, palette.warning());
    }

    /** Les palettes de référence passent par la même dérivation du verre. */
    @Test
    public void referencesPassentParLeVerre() {
        assertEquals(0xDB1E1E1E, PopupPalette.dark().background());
        assertEquals(0x1AFFFFFF, PopupPalette.dark().border());
        assertEquals(0xE0FFFFFF, PopupPalette.light().background());
        assertEquals(0x14000000, PopupPalette.light().border());
        assertEquals("la base typographique de référence est celle de l'éditeur",
                14f, PopupPalette.dark().textBaseSp(), 0f);
    }

    /** Les jetons du kit gardent les valeurs du dépôt de départ. */
    @Test
    public void lesJetonsGardentLeursValeurs() {
        assertEquals(14f, PopupTokens.RAYON_CARTE_DP, 0f);
        assertEquals(8f, PopupTokens.RAYON_RANGEE_DP, 0f);
        assertEquals(6f, PopupTokens.RAYON_PUCE_DP, 0f);
        assertEquals(12f, PopupTokens.RESPIRATION_X_DP, 0f);
        assertEquals(8f, PopupTokens.RESPIRATION_Y_DP, 0f);
        assertEquals(6f, PopupTokens.ECART_DP, 0f);
        assertEquals(0.90f, PopupTokens.TEXTE_TITRE, 0f);
        assertEquals(0.85f, PopupTokens.TEXTE_CORPS, 0f);
        assertEquals(0.75f, PopupTokens.TEXTE_META, 0f);
        assertEquals(0.70f, PopupTokens.TEXTE_MICRO, 0f);
        assertEquals(280f, PopupTokens.LARGEUR_MAX_LISTE_DP, 0f);
        assertEquals(40f, PopupTokens.HAUTEUR_RANGEE_LISTE_DP, 0f);
    }

    /** Les badges : glyphe et palette calibrée, l'assombrissement du clair. */
    @Test
    public void lesBadgesPortentGlypheEtPalette() {
        BadgeGenre.Meta motCle = BadgeGenre.meta(BadgeGenre.Genre.MOT_CLE,
                true, 0xFFDCDCAA);
        assertEquals("K", motCle.glyphe());
        assertEquals("la palette sombre est calibrée telle quelle",
                0xFFCD7EE0, motCle.couleur());
        BadgeGenre.Meta methode = BadgeGenre.meta(BadgeGenre.Genre.METHODE,
                true, 0xFF123456);
        assertEquals("M", methode.glyphe());
        assertEquals("la méthode prend l'accent du thème", 0xFF123456,
                methode.couleur());
        BadgeGenre.Meta classeClaire = BadgeGenre.meta(BadgeGenre.Genre.CLASSE,
                false, 0xFF000000);
        assertEquals("le thème clair assombrit (mélange noir à 38 %)",
                0xFF8E774A, classeClaire.couleur());
        assertEquals("un glyphe long se réduit", 0.42f,
                BadgeGenre.facteurTailleGlyphe("{}"), 0f);
        assertEquals("un glyphe court garde sa taille", 0.56f,
                BadgeGenre.facteurTailleGlyphe("K"), 0f);
    }

    /** La signature découpe ses plages de paramètres, sans les bords blancs. */
    @Test
    public void laSignatureDecoupeSesPlages() {
        int[][] plages = SignatureHelpView.plagesParametres(
                "ajouter(int a, int b)");
        assertEquals(2, plages.length);
        assertEquals("int a", "ajouter(int a, int b)"
                .substring(plages[0][0], plages[0][1]));
        assertEquals("int b", "ajouter(int a, int b)"
                .substring(plages[1][0], plages[1][1]));
        assertTrue("sans parenthèses, aucune plage",
                SignatureHelpView.plagesParametres("sans") == null);
        int[][] imriquees = SignatureHelpView.plagesParametres(
                "f(int[] m, Map p, int n)");
        assertEquals("les virgules imbriquées ne coupent pas", 3,
                imriquees.length);
        int[][] tableaux = SignatureHelpView.plagesParametres(
                "f(int[] t, g(char[] u, byte v))");
        assertEquals("la virgule du tableau imbriqué ne coupe pas", 2,
                tableaux.length);
    }

    /** La bande d'erreurs porte ses gravités — la marque colore à l'encre. */
    @Test
    public void laBandePorteSesGravites() {
        jo.celeris.core.text.Document doc = jo.celeris.core.text.Document.of(
                "a\nb\nc\n");
        java.util.List<jo.celeris.core.syntax.SyntaxIssue> issues =
                java.util.List.of(
                        new jo.celeris.core.syntax.SyntaxIssue(0, 1, "erreur"),
                        new jo.celeris.core.syntax.SyntaxIssue(2, 1,
                                "avertissement"));
        ErrorBandModel defaut = ErrorBandModel.of(issues, doc, 10);
        assertEquals("sans gravités, l'erreur tient lieu",
                ErrorBandModel.GRAVITE_ERREUR,
                defaut.marques().get(0).gravite());
        ErrorBandModel typee = ErrorBandModel.of(issues,
                new int[] {ErrorBandModel.GRAVITE_ERREUR, 2}, doc, 10);
        assertEquals("la gravité posée se porte",
                2, typee.marques().get(1).gravite());
    }
}
