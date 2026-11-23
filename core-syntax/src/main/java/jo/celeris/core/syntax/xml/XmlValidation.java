package jo.celeris.core.syntax.xml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * Validation XML (C-03) : les imbrications invalides rapportées avec
 * leur plage exacte. La fermeture étrangère — aucun ancêtre de ce nom
 * dans la pile — porte sa propre plage et nomme l'attendu comme le
 * trouvé ; la fermeture qui referme un ancêtre signale chaque niveau
 * traversé à la plage de son ouverture ; l'élément resté ouvert à la
 * fin porte la plage de sa balise d'ouverture.
 *
 * <p>Le lexique XML existe déjà (ADR-004) — commentaires, sections
 * CDATA, prologues et textes traversent les lignes par l'état
 * transporté : la validation relit le document ligne à ligne par le
 * même lexeur, aucune logique d'évitement ne se duplique. Jamais
 * d'exception — la malformation est une donnée.</p>
 */
public final class XmlValidation {

    private XmlValidation() {
    }

    /**
     * Les imbrications invalides du document, dans l'ordre du source.
     *
     * @param doc le document XML à valider
     * @return les problèmes localisés (peut être vide, jamais null)
     */
    public static List<SyntaxIssue> validate(Document doc) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        Deque<Ouverture> pile = new ArrayDeque<>();
        XmlLexeur lexeur = new XmlLexeur();
        LexState etat = lexeur.initialState();
        for (int line = 0; line < doc.lineCount(); line++) {
            String text = doc.lineText(line);
            LexedLine lexe = lexeur.lex(text, etat);
            LexState sortie = lexe.exitState();
            // Une ligne LUE porte son état (contrat du lexeur) : la
            // traduction ne sert que NullAway.
            etat = sortie == null ? etat : sortie;
            for (Token token : lexe.tokens()) {
                traiter(token, text, doc.lineStart(line), pile, problemes);
            }
        }
        while (!pile.isEmpty()) {
            Ouverture ouverte = pile.pop();
            problemes.add(new SyntaxIssue(ouverte.start(),
                    ouverte.end() - ouverte.start(),
                    "unclosed element: <" + ouverte.nom() + ">"));
        }
        return List.copyOf(problemes);
    }

    /** Traite un jeton : seuls les crochets décident, le reste traverse. */
    private static void traiter(Token token, String line, int base,
            Deque<Ouverture> pile, List<SyntaxIssue> problemes) {
        if (token.kind() != TokenKind.OPEN_BRACKET
                && token.kind() != TokenKind.CLOSE_BRACKET) {
            return;
        }
        String text = line.substring(token.start(), token.end());
        if (token.kind() == TokenKind.OPEN_BRACKET) {
            if (!text.startsWith("<![CDATA[")) {
                pile.push(new Ouverture(text.substring(1),
                        base + token.start(), base + token.end()));
            }
            return;
        }
        if (text.equals("/>")) {
            if (!pile.isEmpty()) {
                pile.pop();
            }
            return;
        }
        if (text.equals("]]>") || !text.startsWith("</")) {
            return;
        }
        fermeture(nomDe(text), base + token.start(), base + token.end(), pile, problemes);
    }

    /** Le nom refermé par une fermeture nommée, «&#x2f;» et «&#x3e;» déduits. */
    private static String nomDe(String text) {
        int end = text.endsWith(">") ? text.length() - 1 : text.length();
        return text.substring(2, end);
    }

    /** La fermeture nommée : assortie referme, ancêtre traversé signale. */
    private static void fermeture(String nom, int start, int end,
            Deque<Ouverture> pile, List<SyntaxIssue> problemes) {
        if (pile.isEmpty()) {
            problemes.add(new SyntaxIssue(start, end - start,
                    "closing tag without opening: </" + nom + ">"));
            return;
        }
        if (pile.peek().nom().equals(nom)) {
            pile.pop();
            return;
        }
        if (!porteNom(pile, nom)) {
            problemes.add(new SyntaxIssue(start, end - start,
                    "mismatched closing tag: expected </" + pile.peek().nom()
                            + ">, found </" + nom + ">"));
            pile.clear();
            return;
        }
        while (!pile.peek().nom().equals(nom)) {
            Ouverture ouverte = pile.pop();
            problemes.add(new SyntaxIssue(ouverte.start(),
                    ouverte.end() - ouverte.start(),
                    "unclosed element: <" + ouverte.nom() + ">"));
        }
        pile.pop();
    }

    /** Vrai quand un ancêtre de la pile porte ce nom. */
    private static boolean porteNom(Deque<Ouverture> pile, String nom) {
        for (Ouverture ouverture : pile) {
            if (ouverture.nom().equals(nom)) {
                return true;
            }
        }
        return false;
    }

    /** Une ouverture en attente de sa fermeture : nom et plage source. */
    private record Ouverture(String nom, int start, int end) {
    }
}
