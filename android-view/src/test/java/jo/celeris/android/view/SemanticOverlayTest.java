package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.TextSession;

/**
 * Le banc de fusion de la surcouche sémantique (ADR-0027, B1) : le
 * jeton RAFFINE — il ne colore que l'intersection avec la preuve
 * syntaxique dessous ; l'orphelin s'ignore ; le périmé s'ignore
 * aussi (PIEGE-20) — jamais un jeton ne colore une révision plus
 * récente que celle que le serveur a vue.
 */
class SemanticOverlayTest {

    /** Trois plages syntaxiques : identificateur, type, identificateur. */
    private static List<ColoredSpan> plagesSyntaxiques() {
        return List.of(
                new ColoredSpan(TokenKind.IDENTIFIER, 0, 7, 0),
                new ColoredSpan(TokenKind.IDENTIFIER, 8, 13, 0),
                new ColoredSpan(TokenKind.IDENTIFIER, 14, 21, 0));
    }

    @Test
    void leJetonRaffineLaSorteDeLaPlageQuiLePorte() {
        // « marge » (offsets 0-7) est un PARAMÈTRE selon le serveur :
        // la plage syntaxique identificateur devient identificateur…
        // sauf que le type LSP « parameter » raffine en IDENTIFIER —
        // prenons une fonction : le serveur voit « marge » FONCTION.
        SemanticOverlay surcouche = SemanticOverlay.of(
                new int[] {0, 5}, new int[] {0},
                List.of("function"), 42L);
        List<ColoredSpan> raffinees = surcouche.raffiner(
                plagesSyntaxiques(), 0, 21);
        assertEquals(TokenKind.KEYWORD, raffinees.get(0).kind(),
                "la part couverte par le jeton fonction prend la sorte raffinée");
        assertEquals(0, raffinees.get(0).start());
        assertEquals(5, raffinees.get(0).end());
        assertEquals(TokenKind.IDENTIFIER, raffinees.get(1).kind(),
                "la part non couverte garde la sorte syntaxique");
        assertEquals(5, raffinees.get(1).start());
        assertEquals(7, raffinees.get(1).end());
        // les autres plages ne bougent pas
        assertEquals(TokenKind.IDENTIFIER, raffinees.get(2).kind());
    }

    @Test
    void leJetonOrphelinNeColoreRien() {
        // un jeton sur les blancs (7-8) : AUCUNE preuve syntaxique
        // dessous — la règle 1 de l'ADR l'ignore.
        SemanticOverlay surcouche = SemanticOverlay.of(
                new int[] {7, 8}, new int[] {0},
                List.of("function"), 42L);
        List<ColoredSpan> originales = plagesSyntaxiques();
        List<ColoredSpan> rendues = surcouche.raffiner(originales, 0, 21);
        assertSame(originales, rendues,
                "sans jeton actif sur une preuve, la MÊME liste revient");
    }

    @Test
    void leJetonQuiRecouvreDeuxPlagesLesCoupeToutesDeux() {
        // le jeton (2-10) chevauche la première plage (0-7) et la
        // deuxième (8-13) : chaque intersection se colore, les blancs
        // entre les deux restent à la syntaxe (rien dessous).
        SemanticOverlay surcouche = SemanticOverlay.of(
                new int[] {2, 10}, new int[] {0},
                List.of("function"), 42L);
        List<ColoredSpan> raffinees = surcouche.raffiner(
                plagesSyntaxiques(), 0, 21);
        assertEquals(TokenKind.IDENTIFIER, raffinees.get(0).kind(),
                "l'avant du jeton garde la syntaxe");
        assertEquals(0, raffinees.get(0).start());
        assertEquals(2, raffinees.get(0).end());
        assertEquals(TokenKind.KEYWORD, raffinees.get(1).kind());
        assertEquals(2, raffinees.get(1).start());
        assertEquals(7, raffinees.get(1).end());
        assertEquals(TokenKind.KEYWORD, raffinees.get(2).kind(),
                "la deuxième plage coupée prend aussi le raffinement");
        assertEquals(8, raffinees.get(2).start());
        assertEquals(10, raffinees.get(2).end());
        assertEquals(TokenKind.IDENTIFIER, raffinees.get(3).kind());
        assertEquals(10, raffinees.get(3).start());
    }

    @Test
    void leTypeInconnuDeLaLegendeResteMuet() {
        // une légende propriétaire (« magic ») : le mapping n'invente
        // rien, le jeton s'ignore.
        SemanticOverlay surcouche = SemanticOverlay.of(
                new int[] {0, 5}, new int[] {0},
                List.of("magic"), 42L);
        List<ColoredSpan> originales = plagesSyntaxiques();
        assertSame(originales, surcouche.raffiner(originales, 0, 21),
                "un type hors mapping ne change aucune plage");
    }

    @Test
    void lesJetonsRecouvrantsSeRognentLePremierEmporte() {
        // deux jetons qui se recouvrent (2-8) et (4-12) : le premier
        // l'emporte, le second se rogne (8-12).
        SemanticOverlay surcouche = SemanticOverlay.of(
                new int[] {2, 8, 4, 12}, new int[] {0, 1},
                List.of("function", "class"), 42L);
        List<ColoredSpan> raffinees = surcouche.raffiner(
                plagesSyntaxiques(), 0, 21);
        // la part 2-7 : fonction (le premier jeton)
        assertEquals(TokenKind.KEYWORD, raffinees.get(1).kind());
        assertEquals(2, raffinees.get(1).start());
        assertEquals(7, raffinees.get(1).end());
        // la part 8-12 : classe (le second jeton, rogné à ce qui reste)
        assertEquals(TokenKind.TYPE, raffinees.get(2).kind());
        assertEquals(8, raffinees.get(2).start());
        assertEquals(12, raffinees.get(2).end());
        // la part 12-13 : retour à la syntaxe
        assertEquals(TokenKind.IDENTIFIER, raffinees.get(3).kind());
        assertEquals(12, raffinees.get(3).start());
    }

    @Test
    void laSurcoucheVideNeChangeRien() {
        List<ColoredSpan> originales = plagesSyntaxiques();
        assertSame(originales,
                SemanticOverlay.vide().raffiner(originales, 0, 21));
        assertTrue(SemanticOverlay.vide().sansJetons());
    }

    @Test
    void leJetonPerimeNeColorePasLaRevisionPlusRecente() {
        // le serveur a vu la révision 42, le document affiché est à
        // 43 : la publication s'ignore (PIEGE-20) — le recalage fin
        // vivra chez l'hôte, qui sait l'édition.
        TextSession session = TextSession.of("abcdef\nghijkl\n");
        SemanticOverlay perimee = SemanticOverlay.of(
                new int[] {0, 6}, new int[] {0},
                List.of("function"), 42L);
        SemanticOverlay recalee = perimee.recaler(session.document());
        assertTrue(recalee.sansJetons(),
                "une publication périmée sur une révision plus récente se vide");
        // la même révision survit : le texte n'a pas bougé
        SemanticOverlay meme = SemanticOverlay.of(
                new int[] {0, 6}, new int[] {0},
                List.of("function"), session.document().revision());
        assertSame(meme, meme.recaler(session.document()));
    }
}
