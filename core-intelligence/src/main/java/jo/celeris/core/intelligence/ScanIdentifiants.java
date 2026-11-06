package jo.celeris.core.intelligence;

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
final class ScanIdentifiants {

    /** Un identifiant relevé : son nom et son offset absolu. */
    record NomOffset(String nom, int offset) {
    }

    private ScanIdentifiants() {
    }

    /**
     * Relève les identifiants du document.
     *
     * @param doc le document porteur
     * @param lex le lexique de la même analyse
     * @return les noms dans l'ordre du source
     */
    static List<NomOffset> scanner(Document doc, LexDocument lex) {
        List<NomOffset> releves = new java.util.ArrayList<>();
        for (int ligne = 0; ligne < lex.lineCount(); ligne++) {
            LexedLine panier = lex.line(ligne);
            int base = doc.lineStart(ligne);
            for (Token jeton : panier.tokens()) {
                relever(doc, base, jeton, releves);
            }
        }
        return List.copyOf(releves);
    }

    private static void relever(Document doc, int base, Token jeton,
            List<NomOffset> releves) {
        String texte = doc.subText(base + jeton.start(), base + jeton.end()).trim();
        if (texte.isEmpty()) {
            return;
        }
        if (estNom(jeton.kind())) {
            releves.add(new NomOffset(texte, base + jeton.start()
                    + espacesDeTete(doc, base + jeton.start(), base + jeton.end())));
        } else if (jeton.kind() == TokenKind.OPEN_BRACKET && texte.startsWith("<")) {
            // balise d'ouverture du balisage : le nom vit après « < »
            String nom = texte.substring(1);
            if (!nom.isEmpty()) {
                releves.add(new NomOffset(nom, base + jeton.start()
                        + espacesDeTete(doc, base + jeton.start(), base + jeton.end()) + 1));
            }
        } else if (jeton.kind() == TokenKind.CLOSE_BRACKET && texte.startsWith("</")) {
            // balise de fermeture du balisage : le nom vit après « </ », le
            // chevron fermant fait partie du jeton et s'ôte
            String nom = texte.substring(2);
            if (nom.endsWith(">")) {
                nom = nom.substring(0, nom.length() - 1);
            }
            if (!nom.isEmpty()) {
                releves.add(new NomOffset(nom, base + jeton.start()
                        + espacesDeTete(doc, base + jeton.start(), base + jeton.end()) + 2));
            }
        }
    }

    /** Les espaces de tête du jeton : le relevé pointe le premier caractère du nom. */
    private static int espacesDeTete(Document doc, int debut, int fin) {
        int espaces = 0;
        while (debut + espaces < fin && Character.isWhitespace(doc.charAt(debut + espaces))) {
            espaces++;
        }
        return espaces;
    }

    private static boolean estNom(TokenKind sorte) {
        return sorte == TokenKind.IDENTIFIER || sorte == TokenKind.TYPE
                || sorte == TokenKind.CONSTANT;
    }
}
