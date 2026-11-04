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

    private static final int LIGNES = 50_000;
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
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int debutOuverture = doc.lineStart(OUVERTURE);
        Document edite = doc.replace(debutOuverture, debutOuverture + 2, "//");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, debutOuverture, debutOuverture + 2, "//"));

        assertEquals(OUVERTURE, diff.firstLine(),
                "la cascade démarre à la ligne éditée");
        assertEquals(FERMETURE, diff.lastLine(),
                "la cascade s'arrête à la fermeture du commentaire, pas après");
        assertEquals(FERMETURE - OUVERTURE + 1, diff.lastLine() - diff.firstLine() + 1);

        for (int ligne = FERMETURE + 1; ligne < LIGNES; ligne++) {
            assertSame(complet.lex().line(ligne), diff.lex().line(ligne),
                    "la ligne " + ligne + " au-delà de la fermeture doit être réutilisée");
        }
        assertSame(MiniLangage.Etat.NORMAL, diff.lex().line(FERMETURE).exitState());

        LexResult temoin = moteur.lexFull(edite, lexeur());
        for (int ligne = OUVERTURE; ligne <= FERMETURE; ligne++) {
            assertEquals(temoin.lex().line(ligne).exitState(),
                    diff.lex().line(ligne).exitState(),
                    "état reconvergé de la ligne " + ligne);
        }
    }

    /** Une ouverture ajoutée en amont cascade jusqu'à la fermeture existante. */
    @Test
    void ouvertureAjouteeCascadeJusquaFermetureExistante() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int cible = doc.lineStart(50);
        Document edite = doc.replace(cible, cible, "/* ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, cible, cible, "/* "));
        assertEquals(50, diff.firstLine());
        assertEquals(FERMETURE, diff.lastLine());
        for (int ligne = FERMETURE + 1; ligne < LIGNES; ligne++) {
            assertSame(complet.lex().line(ligne), diff.lex().line(ligne));
        }
    }

    /** Un commentaire ouvert sans fermeture cascade jusqu'à la fin et termine. */
    @Test
    void ouvertureSansFermetureCascadeJusquaLaFin() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int cible = doc.lineStart(45_000);
        Document edite = doc.replace(cible, cible, "/* isole");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, cible, cible, "/* isole"));
        assertEquals(45_000, diff.firstLine());
        assertEquals(LIGNES - 1, diff.lastLine(),
                "sans fermeture, la cascade va jusqu'à la dernière ligne — et termine");
        for (int ligne = 45_000; ligne < LIGNES; ligne++) {
            assertEquals(MiniLangage.Etat.COMMENTAIRE,
                    diff.lex().line(ligne).exitState(),
                    "la ligne " + ligne + " reste dans le commentaire ouvert");
        }
    }

    /** Éditer une ligne DANS le commentaire ne relit que la convergence voisine. */
    @Test
    void editionDansLeCommentaireResteLocale() {
        Document doc = document();
        IncrementalLexer moteur = new IncrementalLexer();
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        LexResult complet = moteur.lexFull(doc, lexeur());

        int cible = doc.lineStart(20_000);
        Document edite = doc.replace(cible, cible, "x ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(doc, edite, cible, cible, "x "));
        assertEquals(20_000, diff.firstLine());
        assertEquals(20_000, diff.lastLine(),
                "l'état d'entrée et de sortie ne change pas : une seule ligne relue");
    }

    private static Document document() {
        StringBuilder texte = new StringBuilder();
        for (int ligne = 0; ligne < LIGNES; ligne++) {
            if (ligne == OUVERTURE) {
                texte.append("/* ouverture du commentaire\n");
            } else if (ligne == FERMETURE) {
                texte.append("fermeture */ code\n");
            } else {
                texte.append("ligne ").append(ligne).append('\n');
            }
        }
        return Document.of(texte.toString());
    }
}
