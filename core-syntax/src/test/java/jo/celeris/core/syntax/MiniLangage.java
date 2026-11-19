package jo.celeris.core.syntax;

import java.util.List;

/**
 * Langage d'essai du socle (outillage de test, ADR-004) : mots, opérateurs,
 * crochets ASCII, chaînes simples sur une ligne et commentaires de bloc
 * «&#x2f;* … *&#x2f;» pouvant couvrir plusieurs lignes. Le but n'est pas
 * d'imiter un vrai langage mais d'exercer chaque promesse du moteur — états
 * transportés par ligne, cascade de commentaires (PIEGE-19), profondeur de
 * crochets — avec un lexique minuscule et lisible.
 *
 * <p>Grammaire des déclarations : {@code fn nom { … }} (fonction),
 * {@code var nom ;} (champ), tout le reste forme des instructions libres.
 * Le prédicat de découpe ouvre un chunk sur {@code fn} et {@code var} à
 * profondeur nulle.</p>
 */
final class MiniLangage {

    /** États transportés d'une ligne à la suivante. */
    enum State implements LexState {
        /** Hors commentaire de bloc. */
        NORMAL,
        /** Dans un commentaire de bloc ouvert. */
        COMMENTAIRE
    }

    /** Sortes de nœuds du langage d'essai. */
    enum Sorte implements NodeKind {
        /** Déclaration de fonction. */
        FONCTION,
        /** Déclaration de champ. */
        CHAMP,
        /** Bloc d'accolades. */
        BLOC,
        /** Instruction ou trivia. */
        AUTRE,
        /** Structure malformée. */
        ERREUR
    }

    private MiniLangage() {
    }

    /** Le langage d'essai complet, prêt pour un service. */
    static LanguageSyntax langage() {
        return new LanguageSyntax() {
            @Override
            public String id() {
                return "mini";
            }

            @Override
            public LanguageLexer lexer() {
                return new MiniLexeur();
            }

            @Override
            public LanguageParser parser() {
                return new MiniParseur();
            }
        };
    }

    /** Lexeur du langage d'essai : commentaires de bloc multi-lignes. */
    static final class MiniLexeur implements LanguageLexer {

        @Override
        public LexState initialState() {
            return State.NORMAL;
        }

        @Override
        public LexedLine lex(CharSequence text, LexState input) {
            List<Token> tokens = new java.util.ArrayList<>();
            State etat = input == State.COMMENTAIRE ? State.COMMENTAIRE : State.NORMAL;
            int n = text.length();
            int i = 0;
            while (i < n) {
                char c = text.charAt(i);
                if (etat == State.COMMENTAIRE) {
                    int fermeture = find(text, "*/", i);
                    int end = fermeture < 0 ? n : fermeture + 2;
                    tokens.add(new Token(TokenKind.COMMENT, i, end));
                    i = end;
                    if (fermeture >= 0) {
                        etat = State.NORMAL;
                    }
                } else if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                    etat = State.COMMENTAIRE;
                    int resultat = lexCommentaire(text, tokens, i);
                    i = Math.abs(resultat);
                    if (resultat > 0) {
                        etat = State.NORMAL;
                    }
                } else {
                    i = lexSimple(text, tokens, i);
                }
            }
            return new LexedLine(tokens.toArray(new Token[0]), etat);
        }

        private static int lexCommentaire(CharSequence text, List<Token> tokens, int start) {
            int fermeture = find(text, "*/", start + 2);
            int end = fermeture < 0 ? text.length() : fermeture + 2;
            tokens.add(new Token(TokenKind.COMMENT, start, end));
            return fermeture < 0 ? -end : end;
        }

        private static int lexSimple(CharSequence text, List<Token> tokens, int i) {
            char c = text.charAt(i);
            int n = text.length();
            if (c == ' ' || c == '\t') {
                return i + 1;
            }
            if (c == '"') {
                return lexChaine(text, tokens, i, n);
            }
            if (estOuvrant(c)) {
                tokens.add(new Token(TokenKind.OPEN_BRACKET, i, i + 1));
                return i + 1;
            }
            if (estFermant(c)) {
                tokens.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 1));
                return i + 1;
            }
            return lexMot(text, tokens, i, n);
        }

        private static int lexChaine(CharSequence text, List<Token> tokens, int i, int n) {
            int end = i + 1;
            while (end < n && text.charAt(end) != '"') {
                end++;
            }
            tokens.add(new Token(TokenKind.STRING, i, Math.min(end + 1, n)));
            return Math.min(end + 1, n);
        }

        private static int lexMot(CharSequence text, List<Token> tokens, int i, int n) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) {
                tokens.add(new Token(TokenKind.OPERATOR, i, i + 1));
                return i + 1;
            }
            int start = i;
            while (i < n && Character.isLetter(text.charAt(i))) {
                i++;
            }
            tokens.add(new Token(TokenKind.IDENTIFIER, start, i));
            return i;
        }

        private static boolean estOuvrant(char c) {
            return c == '(' || c == '{' || c == '[';
        }

        private static boolean estFermant(char c) {
            return c == ')' || c == '}' || c == ']';
        }

        private static int find(CharSequence text, String pattern, int depuis) {
            for (int i = depuis; i + pattern.length() <= text.length(); i++) {
                boolean egale = true;
                for (int j = 0; j < pattern.length(); j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        egale = false;
                        break;
                    }
                }
                if (egale) {
                    return i;
                }
            }
            return -1;
        }
    }

    /** Parseur du langage d'essai : découpe sur fn/var, blocs tolérants. */
    static final class MiniParseur implements LanguageParser {

        @Override
        public int chunkLevel() {
            return 0;
        }

        @Override
        public boolean startsChunk(TokenSlice slice, int index, int depth) {
            if (depth != 0) {
                return false;
            }
            if (slice.kind(index) != TokenKind.IDENTIFIER) {
                return false;
            }
            String text = slice.text(index);
            if (!"fn".equals(text) && !"var".equals(text)) {
                return false;
            }
            if (index == 0) {
                return true;
            }
            TokenKind precedent = slice.kind(index - 1);
            return precedent == TokenKind.CLOSE_BRACKET
                    || precedent == TokenKind.OPERATOR;
        }

        @Override
        public SynNode parseChunk(TokenSlice slice) {
            if (slice.size() == 0) {
                return SynNode.of(Sorte.AUTRE, NodeCategory.OTHER, 0, 0,
                        new SynNode[0], null, false);
            }
            String premier = slice.text(0);
            if ("fn".equals(premier) && slice.size() > 1) {
                return parseFonction(slice);
            }
            if ("var".equals(premier) && slice.size() > 1) {
                return SynNode.of(Sorte.CHAMP, NodeCategory.FIELD, 0, slice.end(slice.size() - 1),
                        new SynNode[0], slice.text(1), false);
            }
            return parseCorps(slice, 0, slice.end(slice.size() - 1));
        }

        private SynNode parseFonction(TokenSlice slice) {
            String nom = slice.size() > 1 ? slice.text(1) : null;
            SynNode body = parseCorps(slice, 2, slice.end(slice.size() - 1));
            List<SynNode> enfantsCorps = body.children();
            int end = enfantsCorps.isEmpty()
                    ? slice.end(Math.min(1, slice.size() - 1))
                    : enfantsCorps.get(enfantsCorps.size() - 1).end();
            return SynNode.of(Sorte.FONCTION, NodeCategory.FUNCTION, 0,
                    end, new SynNode[] {body}, nom, body.isError());
        }

        private SynNode parseCorps(TokenSlice slice, int depuis, int end) {
            List<SynNode> enfants = new java.util.ArrayList<>();
            boolean erreur = false;
            int i = depuis;
            while (i < slice.size()) {
                if (slice.kind(i) == TokenKind.OPEN_BRACKET && "{".equals(slice.text(i))) {
                    int fermeture = slice.matchOf(i);
                    if (fermeture < 0) {
                        erreur = true;
                        enfants.add(SynNode.of(Sorte.ERREUR, NodeCategory.ERROR,
                                slice.start(i), end, new SynNode[0], null, true));
                        i = slice.size();
                        break;
                    }
                    enfants.add(SynNode.of(Sorte.BLOC, NodeCategory.BLOCK,
                            slice.start(i), slice.end(fermeture),
                            corpsEnfants(slice, i + 1, fermeture), null, false));
                    i = fermeture + 1;
                } else {
                    i++;
                }
            }
            return SynNode.of(erreur ? Sorte.ERREUR : Sorte.AUTRE,
                    erreur ? NodeCategory.ERROR : NodeCategory.OTHER,
                    depuis < slice.size() ? slice.start(depuis) : 0, end,
                    enfants.toArray(new SynNode[0]), null, erreur);
        }

        private SynNode[] corpsEnfants(TokenSlice slice, int depuis, int end) {
            List<SynNode> imbriques = new java.util.ArrayList<>();
            int i = depuis;
            while (i < end) {
                if (slice.kind(i) == TokenKind.OPEN_BRACKET && "{".equals(slice.text(i))) {
                    int fermeture = slice.matchOf(i);
                    if (fermeture < 0 || fermeture >= end) {
                        break;
                    }
                    imbriques.add(SynNode.of(Sorte.BLOC, NodeCategory.BLOCK,
                            slice.start(i), slice.end(fermeture),
                            corpsEnfants(slice, i + 1, fermeture), null, false));
                    i = fermeture + 1;
                } else {
                    i++;
                }
            }
            return imbriques.toArray(new SynNode[0]);
        }
    }
}
