package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Parseur Kotlin tolérant (ADR-004) : découpe les chunks de haut niveau
 * (paquet et imports, puis chaque déclaration avec sa documentation),
 * analyse les classes, objets et interfaces en membres — fonctions,
 * propriétés, types imbriqués, objets compagnons, blocs init — et délègue
 * les corps à {@link Corps}. Les fonctions et propriétés de premier
 * niveau sont des chunk racines à part entière. Jamais d'exception : la
 * malformation devient un nœud d'erreur et l'analyse continue.
 */
final class KotlinParseur implements LanguageParser {

    private static final Set<String> DECLARATIONS_TYPE = Set.of(
            "class", "object", "interface");

    private static final Set<String> PROPRIETES = Set.of("val", "var");

    private static final Set<String> MODIFICATEURS = Set.of(
            "private", "public", "internal", "protected", "suspend", "lateinit",
            "const", "final", "open", "abstract", "sealed", "data", "companion",
            "enum", "annotation", "operator", "infix", "inline", "external",
            "actual", "expect", "tailrec", "vararg", "out", "reified", "override",
            "crossinline", "noinline");

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
                                || PROPRIETES.contains(Jetons.mot(slice, index))
                                || "fun".equals(Jetons.mot(slice, index))
                                || MODIFICATEURS.contains(Jetons.mot(slice, index))));
        if (!candidat) {
            return false;
        }
        TokenKind precedente = slice.kind(index - 1);
        boolean nouvelleLigne = slice.line(index) > slice.line(index - 1);
        return nouvelleLigne
                || precedente == TokenKind.PUNCTUATION
                || precedente == TokenKind.CLOSE_BRACKET
                || precedente == TokenKind.COMMENT
                || precedente == TokenKind.DOC
                || precedente == TokenKind.ANNOTATION;
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(KotlinKind.AUTRE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        int apresTrivia = Jetons.sauterTrivia(slice, 0);
        int apresTete = sauterTete(slice, apresTrivia);
        if (apresTete < slice.size()) {
            String mot = Jetons.mot(slice, apresTete);
            if (DECLARATIONS_TYPE.contains(mot)) {
                return type(slice, 0, apresTete);
            }
            if ("fun".equals(mot)) {
                return membre(slice, 0, apresTete);
            }
            if (PROPRIETES.contains(mot)) {
                return membre(slice, 0, apresTete);
            }
        }
        List<SynNode> enfants = Corps.instructions(slice, 0, slice.size(),
                KotlinKind.STATEMENT, KotlinKind.BLOC, KotlinKind.ERREUR);
        return SynNode.of(KotlinKind.AUTRE, NodeCategory.OTHER, 0,
                Jetons.finDe(slice, slice.size() - 1), enEnfants(enfants), null,
                contientErreur(enfants));
    }

    private SynNode type(TokenSlice slice, int debut, int motCle) {
        String nom = nomApres(slice, motCle);
        int ouvrante = Jetons.chercherAccolade(slice, motCle + 1);
        if (ouvrante < 0) {
            return feuille(slice, debut, motCle, nom);
        }
        int fermante = slice.matchOf(ouvrante);
        if (fermante < 0) {
            List<SynNode> membres = membres(slice, ouvrante + 1, slice.size());
            membres.add(SynNode.of(KotlinKind.ERREUR, NodeCategory.ERROR,
                    slice.start(ouvrante), Jetons.finDe(slice, slice.size() - 1),
                    new SynNode[0], null, true));
            return SynNode.of(KotlinKind.TYPE, NodeCategory.TYPE,
                    slice.start(debut), Jetons.finDe(slice, slice.size() - 1),
                    enEnfants(membres), nom, true);
        }
        List<SynNode> membres = membres(slice, ouvrante + 1, fermante);
        return SynNode.of(KotlinKind.TYPE, NodeCategory.TYPE, slice.start(debut),
                slice.end(fermante), enEnfants(membres), nom, false);
    }

    private SynNode feuille(TokenSlice slice, int debut, int motCle,
                            @Nullable String nom) {
        return SynNode.of(KotlinKind.TYPE, NodeCategory.TYPE, slice.start(debut),
                Jetons.finDe(slice, Math.min(motCle + 2, slice.size() - 1)),
                new SynNode[0], nom, false);
    }

    private List<SynNode> membres(TokenSlice slice, int depuis, int vers) {
        List<SynNode> membres = new ArrayList<>();
        int i = depuis;
        while (i < vers) {
            int apresTrivia = Jetons.sauterTrivia(slice, i);
            if (apresTrivia >= vers) {
                break;
            }
            i = unMembre(slice, i, vers, membres);
        }
        return membres;
    }

    private int unMembre(TokenSlice slice, int debut, int vers,
                         List<SynNode> membres) {
        int apresTrivia = Jetons.sauterTrivia(slice, debut);
        int apresTete = sauterTete(slice, apresTrivia);
        if (apresTete < vers) {
            String mot = Jetons.mot(slice, apresTete);
            if (DECLARATIONS_TYPE.contains(mot)) {
                int ouvrante = Jetons.chercherAccolade(slice, apresTete + 1);
                int fermante = ouvrante < 0 ? vers : slice.matchOf(ouvrante);
                membres.add(type(slice, debut, apresTete));
                return fermante < 0 ? vers : fermante + 1;
            }
        }
        membres.add(membre(slice, debut, apresTete));
        return finDeMembre(slice, apresTete, vers);
    }

    /** Le résultat du scan d'un membre : son corps et sa fin. */
    private record Scan(int ouvrante, int fermante, int fin) {
    }

    /**
     * Fonction, propriété ou bloc init — le membre nommé, son corps
     * éventuel et sa fin (la sienne : Kotlin vit sans point-virgule).
     */
    private SynNode membre(TokenSlice slice, int debut, int motCle) {
        String mot = Jetons.mot(slice, motCle);
        boolean fonction = "fun".equals(mot) || "init".equals(mot);
        @Nullable String nom = nomApres(slice, motCle);
        Scan scan = scannerMembre(slice, motCle);
        int ouvrante = scan.ouvrante();
        int fermante = scan.fermante();
        int fin = scan.fin();
        boolean erreur = ouvrante >= 0 && fermante < 0;
        List<SynNode> corps = ouvrante < 0 ? new ArrayList<>()
                : Corps.instructions(slice, ouvrante + 1,
                        fermante < 0 ? slice.size() : fermante,
                        KotlinKind.STATEMENT, KotlinKind.BLOC, KotlinKind.ERREUR);
        if (erreur) {
            corps.add(SynNode.of(KotlinKind.ERREUR, NodeCategory.ERROR,
                    slice.start(ouvrante), Jetons.finDe(slice, slice.size() - 1),
                    new SynNode[0], null, true));
        }
        return SynNode.of(fonction ? KotlinKind.METHOD : KotlinKind.FIELD,
                fonction ? NodeCategory.FUNCTION : NodeCategory.FIELD,
                slice.start(debut),
                Jetons.finDe(slice, Math.min(fin, slice.size()) - 1),
                enEnfants(corps), nom, erreur);
    }

    /**
     * Le scan tolérant d'un membre : son corps d'accolades, ou la fin de
     * sa ligne — Kotlin vit sans point-virgule, la déclaration s'arrête
     * où sa ligne s'arrête (les constructions à cheval restent tolérées
     * en plages).
     */
    private static Scan scannerMembre(TokenSlice slice, int motCle) {
        int i = motCle + 1;
        boolean egalVu = false;
        while (i < slice.size()) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET) {
                int close = slice.matchOf(i);
                if ("{".equals(Jetons.texteCrochet(slice, i)) && !egalVu) {
                    return new Scan(i, close,
                            close < 0 ? slice.size() : close + 1);
                }
                if (close < 0) {
                    return new Scan(-1, -1, slice.size());
                }
                i = close + 1;
                continue;
            }
            if (sorte == TokenKind.CLOSE_BRACKET || slice.line(i) > slice.line(motCle)) {
                return new Scan(-1, -1, i);
            }
            if (sorte == TokenKind.OPERATOR && "=".equals(Jetons.mot(slice, i))) {
                egalVu = true;
            }
            i++;
        }
        return new Scan(-1, -1, slice.size());
    }

    /** La fin d'un membre déclaré : après son corps ou à la fin de sa ligne. */
    private int finDeMembre(TokenSlice slice, int motCle, int vers) {
        Scan scan = scannerMembre(slice, motCle);
        if (scan.ouvrante() >= 0 && scan.fermante() >= 0) {
            return scan.fermante() + 1;
        }
        if (scan.fin() < vers) {
            return scan.fin();
        }
        return vers;
    }

    /** Le nom qui suit un mot-clé de déclaration, ou null (objet anonyme). */
    private static @Nullable String nomApres(TokenSlice slice, int motCle) {
        if (motCle + 1 >= slice.size()) {
            return null;
        }
        TokenKind sorte = slice.kind(motCle + 1);
        if (sorte == TokenKind.IDENTIFIER || sorte == TokenKind.TYPE
                || sorte == TokenKind.CONSTANT) {
            return Jetons.mot(slice, motCle + 1);
        }
        return null;
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
