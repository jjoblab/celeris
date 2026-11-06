package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le respect de `.editorconfig` (B-26) : retrait, fins de ligne et
 * encodage pilotés par le fichier — les sections se matchent par
 * extension, la plus spécifique gagne, les racines portent les
 * défauts, les clés inconnues sont ignorées.
 */
class EditorConfigTest {

    private static final String CONFIG = """
            root = true

            [*]
            indent_style = space
            indent_size = 4
            end_of_line = lf
            charset = utf-8

            [*.kt]
            indent_size = 2

            [*.xml]
            indent_style = tab
            """;

    /** Les sections : la plus spécifique gagne, les racines portent les défauts. */
    @Test
    void lesSectionsParExtension() {
        EditorConfig config = EditorConfig.analyser(CONFIG);
        EditorConfig.Regles java = config.pour("src/Main.java");
        assertEquals(false, java.indentStyle().tabs());
        assertEquals(4, java.indentStyle().width());
        assertEquals("lf", java.finDeLigne());
        assertEquals("utf-8", java.encodage());
        // le Kotlin : indent_size de sa section, le reste des racines
        EditorConfig.Regles kotlin = config.pour("src/Main.kt");
        assertEquals(2, kotlin.indentStyle().width());
        assertEquals(false, kotlin.indentStyle().tabs());
        assertEquals("lf", kotlin.finDeLigne());
        // le XML : tabulations de sa section, taille de quatre des racines
        EditorConfig.Regles xml = config.pour("res/layout.xml");
        assertTrue(xml.indentStyle().tabs());
        assertEquals(4, xml.indentStyle().width());
    }

    /** Le chemin complet : c'est son extension qui décide de la section. */
    @Test
    void leCheminCompletSeMatcheParSaFin() {
        EditorConfig config = EditorConfig.analyser(CONFIG);
        // un chemin profond porte l'extension qui décide
        assertEquals(2, config.pour("a/b/c/d/Serveur.kt").indentStyle().width());
        assertEquals(4, config.pour("a/b/c/d/Serveur.java").indentStyle().width());
    }

    /** Sans section qui matche : les défauts canoniques, jamais d'exception. */
    @Test
    void sansSectionQuiMatche() {
        EditorConfig config = EditorConfig.analyser("[*]\nindent_style = space\n");
        EditorConfig.Regles regles = config.pour("autre.md");
        assertEquals(IndentStyle.ESPACES_4, regles.indentStyle());
        assertEquals("", regles.finDeLigne());
        assertEquals("", regles.encodage());
    }

    /** Le fichier vide : défauts canoniques. */
    @Test
    void leFichierVide() {
        EditorConfig config = EditorConfig.analyser("");
        assertEquals(IndentStyle.ESPACES_4, config.pour("x.java").indentStyle());
    }

    /** Les fins de ligne : la règle convertit le texte. */
    @Test
    void lesFinsDeLigne() {
        EditorConfig config = EditorConfig.analyser(
                "[*]\nend_of_line = crlf\nindent_style = space\n");
        EditorConfig.Regles regles = config.pour("x.txt");
        assertEquals("crlf", regles.finDeLigne());
        assertEquals("a\r\nb\r\n", EditorConfig.appliquerFins("a\nb\n", regles));
        // et le retour : lf ramène les retours chariots
        EditorConfig lf = EditorConfig.analyser("[*]\nend_of_line = lf\n");
        assertEquals("a\nb\n", EditorConfig.appliquerFins("a\r\nb\r\n", lf.pour("x.txt")));
        // sans règle : le texte reste tel quel
        EditorConfig muet = EditorConfig.analyser("[*]\nindent_style = space\n");
        assertEquals("a\r\nb\n", EditorConfig.appliquerFins("a\r\nb\n", muet.pour("x.txt")));
    }

    /** Les clés inconnues et les commentaires : ignorés sans casse. */
    @Test
    void lesClesInconnues() {
        EditorConfig config = EditorConfig.analyser("""
                # un commentaire
                [*]
                indent_style = tab
                indent_size = 8
                cle_inconnue = valeur
                """);
        EditorConfig.Regles regles = config.pour("x");
        assertTrue(regles.indentStyle().tabs());
        assertEquals(8, regles.indentStyle().width());
    }

    /** L'indent_size à tab : la largeur suit le style tab, quatre par défaut. */
    @Test
    void laTailleDeTabulation() {
        EditorConfig config = EditorConfig.analyser(
                "[*]\nindent_style = tab\nindent_size = 2\n");
        assertEquals(2, config.pour("x").indentStyle().width());
        // tab_style sans taille : la convention de largeur 4
        EditorConfig sansTaille = EditorConfig.analyser("[*]\nindent_style = tab\n");
        assertEquals(4, sansTaille.pour("x").indentStyle().width());
    }
}
