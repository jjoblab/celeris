package jo.celeris.core.intelligence.internal.scan;

import java.util.List;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * Le relevé des identifiants d'un document, jeton par jeton : les noms
 * du code — identifiants, types, constantes — jamais les mots réservés,
 * jamais les chaînes ni les commentaires. L'index des usages (B-11) y
 * puise ses occurrences : ce que le lexique ne dit pas n'existe pas.
 */
public final class IdentifierScan  {

    /** Un identifiant relevé : son nom et son offset absolu. */
    public record NomOffset(String nom, int offset) {
    }

    private IdentifierScan() {
    }

    /**
     * Relève les identifiants du document.
     *
     * @param doc le document porteur
     * @param lex le lexique de la même analyse
     * @return les noms dans l'ordre du source
     */
    public static List<NomOffset> scanner(Document doc, LexDocument lex) {
        List<NomOffset> releves = new java.util.ArrayList<>();
        for (int line = 0; line < lex.lineCount(); line++) {
            LexedLine panier = lex.line(line);
            int base = doc.lineStart(line);
            for (Token token : panier.tokens()) {
                relever(doc, base, token, releves);
            }
        }
        return List.copyOf(releves);
    }

    private static void relever(Document doc, int base, Token token,
            List<NomOffset> releves) {
        String text = doc.subText(base + token.start(), base + token.end()).trim();
        if (text.isEmpty()) {
            return;
        }
        if (estNom(token.kind())) {
            releves.add(new NomOffset(text, base + token.start()
                    + espacesDeTete(doc, base + token.start(), base + token.end())));
        } else if (token.kind() == TokenKind.OPEN_BRACKET && text.startsWith("<")) {
            // balise d'ouverture du balisage : le nom vit après « < »
            String nom = text.substring(1);
            if (!nom.isEmpty()) {
                releves.add(new NomOffset(nom, base + token.start()
                        + espacesDeTete(doc, base + token.start(), base + token.end()) + 1));
            }
        } else if (token.kind() == TokenKind.CLOSE_BRACKET && text.startsWith("</")) {
            // balise de fermeture du balisage : le nom vit après « </ », le
            // chevron fermant fait partie du jeton et s'ôte
            String nom = text.substring(2);
            if (nom.endsWith(">")) {
                nom = nom.substring(0, nom.length() - 1);
            }
            if (!nom.isEmpty()) {
                releves.add(new NomOffset(nom, base + token.start()
                        + espacesDeTete(doc, base + token.start(), base + token.end()) + 2));
            }
        }
    }

    /** Les espaces de tête du jeton : le relevé pointe le premier caractère du nom. */
    private static int espacesDeTete(Document doc, int start, int end) {
        int espaces = 0;
        while (start + espaces < end && Character.isWhitespace(doc.charAt(start + espaces))) {
            espaces++;
        }
        return espaces;
    }

    private static boolean estNom(TokenKind sorte) {
        return sorte == TokenKind.IDENTIFIER || sorte == TokenKind.TYPE
                || sorte == TokenKind.CONSTANT;
    }
}
