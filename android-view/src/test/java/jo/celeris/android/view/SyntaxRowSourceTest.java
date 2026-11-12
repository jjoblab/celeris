package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la source de rangées colorées (B-01) : le décorateur greffe
 * sur chaque mise en page les plages de son jetons — le rendu coloré
 * lit le même arbre lexicographique que les plis et les diagnostics
 * (ADR-004 : un seul arbre, tous les consommateurs).
 *
 * <p>Les plages couvrent la rangée EXACTEMENT, sans trou à peindre : le
 * lexeur soude les jetons (les blancs appartiennent au jeton suivant),
 * et tout écart résiduel se comble à l'identificateur — l'encre de base
 * par défaut.</p>
 */
class SyntaxRowSourceTest {

    /** Règle fixe : largeur 10 px, hauteur 20 px, ascendance 16, descendance 4. */
    private static final class RegleFixe implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return 10f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return 20f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return 16f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 4f;
        }
    }

    /** Source de rangées de test : sans pli ni coupure, la rangée r porte la ligne r. */
    private static final class SourceParLigne implements RowSource {
        private final Document document;
        private final EditorMetrics metrics;

        SourceParLigne(Document document, EditorMetrics metrics) {
            this.document = document;
            this.metrics = metrics;
        }

        @Override
        public RowLayout layoutOf(RowKey key) {
            String texte = document.lineText(key.row());
            return new RowLayout(key.row(), 0, texte.length(), texte,
                    texte.length() * metrics.charWidth());
        }
    }

    /** Source à tranche imposée : la rangée porte [debut, fin) de sa ligne (A-18). */
    private static final class SourceATranche implements RowSource {
        private final Document document;
        private final int debut;
        private final int fin;

        SourceATranche(Document document, int debut, int fin) {
            this.document = document;
            this.debut = debut;
            this.fin = fin;
        }

        @Override
        public RowLayout layoutOf(RowKey key) {
            String ligne = document.lineText(key.row());
            String tranche = ligne.substring(debut, Math.min(fin, ligne.length()));
            return new RowLayout(key.row(), debut, debut + tranche.length(), tranche,
                    tranche.length() * 10f);
        }
    }

    private static LexDocument lexique(String texte) {
        return SyntaxService.of(JavaSyntax.INSTANCE)
                .analyseFull(Document.of(texte)).lex();
    }

    /** Concatène les plages colorées : le texte reconstitué, preuve de couverture exacte. */
    private static String reconstruit(RowLayout mise) {
        StringBuilder texte = new StringBuilder();
        for (ColoredSpan plage : mise.spans()) {
            texte.append(mise.text(), plage.start(), plage.end());
        }
        return texte.toString();
    }

    /** Les plages couvrent la rangée exactement, sans trou ni chevauchement. */
    @Test
    void lesPlagesCouvrentLaRangeeExactement() {
        Document document = Document.of("int x = 42;");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, metrics), lexique("int x = 42;"));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        assertEquals("int x = 42;", mise.text());
        assertEquals("int x = 42;", reconstruit(mise),
                "les plages doivent reconstituer le texte de la rangée sans trou");
        int curseur = 0;
        for (ColoredSpan plage : mise.spans()) {
            assertEquals(curseur, plage.start(),
                    "la plage suivante commence où la précédente finit (jetons soudés)");
            assertTrue(plage.end() > plage.start());
            curseur = plage.end();
        }
        assertEquals(mise.text().length(), curseur, "la dernière plage ferme la rangée");
    }

    /** Les sortes attendues : mot réservé, nombre, ponctuation — la couleur suit le jeton. */
    @Test
    void lesSortesSuiventLesJetons() {
        Document document = Document.of("return 42;");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, metrics), lexique("return 42;"));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        List<ColoredSpan> plages = mise.spans();
        assertTrue(plages.size() >= 3, "au moins trois plages : return, 42, point-virgule");
        assertEquals(TokenKind.KEYWORD, plages.get(0).kind());
        assertEquals("return", mise.text().substring(plages.get(0).start(),
                plages.get(0).end()));
        assertEquals(TokenKind.NUMBER, plages.get(1).kind());
        assertEquals(" 42", mise.text().substring(plages.get(1).start(),
                plages.get(1).end()),
                "le blanc soudé au nombre appartient à sa plage (jetons soudés)");
    }

    /** Sous retour à la ligne (A-18), la rangée ne porte que sa tranche : les plages sont rognées. */
    @Test
    void lesPlagesSontRoigneesALaTranche() {
        String ligne = "String message = \"bonjour\";";
        Document document = Document.of(ligne);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceATranche(document, 17, 26), lexique(ligne));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        assertEquals("\"bonjour\"", mise.text());
        assertEquals("\"bonjour\"", reconstruit(mise),
                "les plages rognées reconstituent la tranche, rien de plus");
        assertEquals(1, mise.spans().size());
        assertEquals(TokenKind.STRING, mise.spans().get(0).kind());
        assertEquals(0, mise.spans().get(0).start());
        assertEquals(9, mise.spans().get(0).end());
    }

    /** La tranche peut couper un jeton en deux : les plages suivent les bords de la rangée. */
    @Test
    void laTrancheCoupeUnJeton() {
        String ligne = "// commentaire entier de ligne";
        Document document = Document.of(ligne);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceATranche(document, 4, 12), lexique(ligne));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        assertEquals("ommentai", reconstruit(mise));
        assertEquals(1, mise.spans().size());
        assertEquals(TokenKind.COMMENT, mise.spans().get(0).kind());
    }

    /** Un commentaire de bloc multi-lignes colore la ligne suivante entière (état transporté). */
    @Test
    void commentaireMultiLignesColoreLaSuite() {
        String texte = "/* debut\nsuite du bloc */ int apres;";
        Document document = Document.of(texte);
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, metrics), lexique(texte));
        RowLayout ligneDeux = source.layoutOf(
                new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));

        assertEquals("suite du bloc */ int apres;", reconstruit(ligneDeux));
        assertEquals(TokenKind.COMMENT, ligneDeux.spans().get(0).kind(),
                "la ligne 2 vit toujours dans le commentaire (état transporté)");
        assertEquals(0, ligneDeux.spans().get(0).start());
    }

    /** Une rangée vide reste sur le chemin rapide : aucune plage, l'encre de base seule. */
    @Test
    void rangeeVideSansPlage() {
        Document document = Document.of("");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, metrics), lexique(""));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        assertTrue(mise.spans().isEmpty(), "une rangée vide n'a rien à colorer");
    }

    /** La source déléguée sert les autres champs (ligne, bornes, largeur) tels quels. */
    @Test
    void laSourceDelegueeServitLesAutresChamps() {
        String ligne = "int x = 42;";
        Document document = Document.of(ligne);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceATranche(document, 0, 5), lexique(ligne));
        RowLayout mise = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));

        assertEquals(0, mise.line());
        assertEquals(0, mise.start());
        assertEquals(5, mise.end());
        assertEquals(50f, mise.width(), 1e-6f);
        assertTrue(mise.runs().isEmpty());
    }

    /**
     * Sous rattrapage (publication périmée), la ligne épargnée par
     * l'édition garde les plages de SA ligne du document publié — les
     * jetons relatifs à la ligne restent exacts (ADR-004, PIEGE-26).
     */
    @Test
    void rattapeeLaLigneEpargneeGardeSesPlages() {
        // Publié : trois lignes ; l'édition remplace « b » par « bb » à
        // la ligne 1 (delta nul) : les lignes 0 et 2 sont épargnées.
        String publie = "int a = 1;\nint b = 2;\nint c = 3;";
        String edite = "int a = 1;\nint bb = 2;\nint c = 3;";
        Document document = Document.of(edite);
        TokenCatchUp rattrapage = TokenCatchUp.of(lexique(publie),
                List.of(new TokenCatchUp.PendingEdit(4, 1, 2, 1, false)),
                List.of(new EditSite(1, 1, 0)), document, null);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, EditorMetrics.of(
                        new FontSpec("mono", 14f), new RegleFixe())),
                lexique(publie), rattrapage);

        RowLayout ligneDeux = source.layoutOf(
                new RowKey(1L, 2, 1, new FontSpec("mono", 14f)));
        assertEquals("int c = 3;", reconstruit(ligneDeux),
                "la ligne épargnée se couvre exactement");
        assertTrue(ligneDeux.spans().size() >= 3, "mot réservé, nom, nombre : les plages du publié");
        assertEquals(TokenKind.KEYWORD, ligneDeux.spans().get(0).kind());
    }

    /**
     * PIEGE-26 — la ligne touchée par l'édition en attente garde ses
     * couleurs : ses jetons publiés se recalent, le mot réservé reste
     * un mot réservé, le nombre un nombre.
     */
    @Test
    void rattapeeLaLigneToucheeGardeSesCouleurs() {
        String publie = "int a = 1;\nint b = 2;\nint c = 3;";
        String edite = "int a = 1;\nint bb = 2;\nint c = 3;";
        Document document = Document.of(edite);
        TokenCatchUp rattrapage = TokenCatchUp.of(lexique(publie),
                List.of(new TokenCatchUp.PendingEdit(4, 1, 2, 1, false)),
                List.of(new EditSite(1, 1, 0)), document, null);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, EditorMetrics.of(
                        new FontSpec("mono", 14f), new RegleFixe())),
                lexique(publie), rattrapage);

        RowLayout ligneUne = source.layoutOf(
                new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));
        assertEquals("int bb = 2;", reconstruit(ligneUne),
                "la ligne touchée se couvre exactement");
        assertEquals(TokenKind.KEYWORD, ligneUne.spans().get(0).kind(),
                "le mot réservé garde sa couleur à travers l'édition");
    }

    /**
     * Une insertion de ligne (édition structurelle) décale les
     * suivantes : la ligne décalée porte les plages de l'ANCIENNE ligne
     * à son index publié ; la ligne vide insérée, sans lexeur à défaut
     * de langage, reste à l'encre de base.
     */
    @Test
    void rattapeeLigneInsereeDecaleLesSuivantes() {
        // Publié : deux lignes ; l'édition insère une ligne vide en tête.
        String publie = "int a = 1;\nint b = 2;";
        String edite = "\nint a = 1;\nint b = 2;";
        Document document = Document.of(edite);
        TokenCatchUp rattrapage = TokenCatchUp.of(lexique(publie),
                List.of(new TokenCatchUp.PendingEdit(0, 0, 1, 0, true)),
                List.of(new EditSite(0, 0, 1)), document, null);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, EditorMetrics.of(
                        new FontSpec("mono", 14f), new RegleFixe())),
                lexique(publie), rattrapage);

        RowLayout ligneInseree = source.layoutOf(
                new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));
        assertTrue(ligneInseree.spans().isEmpty(),
                "la ligne insérée vide n'a rien à colorer");
        RowLayout ligneDecalee = source.layoutOf(
                new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));
        assertEquals("int a = 1;", reconstruit(ligneDecalee),
                "la ligne décalée porte les plages de sa ligne publiée");
        assertEquals(TokenKind.KEYWORD, ligneDecalee.spans().get(0).kind());
    }

    /**
     * PIEGE-26 — deux éditions consécutives en attente se composent :
     * la ligne garde ses couleurs à travers les deux frappes.
     */
    @Test
    void rattapeeDeuxEditionsConsecutives() {
        String publie = "int a = 1;";
        String edite = "int axy = 1;";
        Document document = Document.of(edite);
        // « x » puis « y » insérés après le a — la seconde en coordonnées
        // publiées (4), la chirurgie compose les deux.
        TokenCatchUp rattrapage = TokenCatchUp.of(lexique(publie),
                List.of(new TokenCatchUp.PendingEdit(4, 0, 1, 0, false),
                        new TokenCatchUp.PendingEdit(4, 0, 1, 0, false)),
                List.of(new EditSite(0, 0, 0), new EditSite(0, 0, 0)),
                document, null);
        SyntaxRowSource source = new SyntaxRowSource(
                new SourceParLigne(document, EditorMetrics.of(
                        new FontSpec("mono", 14f), new RegleFixe())),
                lexique(publie), rattrapage);
        RowLayout ligne = source.layoutOf(
                new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));
        assertEquals("int axy = 1;", reconstruit(ligne),
                "les deux insertions composées couvrent la ligne");
        assertEquals(TokenKind.KEYWORD, ligne.spans().get(0).kind());
    }
}
