package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;



/**
 * PIEGE-19 — la cascade de commentaires de bloc est bornée par la
 * reconvergence des états.
 *
 * <p>L'édition de l'ouverture d'un commentaire de bloc invalide l'état de
 * toutes les lignes suivantes jusqu'à la fermeture : la re-lexicographie
 * doit relire exactement ces lignes, s'arrêter à la fermeture (jamais
 * relire la suite du document) et réutiliser les paniers au-delà à
 * l'identité. Un commentaire ouvert sans fermeture cascade jusqu'à la
 * dernière ligne et termine — jamais de boucle infinie.</p>
 */
class CascadeCommentaireTest {

    private static final int EMPTY_LINES = 50_000;
    private static final int OUVERTURE = 100;
    private static final int FERMETURE = 40_000;

    /**
     * Piège hérité du constat du spike (ADR-004) : la suppression de
     * l'ouverture relit exactement les lignes du commentaire, pas la suite.
     */
    @Test
    void commentCascade_boundedByStateConvergence() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int debutOuverture = doc.lineStart(OUVERTURE);
        Document edite = doc.replace(debutOuverture, debutOuverture + 2, "//");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, debutOuverture, debutOuverture + 2, "//"));

        assertEquals(OUVERTURE, diff.firstLine(),
                "la cascade démarre à la ligne éditée");
        assertEquals(FERMETURE, diff.lastLine(),
                "la cascade s'arrête à la fermeture du commentaire, pas après");

        for (int line = FERMETURE + 1; line < edite.lineCount(); line++) {
            assertSame(complet.lex().line(line), diff.lex().line(line),
                    "la ligne " + line + " au-delà de la fermeture doit être réutilisée");
        }
        assertSame(MiniLangage.State.NORMAL, diff.lex().line(FERMETURE).exitState());

        LexResult temoin = moteur.lexFull(edite, lexeur());
        for (int line = OUVERTURE; line <= FERMETURE; line++) {
            assertEquals(temoin.lex().line(line).exitState(),
                    diff.lex().line(line).exitState(),
                    "état reconvergé de la ligne " + line);
        }
    }

    /** Une ouverture ajoutée en amont cascade jusqu'à la fermeture existante. */
    @Test
    void ouvertureAjouteeCascadeJusquaFermetureExistante() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int target = doc.lineStart(50);
        Document edite = doc.replace(target, target, "/* ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, target, target, "/* "));
        assertEquals(50, diff.firstLine());
        assertEquals(OUVERTURE, diff.lastLine(),
                "les états se rejoignent à l'ouverture préexistante du grand commentaire");
        for (int line = OUVERTURE + 1; line < edite.lineCount(); line++) {
            assertSame(complet.lex().line(line), diff.lex().line(line));
        }
    }

    /** Un commentaire ouvert sans fermeture cascade jusqu'à la fin et termine. */
    @Test
    void ouvertureSansFermetureCascadeJusquaLaFin() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int target = doc.lineStart(45_000);
        Document edite = doc.replace(target, target, "/* isole");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, target, target, "/* isole"));
        assertEquals(45_000, diff.firstLine());
        assertEquals(edite.lineCount() - 1, diff.lastLine(),
                "sans fermeture, la cascade va jusqu'à la dernière ligne — et termine");
        for (int line = 45_000; line < edite.lineCount(); line++) {
            assertEquals(MiniLangage.State.COMMENTAIRE,
                    diff.lex().line(line).exitState(),
                    "la ligne " + line + " reste dans le commentaire ouvert");
        }
    }

    /** Éditer une ligne DANS le commentaire ne relit que la convergence voisine. */
    @Test
    void editionDansLeCommentaireResteLocale() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int target = doc.lineStart(20_000);
        Document edite = doc.replace(target, target, "x ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, target, target, "x "));
        assertEquals(20_000, diff.firstLine());
        assertEquals(20_000, diff.lastLine(),
                "l'état d'entrée et de sortie ne change pas : une seule ligne relue");
    }

    private static Document document() {
        StringBuilder text = new StringBuilder();
        for (int line = 0; line < EMPTY_LINES; line++) {
            if (line == OUVERTURE) {
                text.append("/* ouverture du commentaire\n");
            } else if (line == FERMETURE) {
                text.append("fermeture */ code\n");
            } else {
                text.append("ligne ").append(line).append('\n');
            }
        }
        return Document.of(text.toString());
    }

    private static LanguageLexer lexeur() {
        return MiniLangage.langage().lexer();
    }
}
