package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Random;

import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Le rattrapage des jetons d'une publication périmée (M7.5, §6,
 * PIEGE-26) : la ligne touchée garde ses couleurs — ses jetons publiés
 * se recalent sur l'édition, le préfixe non édité garde ses plages À
 * L'IDENTIQUE, la couverture reste exacte, et la relecture prend le
 * relais quand la structure change.
 */
class TokenCatchUpTest {

    private static LexDocument lexicon(String text) {
        return SyntaxService.of(JavaSyntax.INSTANCE)
                .analyseFull(Document.of(text)).lex();
    }

    /**
     * Taper dans un identificateur ne change pas sa couleur : le jeton
     * contenant le point d'insertion s'étend.
     */
    @Test
    void typingInsideIdentifierKeepsItsKind() {
        String published = "int compteur = 1;";
        LexDocument lex = lexicon(published);
        TokenCatchUp rattrapage = TokenCatchUp.of(lex,
                List.of(new TokenCatchUp.PendingEdit(9, 0, 1, 0, false)),
                List.of(new EditSite(0, 0, 0)),
                Document.of("int compteurs = 1;"), null);
        var panier = rattrapage.basketFor(0);
        assertNotNull(panier, "la ligne touchée reçoit un panier recalé");
        boolean identifiantEtendu = false;
        for (var token : panier.tokens()) {
            if (token.kind() == TokenKind.IDENTIFIER && token.end() > 12) {
                identifiantEtendu = true;
            }
        }
        assertEquals(true, identifiantEtendu,
                "l'identificateur s'étend sur la frappe, même sorte");
    }

    /**
     * La propriété centrale : sur des éditions aléatoires d'une ligne,
     * le préfixe NON édité garde ses plages à l'identique et la ligne
     * reste couverte exactement — jamais d'encre de base en attendant
     * la publication.
     */
    @Test
    void randomEditsKeepUntouchedPrefixExactly() {
        String lignePubliee = "int valeurCalculee = retour.obtenir() + 12;";
        Random alea = new Random(20261108L);
        for (int essai = 0; essai < 50; essai++) {
            int point = 4 + alea.nextInt(lignePubliee.length() - 4);
            int retire = alea.nextInt(3);
            int insere = alea.nextInt(3);
            String editee = lignePubliee.substring(0, point)
                    + "xy".substring(0, insere)
                    + lignePubliee.substring(Math.min(point + retire,
                            lignePubliee.length()));
            LexDocument lex = lexicon(lignePubliee);
            TokenCatchUp rattrapage = TokenCatchUp.of(lex,
                    List.of(new TokenCatchUp.PendingEdit(point, retire,
                            insere, 0, false)),
                    List.of(new EditSite(0, 0, 0)),
                    Document.of(editee), null);
            var panier = rattrapage.basketFor(0);
            assertNotNull(panier,
                    "essai " + essai + " : la ligne garde un panier");
            verifierPrefixeIntact(lex, panier, point, essai);
            verifierCouvertureExacte(panier, editee, essai);
        }
    }

    /**
     * Le préfixe avant le point d'édition : plages identiques au
     * publié, sorte pour sorte, jusqu'au point d'édition.
     */
    private static void verifierPrefixeIntact(LexDocument lex,
            LexedLine panier, int point, int essai) {
        var published = lex.line(0);
        for (var jetonPublie : published.tokens()) {
            if (jetonPublie.end() > point) {
                break;
            }
            boolean retrouve = false;
            for (var token : panier.tokens()) {
                if (token.start() == jetonPublie.start()
                        && token.end() == jetonPublie.end()
                        && token.kind() == jetonPublie.kind()) {
                    retrouve = true;
                    break;
                }
            }
            assertEquals(true, retrouve, "essai " + essai
                    + " : le jeton publié [" + jetonPublie.start() + ", "
                    + jetonPublie.end() + ") " + jetonPublie.kind()
                    + " survit au recalage");
        }
    }

    /** La couverture : la ligne reste couverte par les jetons soudés. */
    private static void verifierCouvertureExacte(LexedLine panier,
            String editee, int essai) {
        int coverage = 0;
        for (var token : panier.tokens()) {
            assertEquals(token.start(), coverage,
                    "essai " + essai + " : les jetons restent soudés");
            coverage = token.end();
        }
        assertEquals(editee.length(), coverage,
                "essai " + essai + " : la ligne reste couverte exactement");
    }

    /**
     * L'ouverture d'un guillemet : la structure ne change pas d'avis —
     * le recalage tient tant que la ligne reste une ligne ; la chaîne
     * ouverte colore la fin de ligne.
     */
    @Test
    void openingQuoteKeepsLineCovered() {
        String published = "int x = 1;";
        LexDocument lex = lexicon(published);
        TokenCatchUp rattrapage = TokenCatchUp.of(lex,
                List.of(new TokenCatchUp.PendingEdit(7, 0, 1, 0, false)),
                List.of(new EditSite(0, 0, 0)),
                Document.of("int x = \"1;"), null);
        var panier = rattrapage.basketFor(0);
        assertNotNull(panier);
        int coverage = 0;
        for (var token : panier.tokens()) {
            assertEquals(coverage, token.start(),
                    "les jetons restent soudés après l'ouverture du guillemet");
            coverage = token.end();
        }
        assertEquals("int x = \"1;".length(), coverage,
                "la ligne reste couverte exactement");
    }

    /**
     * Effacer toute la ligne : le panier recalé se vide proprement —
     * la couverture d'une ligne vide est vide, sans trou ni reste.
     */
    @Test
    void erasingWholeLineEmptiesTheBasketCleanly() {
        String published = "int x = 1;\nint y = 2;";
        LexDocument lex = lexicon(published);
        TokenCatchUp rattrapage = TokenCatchUp.of(lex,
                List.of(new TokenCatchUp.PendingEdit(0, 10, 0, 0, false)),
                List.of(new EditSite(0, 0, 0)),
                Document.of("\nint y = 2;"), null);
        var panier = rattrapage.basketFor(0);
        assertNotNull(panier);
        assertEquals(0, panier.tokens().length,
                "la ligne vidée ne porte plus aucun jeton");
    }

    /**
     * Le changement de structure (saut de ligne inséré) passe par la
     * relecture : la ligne décalée garde un panier — jamais vide.
     */
    @Test
    void structuralChangeFallsBackToRelex() {
        String published = "int a = 1;\nint b = 2;";
        LexDocument lex = lexicon(published);
        TokenCatchUp rattrapage = TokenCatchUp.of(lex,
                List.of(new TokenCatchUp.PendingEdit(0, 0, 1, 0, true)),
                List.of(new EditSite(0, 0, 1)),
                Document.of("\nint a = 1;\nint b = 2;"),
                JavaSyntax.INSTANCE.lexer());
        assertNotNull(rattrapage.basketFor(0), "la ligne vide relue existe");
        var decalee = rattrapage.basketFor(1);
        assertNotNull(decalee, "la ligne décalée est relue");
        assertEquals(TokenKind.KEYWORD, decalee.tokens()[0].kind(),
                "la relecture reconnait le mot réservé décalé");
    }
}
