package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jo.celeris.core.text.Document;

/**
 * La matérialisation Java de l'arbre natif aplati (ADR-020) : l'arbre
 * entier se lit UNE fois — les feuilles y deviennent les jetons du
 * lexique (rognés à leur ligne, les blancs soudés au jeton qui les
 * suit, la convention du socle), les nœuds de premier niveau qui
 * traversent les lignes y deviennent les chunks du pli. Tout vit en
 * Java après la passe : l'arbre natif ne survit qu'entre deux éditions
 * de la voie bornée (jamais de natif dans le chemin de rendu — le
 * jalon de l'ADR-004).
 *
 * <p>Les octets de la grammaire s'y traduisent en caractères du modèle
 * (le décodage UTF-8 dresse la table), les colonnes d'octets en
 * colonnes de caractères — la ligne d'un point ne bouge jamais (les
 * sauts comptent pareil des deux côtés).</p>
 */
final class MaterialisationNatif {

    /** Les mots réservés des grammaires servies — la teinte clé. */
    private static final Map<String, TokenKind> SORTES = Map.ofEntries(
            Map.entry("true", TokenKind.KEYWORD),
            Map.entry("false", TokenKind.KEYWORD),
            Map.entry("null", TokenKind.KEYWORD),
            Map.entry("None", TokenKind.KEYWORD),
            Map.entry("True", TokenKind.KEYWORD),
            Map.entry("False", TokenKind.KEYWORD),
            Map.entry("if", TokenKind.KEYWORD),
            Map.entry("elif", TokenKind.KEYWORD),
            Map.entry("else", TokenKind.KEYWORD),
            Map.entry("for", TokenKind.KEYWORD),
            Map.entry("while", TokenKind.KEYWORD),
            Map.entry("return", TokenKind.KEYWORD),
            Map.entry("yield", TokenKind.KEYWORD),
            Map.entry("class", TokenKind.KEYWORD),
            Map.entry("def", TokenKind.KEYWORD),
            Map.entry("lambda", TokenKind.KEYWORD),
            Map.entry("import", TokenKind.KEYWORD),
            Map.entry("from", TokenKind.KEYWORD),
            Map.entry("as", TokenKind.KEYWORD),
            Map.entry("with", TokenKind.KEYWORD),
            Map.entry("try", TokenKind.KEYWORD),
            Map.entry("except", TokenKind.KEYWORD),
            Map.entry("finally", TokenKind.KEYWORD),
            Map.entry("raise", TokenKind.KEYWORD),
            Map.entry("assert", TokenKind.KEYWORD),
            Map.entry("pass", TokenKind.KEYWORD),
            Map.entry("del", TokenKind.KEYWORD),
            Map.entry("global", TokenKind.KEYWORD),
            Map.entry("nonlocal", TokenKind.KEYWORD),
            Map.entry("and", TokenKind.KEYWORD),
            Map.entry("or", TokenKind.KEYWORD),
            Map.entry("not", TokenKind.KEYWORD),
            Map.entry("in", TokenKind.KEYWORD),
            Map.entry("is", TokenKind.KEYWORD),
            Map.entry("const", TokenKind.KEYWORD),
            Map.entry("let", TokenKind.KEYWORD),
            Map.entry("var", TokenKind.KEYWORD),
            Map.entry("function", TokenKind.KEYWORD),
            Map.entry("new", TokenKind.KEYWORD),
            Map.entry("this", TokenKind.KEYWORD),
            Map.entry("super", TokenKind.KEYWORD),
            Map.entry("throw", TokenKind.KEYWORD),
            Map.entry("async", TokenKind.KEYWORD),
            Map.entry("await", TokenKind.KEYWORD),
            Map.entry("switch", TokenKind.KEYWORD),
            Map.entry("case", TokenKind.KEYWORD),
            Map.entry("default", TokenKind.KEYWORD),
            Map.entry("break", TokenKind.KEYWORD),
            Map.entry("continue", TokenKind.KEYWORD),
            Map.entry("typeof", TokenKind.KEYWORD),
            Map.entry("instanceof", TokenKind.KEYWORD),
            Map.entry("delete", TokenKind.KEYWORD),
            Map.entry("void", TokenKind.OPERATOR));

    /** Le jeton d'une feuille : sa colonne (caractères) et sa longueur. */
    private record LeafToken(TokenKind sorte, int debut, int fin) {
    }

    private final Document document;
    /** La table octet → caractère (l'indice des octets décodés). */
    private final int[] charParOctet;
    /** Le début (caractères) de chaque ligne du document. */
    private final int[] debutLigne;

    private MaterialisationNatif(Document document) {
        this.document = document;
        this.charParOctet = caracteresParOctet(document.text());
        this.debutLigne = debutsDeLignes();
    }

    /**
     * L'analyse matérialisée d'un arbre aplati : le lexique du document
     * (jetons par ligne, profondeurs de crochets) et l'arbre des chunks
     * (les nœuds de premier niveau qui traversent les lignes — le pli
     * exact du niveau 2).
     *
     * @param document   l'instantané analysé
     * @param aplatit    l'arbre aplati (six entiers par nœud, préordre)
     * @param symboles   la table des noms de symboles du langage
     * @param revision   la révision de l'instantané
     * @return l'analyse entière, versionnée
     */
    static Analysis de(Document document, int[] aplatit, String[] symboles,
            int revision) {
        MaterialisationNatif matiere = new MaterialisationNatif(document);
        return matiere.materiel(aplatit, symboles, revision);
    }

    /** La traduction octets → caractères du texte décodé. */
    private static int[] caracteresParOctet(String texte) {
        byte[] octets = texte.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] table = new int[octets.length + 1];
        int caractere = 0;
        for (int octet = 0; octet < octets.length; ) {
            table[octet] = caractere;
            int largeur = largeurUtf8(octets[octet]);
            octet += Math.max(1, largeur);
            caractere++;
        }
        table[octets.length] = caractere;
        return table;
    }

    /** La largeur d'un octet de tête UTF-8, 1 par défaut. */
    private static int largeurUtf8(byte octet) {
        int b = octet & 0xFF;
        if ((b & 0xE0) == 0xC0) {
            return 2;
        }
        if ((b & 0xF0) == 0xE0) {
            return 3;
        }
        if ((b & 0xF8) == 0xF0) {
            return 4;
        }
        return 1;
    }

    /** Les débuts (caractères) de chaque ligne du document. */
    private int[] debutsDeLignes() {
        int nombre = document.lineCount();
        int[] debuts = new int[nombre];
        for (int ligne = 0; ligne < nombre; ligne++) {
            debuts[ligne] = document.lineStart(ligne);
        }
        return debuts;
    }

    /** La ligne (indice) d'un caractère donné — recherche dichotomique. */
    private int ligneDe(int caractere) {
        int bas = 0;
        int haut = debutLigne.length - 1;
        while (bas < haut) {
            int milieu = (bas + haut + 1) >>> 1;
            if (debutLigne[milieu] <= caractere) {
                bas = milieu;
            } else {
                haut = milieu - 1;
            }
        }
        return bas;
    }

    /** La matière : le lexique et l'arbre des chunks. */
    private Analysis materiel(int[] aplatit, String[] symboles, int revision) {
        List<LeafToken> leaves = new ArrayList<>();
        List<int[]> blocks = new ArrayList<>();
        parcourir(aplatit, symboles, leaves, blocks);
        LexedLine[] lignes = lexique(leaves);
        int[] profondeurs = profondeurs(lignes);
        SyntaxTree arbre = arbre(blocks, revision);
        return new Analysis(revision,
                new LexDocument(lignes, profondeurs, EtatNatif.PREMIER), arbre);
    }

    /**
     * Le parcours préordre de l'arbre aplati : les FEUILLES se ramassent
     * en jetons, les ENFANTS DE PREMIER NIVEAU qui traversent les lignes
     * se ramassent en blocs — la pile des enfants restants tient la
     * profondeur (un nœud sans enfant n'y descend jamais).
     */
    private void parcourir(int[] aplatit, String[] symboles,
            List<LeafToken> leaves, List<int[]> blocks) {
        int[] restants = new int[64];
        int profondeur = 0;
        for (int i = 0; i < aplatit.length; i += 6) {
            int enfants = aplatit[i + 5];
            if (profondeur > 0) {
                restants[profondeur - 1]--;
            }
            int symbole = aplatit[i];
            int debut = charParOctet[aplatit[i + 1]];
            int fin = charParOctet[aplatit[i + 2]];
            if (profondeur == 1 && fin > debut) {
                blocks.add(new int[] {debut, fin, aplatit[i + 3]});
            }
            if (enfants == 0) {
                if (fin > debut) {
                    leaves.add(new LeafToken(sorteDe(symboles[symbole]),
                            debut, fin));
                }
            } else {
                if (profondeur >= restants.length) {
                    restants = java.util.Arrays.copyOf(restants,
                            restants.length * 2);
                }
                restants[profondeur] = enfants;
                profondeur++;
            }
            while (profondeur > 1 && restants[profondeur - 1] == 0) {
                profondeur--;
            }
        }
    }

    /** Le lexique du document : les jetons rognés à leur ligne, les
     *  feuilles CONTIGUËS de même sorte soudées en un jeton (les
     *  guillemets d'une chaîne, le contenu du commentaire — la
     *  grammaire les découpe, le lexique du socle les soude), les
     *  blancs APPARTIENNENT au jeton qui les suit — la convention du
     *  socle, sauf le blanc de TÊTE de ligne, qui n'est à personne. */
    private LexedLine[] lexique(List<LeafToken> leaves) {
        souder(leaves);
        int nombre = document.lineCount();
        LexedLine[] lignes = new LexedLine[nombre];
        List<Token> panier = new ArrayList<>();
        int index = 0;
        for (int ligne = 0; ligne < nombre; ligne++) {
            panier.clear();
            int debut = debutLigne[ligne];
            int fin = document.lineEnd(ligne);
            int curseur = -1;
            while (index < leaves.size() && leaves.get(index).debut() < fin) {
                LeafToken jeton = leaves.get(index);
                if (jeton.fin() <= debut) {
                    index++;
                    continue;
                }
                int finRogne = Math.min(jeton.fin(), fin);
                int debutRogne = curseur < 0
                        ? Math.max(jeton.debut(), debut) : curseur;
                if (finRogne > debutRogne) {
                    panier.add(new Token(jeton.sorte(),
                            debutRogne - debut, finRogne - debut));
                }
                if (jeton.fin() > fin) {
                    break;
                }
                index++;
                curseur = jeton.fin();
            }
            lignes[ligne] = new LexedLine(panier.toArray(new Token[0]),
                    EtatNatif.PREMIER);
        }
        return lignes;
    }

    /** Soude les feuilles contiguës de même sorte — la chaîne de la
     *  grammaire redevient le jeton du socle ; les crochets et la
     *  ponctuation ne se soudent JAMAIS (deux fermantes voisines
     *  restent deux jetons, l'appariement le exige). */
    private static void souder(List<LeafToken> leaves) {
        int ecrit = 0;
        for (LeafToken jeton : leaves) {
            if (ecrit > 0) {
                LeafToken precedent = leaves.get(ecrit - 1);
                if (souirable(precedent.sorte())
                        && precedent.sorte() == jeton.sorte()
                        && precedent.fin() == jeton.debut()) {
                    leaves.set(ecrit - 1, new LeafToken(precedent.sorte(),
                            precedent.debut(), jeton.fin()));
                    continue;
                }
            }
            leaves.set(ecrit, jeton);
            ecrit++;
        }
        if (ecrit < leaves.size()) {
            leaves.subList(ecrit, leaves.size()).clear();
        }
    }

    /** Vrai quand la sorte se soude : la chaîne et le commentaire —
     *  les morceaux d'une même construction multi-feuilles. */
    private static boolean souirable(TokenKind sorte) {
        return sorte == TokenKind.STRING || sorte == TokenKind.COMMENT;
    }

    /** Les profondeurs de crochets au début de chaque ligne. */
    private int[] profondeurs(LexedLine[] lignes) {
        int[] profondeurs = new int[lignes.length];
        int profondeur = 0;
        for (int ligne = 0; ligne < lignes.length; ligne++) {
            profondeurs[ligne] = profondeur;
            for (Token jeton : lignes[ligne].tokens()) {
                if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur++;
                } else if (jeton.kind() == TokenKind.CLOSE_BRACKET
                        && profondeur > 0) {
                    profondeur--;
                }
            }
        }
        return profondeurs;
    }

    /** L'arbre des chunks : chaque bloc de premier niveau porte un nœud
     *  BLOC couvrant son étendue — le pli exact du niveau 2. */
    private SyntaxTree arbre(List<int[]> blocks, int revision) {
        List<Chunk> chunks = new ArrayList<>();
        for (int[] bloc : blocks) {
            int ligneDebut = bloc[2];
            int ligneFin = ligneDe(bloc[1] - 1);
            if (ligneFin <= ligneDebut) {
                continue;
            }
            int debut = document.lineStart(ligneDebut);
            SynNode racine = SynNode.of(NativeNodeKind.NODE, NodeCategory.BLOCK,
                    0, bloc[1] - debut, new SynNode[0], null, false);
            chunks.add(new Chunk(ligneDebut, bloc[0] - debut, ligneFin,
                    finDeColonne(bloc[1], ligneFin), racine));
        }
        return new SyntaxTree(revision, List.copyOf(chunks));
    }

    /** La colonne (caractères) d'un début de ligne. */
    private int finDeColonne(int caractere, int ligne) {
        return caractere - document.lineStart(ligne);
    }

    /** Les opérateurs d'un seul caractère — la table qui les dit. */
    private static final String OPERATEURS = "+-*/%=<>!~&|^?";

    /** La sorte de jeton d'un symbole de grammaire. */
    private static TokenKind sorteDe(String nom) {
        if (nom.length() == 1) {
            return sorteDuCaractere(nom.charAt(0));
        }
        return sorteDuNom(nom);
    }

    /** La sorte d'un symbole d'un caractère : crochet, chaîne,
     *  opérateur de la table, ponctuation sinon. */
    private static TokenKind sorteDuCaractere(char caractere) {
        return switch (caractere) {
            case '{', '[', '(' -> TokenKind.OPEN_BRACKET;
            case '}', ']', ')' -> TokenKind.CLOSE_BRACKET;
            case '"', '\'', '`' -> TokenKind.STRING;
            default -> OPERATEURS.indexOf(caractere) >= 0
                    ? TokenKind.OPERATOR : TokenKind.PUNCTUATION;
        };
    }

    /** La sorte d'un symbole nommé : le mot réservé de la table, la
     *  construction par son nom — l'identificateur en repli. */
    private static TokenKind sorteDuNom(String nom) {
        TokenKind reservee = SORTES.get(nom);
        if (reservee != null) {
            return reservee;
        }
        if (nom.contains("comment")) {
            return TokenKind.COMMENT;
        }
        if (nom.contains("string") || nom.contains("regex")
                || nom.contains("escape")) {
            return TokenKind.STRING;
        }
        if (nom.contains("integer") || nom.contains("float")
                || nom.equals("number")) {
            return TokenKind.NUMBER;
        }
        if (nom.contains("identifier")) {
            return TokenKind.IDENTIFIER;
        }
        if (nom.contains("decorator") || nom.contains("attribute")) {
            return TokenKind.ANNOTATION;
        }
        return TokenKind.IDENTIFIER;
    }
}
