package jo.celeris.sample.lsp;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;

/**
 * Les diagnostics Java du mini serveur : un balayage réel du texte,
 * en ignorant chaînes et commentaires — les accolades non fermées et
 * les fermantes orphelines, les parenthèses et crochets
 * désappariés, le point-virgule manquant d'une déclaration de
 * variable ou d'instruction qui finit sa ligne. Le même esprit que
 * le mini Java du dépôt de départ, à la mesure de l'exemple.
 */
final class DiagnosticsJava {

    private DiagnosticsJava() {
    }

    /** Les diagnostics du texte Java. */
    static List<DiagnosticLsp> de(String texte) {
        List<DiagnosticLsp> diagnostics = new ArrayList<>();
        appariement(texte, diagnostics);
        pointsVirgules(texte, diagnostics);
        return diagnostics;
    }

    /** L'appariement des accolades, parenthèses et crochets — les
     *  ouvrantes orphelines rendent leur ligne, chaque fermante
     *  solitaire aussi. */
    private static void appariement(String texte,
            List<DiagnosticLsp> diagnostics) {
        BalayageJava balayage = new BalayageJava();
        java.util.ArrayDeque<int[]> pile = balayage.balayer(texte, diagnostics);
        noterOuvrantesOrphelines(diagnostics, pile);
    }

    /** Le balayage d'un texte Java : chaînes et commentaires
     *  écartés, ouvrantes empilées, fermantes solitaires notées. */
    private static final class BalayageJava {

        /** Les ouvrantes encore ouvertes : caractère, ligne, colonne. */
        private final java.util.ArrayDeque<int[]> pile =
                new java.util.ArrayDeque<>();

        /** Balaye le texte : la pile des ouvrantes vivantes en sort,
         *  les fermantes solitaires notées au passage. */
        /** L'état du balayage : chaîne, commentaires, position. */
        private boolean dansChaine;
        private char quote;
        private boolean ligneCommentaire;
        private boolean blocCommentaire;
        private int ligne;
        private int colonne;

        java.util.ArrayDeque<int[]> balayer(String texte,
                List<DiagnosticLsp> diagnostics) {
            for (int i = 0; i < texte.length(); i++) {
                char c = texte.charAt(i);
                char suivant = i + 1 < texte.length()
                        ? texte.charAt(i + 1) : 0;
                if (c == '\n') {
                    ligne++;
                    colonne = 0;
                    ligneCommentaire = false;
                    continue;
                }
                colonne++;
                if (horsCode(c, suivant, i) >= 0) {
                    continue;
                }
                if (c == '{' || c == '(' || c == '[') {
                    pile.push(new int[]{c, ligne, colonne});
                } else if (c == '}' || c == ')' || c == ']') {
                    noterFermante(diagnostics, pile, c, ligne, colonne);
                }
            }
            return pile;
        }

        /** Le traitement du HORS-CODE : commentaires ouverts
         *  (retour 0, ou 1 sur le glyphe double), chaîne (0, ou 1 sur
         *  l'échappée), chaîne qui s'ouvre (0), -1 quand le caractère
         *  vit dans le code. */
        private int horsCode(char c, char suivant, int position) {
            if (ligneCommentaire) {
                return 0;
            }
            if (blocCommentaire) {
                if (c == '*' && suivant == '/') {
                    blocCommentaire = false;
                    return 1;
                }
                return 0;
            }
            if (dansChaine) {
                if (c == '\\') {
                    return 1;
                }
                if (c == quote) {
                    dansChaine = false;
                }
                return 0;
            }
            if (c == '/' && suivant == '/') {
                ligneCommentaire = true;
                return 0;
            }
            if (c == '/' && suivant == '*') {
                blocCommentaire = true;
                return 1;
            }
            if (c == '"' || c == '\'') {
                dansChaine = true;
                quote = c;
                return 0;
            }
            return -1;
        }
    }

    /** Les ouvrantes jamais fermées : une par ligne d'ouverture. */
    private static void noterOuvrantesOrphelines(List<DiagnosticLsp> diagnostics,
            java.util.ArrayDeque<int[]> pile) {
        java.util.Set<Integer> lignesDeja = new java.util.HashSet<>();
        for (int[] ouvrante : pile) {
            if (lignesDeja.add(ouvrante[1])) {
                diagnostics.add(new DiagnosticLsp(
                        new PlageLsp(
                                new PositionLsp(ouvrante[1], ouvrante[2] - 1),
                                new PositionLsp(ouvrante[1], ouvrante[2])),
                        DiagnosticLsp.ERREUR,
                        "'''" + (char) ouvrante[0] + "''' ouverte mais jamais fermée",
                        "minijava", "appariement"));
            }
        }
    }

    /** Note une fermante : appariée elle se dépile, solitaire elle
     *  rend son diagnostic. */
    private static void noterFermante(List<DiagnosticLsp> diagnostics,
            java.util.ArrayDeque<int[]> pile, char c, int ligne, int colonne) {
        char attendue = c == '}' ? '{' : c == ')' ? '(' : '[';
        if (pile.isEmpty() || pile.peek()[0] != attendue) {
            diagnostics.add(new DiagnosticLsp(
                    new PlageLsp(new PositionLsp(ligne, colonne - 1),
                            new PositionLsp(ligne, colonne)),
                    DiagnosticLsp.ERREUR,
                    "Fermante '''" + c + "''' sans ouvrante",
                    "minijava", "appariement"));
        } else {
            pile.pop();
        }
    }

    /** Le point-virgule manquant : une ligne qui finit en
     *  identifiant, littéral, fermante d'appel ou crochets — sans
     *  point-virgule, sans ouvrante de bloc, sans virgule ni
     *  deux-points, et qui n'est ni un commentaire ni vide — manque
     *  sa conclusion. Les lignes de déclaration de méthode ou de
     *  classe (qui finissent en « ) { » ou « { ») ne comptent pas :
     *  l'ouvrante d'appel finit en « ( » ? Le filtrage par dernier
     *  caractère significant suffit à la mesure de l'exemple. */
    private static void pointsVirgules(String texte,
            List<DiagnosticLsp> diagnostics) {
        String[] lignes = texte.split("\n", -1);
        for (int numero = 0; numero < lignes.length; numero++) {
            String ligne = lignes[numero];
            String code = ligne.trim();
            if (code.isEmpty() || code.startsWith("//")
                    || code.startsWith("*") || code.startsWith("/*")) {
                continue;
            }
            char dernier = code.charAt(code.length() - 1);
            if (";{},".indexOf(dernier) >= 0) {
                continue;
            }
            // les lignes de contrôle finissent en parenthèse ouverte
            // multiligne, les packages/imports ont leur point-virgule :
            // tout le reste, qui finit en lettre, chiffre, « ) » ou
            // crochet fermant, manque sa conclusion
            if (Character.isLetterOrDigit(dernier) || dernier == ')'
                    || dernier == ']' || dernier == '"'
                    || dernier == '.') {
                diagnostics.add(new DiagnosticLsp(
                        new PlageLsp(
                                new PositionLsp(numero, code.length() - 1),
                                new PositionLsp(numero, code.length())),
                        DiagnosticLsp.AVERTISSEMENT,
                        "Point-virgule manquant probable",
                        "minijava", "syntaxe"));
            }
        }
    }
}
