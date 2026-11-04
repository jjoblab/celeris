package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Parseur Java tolérant (ADR-004) : découpe les chunks de haut niveau
 * (paquet et imports, puis chaque type), analyse chaque type en membres
 * (méthodes, champs, types imbriqués, initialiseurs) et délègue chaque
 * corps à {@link JavaCorps} — les expressions restent des plages, l'arbre
 * sert la coloration, les plis, le plan et l'indentation, pas la
 * sémantique. Jamais d'exception : la malformation devient un nœud
 * d'erreur et l'analyse continue.
 */
final class JavaParseur implements LanguageParser {

    private static final Set<String> DECLARATIONS_TYPE = Set.of(
            "class", "interface", "enum", "record");

    private static final Set<String> MODIFICATEURS = Set.of(
            "public", "private", "protected", "static", "final", "abstract",
            "sealed", "strictfp", "transient", "volatile", "native",
            "synchronized", "default", "non", "open", "module", "transitive");

    @Override
    public int chunkLevel() {
        return 0;
    }

    @Override
    public boolean startsChunk(TokenSlice slice, int index, int depth) {
        if (depth != 0 || index == 0) {
            return false;
        }
        TokenKind sorte = slice.kind(index);
        boolean candidat = sorte == TokenKind.ANNOTATION
                || (sorte == TokenKind.KEYWORD
                        && (DECLARATIONS_TYPE.contains(Jetons.mot(slice, index))
                                || MODIFICATEURS.contains(Jetons.mot(slice, index))));
        if (!candidat) {
            return false;
        }
        TokenKind precedente = slice.kind(index - 1);
        return precedente == TokenKind.PUNCTUATION
                || precedente == TokenKind.CLOSE_BRACKET
                || precedente == TokenKind.COMMENT
                || precedente == TokenKind.DOC
                || precedente == TokenKind.ANNOTATION;
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(JavaKind.AUTRE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        int apresTrivia = Jetons.sauterTrivia(slice, 0);
        int apresTete = sauterTete(slice, apresTrivia);
        if (apresTete < slice.size() && estDeclarationType(slice, apresTete)) {
            return parseType(slice, 0, apresTete);
        }
        List<SynNode> enfants = JavaCorps.instructions(slice, 0, slice.size());
        return SynNode.of(JavaKind.AUTRE, NodeCategory.OTHER, 0,
                Jetons.finDe(slice, slice.size() - 1), enEnfants(enfants), null,
                contientErreur(enfants));
    }

    private SynNode parseType(TokenSlice slice, int debut, int motCle) {
        String nom = motCle + 1 < slice.size() ? Jetons.mot(slice, motCle + 1) : null;
        int ouvrante = Jetons.chercherAccolade(slice, motCle + 2);
        if (ouvrante < 0) {
            return noeudFeuille(slice, debut, motCle, nom);
        }
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0) {
            List<SynNode> membres = parseMembres(slice, ouvrante + 1, slice.size());
            membres.add(SynNode.of(JavaKind.ERREUR, NodeCategory.ERROR,
                    slice.start(ouvrante), Jetons.finDe(slice, slice.size() - 1),
                    new SynNode[0], null, true));
            return SynNode.of(JavaKind.TYPE, NodeCategory.TYPE,
                    slice.start(debut), Jetons.finDe(slice, slice.size() - 1),
                    enEnfants(membres), nom, true);
        }
        List<SynNode> membres = parseMembres(slice, ouvrante + 1, fermante);
        return SynNode.of(JavaKind.TYPE, NodeCategory.TYPE, slice.start(debut),
                slice.end(fermante), enEnfants(membres), nom, false);
    }

    private SynNode noeudFeuille(TokenSlice slice, int debut, int motCle,
                                 @Nullable String nom) {
        int fin = Math.min(motCle + 2, slice.size() - 1);
        boolean erreur = false;
        for (int i = motCle; i <= fin; i++) {
            if (slice.kind(i) == TokenKind.ERROR) {
                erreur = true;
            }
        }
        return SynNode.of(JavaKind.TYPE, NodeCategory.TYPE, slice.start(debut),
                Jetons.finDe(slice, fin), new SynNode[0], nom, erreur);
    }

    private List<SynNode> parseMembres(TokenSlice slice, int depuis, int vers) {
        List<SynNode> membres = new ArrayList<>();
        int i = depuis;
        while (i < vers) {
            int apresTrivia = Jetons.sauterTrivia(slice, i);
            if (apresTrivia >= vers) {
                break;
            }
            i = parserUnMembre(slice, i, vers, membres);
        }
        return membres;
    }

    private int parserUnMembre(TokenSlice slice, int debut, int vers, List<SynNode> membres) {
        int apresTrivia = Jetons.sauterTrivia(slice, debut);
        int apresTete = sauterTete(slice, apresTrivia);
        if (apresTete < vers && estDeclarationType(slice, apresTete)) {
            membres.add(parseType(slice, debut, apresTete));
            return apresMembreType(slice, apresTete, vers);
        }
        ScanMembre scan = scannerMembre(slice, apresTrivia, vers);
        membres.add(construireMembre(slice, debut, scan));
        return scan.fin;
    }

    private int apresMembreType(TokenSlice slice, int motCle, int vers) {
        int ouvrante = Jetons.chercherAccolade(slice, motCle + 2);
        if (ouvrante < 0) {
            return vers;
        }
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0 || fermante >= vers) {
            return vers;
        }
        return fermante + 1;
    }

    private static final class ScanMembre {
        int fin;
        int corps = -1;
        int fermanteCorps = -1;
        boolean egalVu;
        boolean erreur;
    }

    private ScanMembre scannerMembre(TokenSlice slice, int debut, int vers) {
        ScanMembre scan = new ScanMembre();
        scan.fin = vers;
        for (int i = debut; i < vers; i++) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET) {
                if (!scan.egalVu && "{".equals(Jetons.texteCrochet(slice, i))) {
                    int fermante = slice.matchOf(i);
                    scan.corps = i;
                    scan.fermanteCorps = fermante;
                    scan.fin = fermante < 0 ? vers : fermante + 1;
                    return scan;
                }
                int fermante = slice.matchOf(i);
                if (fermante < 0) {
                    scan.fin = vers;
                    scan.erreur = true;
                    return scan;
                }
                i = fermante;
            } else if (sorte == TokenKind.CLOSE_BRACKET) {
                scan.fin = i;
                return scan;
            } else if (sorte == TokenKind.OPERATOR
                    && "=".equals(Jetons.mot(slice, i))) {
                scan.egalVu = true;
            } else if (sorte == TokenKind.PUNCTUATION
                    && ";".equals(Jetons.mot(slice, i))) {
                scan.fin = i + 1;
                return scan;
            }
        }
        return scan;
    }

    private SynNode construireMembre(TokenSlice slice, int debut, ScanMembre scan) {
        String nom = chercherNom(slice, debut, scan);
        if (scan.corps >= 0) {
            int vers = scan.fermanteCorps < 0 ? slice.size() : scan.fermanteCorps;
            List<SynNode> corps = JavaCorps.instructions(slice, scan.corps + 1, vers);
            boolean erreur = scan.fermanteCorps < 0;
            if (erreur) {
                corps.add(SynNode.of(JavaKind.ERREUR, NodeCategory.ERROR,
                        slice.start(scan.corps), Jetons.finDe(slice, slice.size() - 1),
                        new SynNode[0], null, true));
            }
            boolean methode = premiereParenthese(slice, debut, scan) >= 0;
            return SynNode.of(methode ? JavaKind.METHOD : JavaKind.INITIALIZER,
                    methode ? NodeCategory.FUNCTION : NodeCategory.FIELD,
                    slice.start(debut), Jetons.finDe(slice, scan.fin - 1),
                    enEnfants(corps), nom, erreur);
        }
        return SynNode.of(JavaKind.FIELD, NodeCategory.FIELD, slice.start(debut),
                Jetons.finDe(slice, scan.fin - 1), new SynNode[0], nom, scan.erreur);
    }

    private int premiereParenthese(TokenSlice slice, int debut, ScanMembre scan) {
        for (int i = debut; i < scan.fin && i < slice.size(); i++) {
            if (slice.kind(i) == TokenKind.OPEN_BRACKET
                    && "(".equals(Jetons.texteCrochet(slice, i))) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable String chercherNom(TokenSlice slice, int debut, ScanMembre scan) {
        for (int i = debut; i < scan.fin && i < slice.size(); i++) {
            TokenKind sorte = slice.kind(i);
            if (sorte != TokenKind.IDENTIFIER && sorte != TokenKind.TYPE
                    && sorte != TokenKind.CONSTANT) {
                continue;
            }
            if (i + 1 >= slice.size()) {
                return sorte == TokenKind.IDENTIFIER ? Jetons.mot(slice, i) : null;
            }
            TokenKind suivant = slice.kind(i + 1);
            if (suivant == TokenKind.OPEN_BRACKET
                    && "(".equals(Jetons.texteCrochet(slice, i + 1))) {
                return Jetons.mot(slice, i);
            }
            if (suivant == TokenKind.OPERATOR || suivant == TokenKind.PUNCTUATION) {
                if (sorte == TokenKind.IDENTIFIER || sorte == TokenKind.CONSTANT) {
                    return Jetons.mot(slice, i);
                }
            }
        }
        return null;
    }

    private static boolean estDeclarationType(TokenSlice slice, int index) {
        if (slice.kind(index) != TokenKind.KEYWORD) {
            return slice.kind(index) == TokenKind.ANNOTATION
                    && "@interface".equals(Jetons.mot(slice, index));
        }
        return DECLARATIONS_TYPE.contains(Jetons.mot(slice, index));
    }

    private static int sauterTete(TokenSlice slice, int depuis) {
        int i = depuis;
        while (i < slice.size()) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.ANNOTATION) {
                i++;
                if (i < slice.size() && "(".equals(Jetons.texteCrochet(slice, i))) {
                    int fermante = slice.matchOf(i);
                    if (fermante < 0) {
                        return slice.size();
                    }
                    i = fermante + 1;
                }
                continue;
            }
            if (sorte == TokenKind.KEYWORD
                    && MODIFICATEURS.contains(Jetons.mot(slice, i))) {
                i++;
                continue;
            }
            return i;
        }
        return i;
    }

    private static SynNode[] enEnfants(List<SynNode> enfants) {
        return enfants.toArray(new SynNode[0]);
    }

    private static boolean contientErreur(List<SynNode> enfants) {
        for (SynNode enfant : enfants) {
            if (enfant.isError()) {
                return true;
            }
        }
        return false;
    }
}
