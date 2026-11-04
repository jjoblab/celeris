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
    enum Etat implements LexState {
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
            return Etat.NORMAL;
        }

        @Override
        public LexedLine lex(CharSequence texte, LexState entree) {
            List<Token> jetons = new java.util.ArrayList<>();
            Etat etat = entree == Etat.COMMENTAIRE ? Etat.COMMENTAIRE : Etat.NORMAL;
            int n = texte.length();
            int i = 0;
            while (i < n) {
                char c = texte.charAt(i);
                if (etat == Etat.COMMENTAIRE) {
                    int fermeture = trouver(texte, "*/", i);
                    int fin = fermeture < 0 ? n : fermeture + 2;
                    jetons.add(new Token(TokenKind.COMMENT, i, fin));
                    i = fin;
                    if (fermeture >= 0) {
                        etat = Etat.NORMAL;
                    }
                } else if (c == '/' && i + 1 < n && texte.charAt(i + 1) == '*') {
                    etat = Etat.COMMENTAIRE;
                    int resultat = lexCommentaire(texte, jetons, i);
                    i = Math.abs(resultat);
                    if (resultat > 0) {
                        etat = Etat.NORMAL;
                    }
                } else {
                    i = lexSimple(texte, jetons, i);
                }
            }
            return new LexedLine(jetons.toArray(new Token[0]), etat);
        }

        private static int lexCommentaire(CharSequence texte, List<Token> jetons, int debut) {
            int fermeture = trouver(texte, "*/", debut + 2);
            int fin = fermeture < 0 ? texte.length() : fermeture + 2;
            jetons.add(new Token(TokenKind.COMMENT, debut, fin));
            return fermeture < 0 ? -fin : fin;
        }

        private static int lexSimple(CharSequence texte, List<Token> jetons, int i) {
            char c = texte.charAt(i);
            int n = texte.length();
            if (c == ' ' || c == '\t') {
                return i + 1;
            }
            if (c == '"') {
                return lexChaine(texte, jetons, i, n);
            }
            if (estOuvrant(c)) {
                jetons.add(new Token(TokenKind.OPEN_BRACKET, i, i + 1));
                return i + 1;
            }
            if (estFermant(c)) {
                jetons.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 1));
                return i + 1;
            }
            return lexMot(texte, jetons, i, n);
        }

        private static int lexChaine(CharSequence texte, List<Token> jetons, int i, int n) {
            int fin = i + 1;
            while (fin < n && texte.charAt(fin) != '"') {
                fin++;
            }
            jetons.add(new Token(TokenKind.STRING, i, Math.min(fin + 1, n)));
            return Math.min(fin + 1, n);
        }

        private static int lexMot(CharSequence texte, List<Token> jetons, int i, int n) {
            char c = texte.charAt(i);
            if (!Character.isLetter(c)) {
                jetons.add(new Token(TokenKind.OPERATOR, i, i + 1));
                return i + 1;
            }
            int debut = i;
            while (i < n && Character.isLetter(texte.charAt(i))) {
                i++;
            }
            jetons.add(new Token(TokenKind.IDENTIFIER, debut, i));
            return i;
        }

        private static boolean estOuvrant(char c) {
            return c == '(' || c == '{' || c == '[';
        }

        private static boolean estFermant(char c) {
            return c == ')' || c == '}' || c == ']';
        }

        private static int trouver(CharSequence texte, String motif, int depuis) {
            for (int i = depuis; i + motif.length() <= texte.length(); i++) {
                boolean egale = true;
                for (int j = 0; j < motif.length(); j++) {
                    if (texte.charAt(i + j) != motif.charAt(j)) {
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
            String texte = slice.text(index);
            if (!"fn".equals(texte) && !"var".equals(texte)) {
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
            SynNode corps = parseCorps(slice, 2, slice.end(slice.size() - 1));
            List<SynNode> enfantsCorps = corps.children();
            int fin = enfantsCorps.isEmpty()
                    ? slice.end(Math.min(1, slice.size() - 1))
                    : enfantsCorps.get(enfantsCorps.size() - 1).end();
            return SynNode.of(Sorte.FONCTION, NodeCategory.FUNCTION, 0,
                    fin, new SynNode[] {corps}, nom, corps.isError());
        }

        private SynNode parseCorps(TokenSlice slice, int depuis, int fin) {
            List<SynNode> enfants = new java.util.ArrayList<>();
            boolean erreur = false;
            int i = depuis;
            while (i < slice.size()) {
                if (slice.kind(i) == TokenKind.OPEN_BRACKET && "{".equals(slice.text(i))) {
                    int fermeture = slice.matchOf(i);
                    if (fermeture < 0) {
                        erreur = true;
                        enfants.add(SynNode.of(Sorte.ERREUR, NodeCategory.ERROR,
                                slice.start(i), fin, new SynNode[0], null, true));
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
                    depuis < slice.size() ? slice.start(depuis) : 0, fin,
                    enfants.toArray(new SynNode[0]), null, erreur);
        }

        private SynNode[] corpsEnfants(TokenSlice slice, int depuis, int fin) {
            List<SynNode> imbriques = new java.util.ArrayList<>();
            int i = depuis;
            while (i < fin) {
                if (slice.kind(i) == TokenKind.OPEN_BRACKET && "{".equals(slice.text(i))) {
                    int fermeture = slice.matchOf(i);
                    if (fermeture < 0 || fermeture >= fin) {
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
