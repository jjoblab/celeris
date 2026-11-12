package jo.celeris.core.syntax.json;

import java.util.ArrayList;
import java.util.List;
import jo.celeris.core.syntax.Jetons;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;

/**
 * Parseur JSON tolérant (ADR-004) : chaque valeur de premier niveau
 * ouvre son chunk — les valeurs frères ne se ré-analysent pas ensemble
 * (PIEGE-13 : le document ne vit plus en un chunk unique, le budget de
 * ré-analyse tient sur machine lente). Les objets et tableaux deviennent
 * des nœuds
 * repliables, chaque paire un nœud FIELD nommé par sa clé (le plan
 * liste les clés), les scalaires des plages couvertes. La virgule
 * manquante ou la valeur étrange ne lève jamais : la malformation
 * devient un nœud d'erreur et l'analyse continue.
 */
final class JsonParseur implements LanguageParser {

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
        return sorte == TokenKind.OPEN_BRACKET
                || sorte == TokenKind.STRING
                || sorte == TokenKind.NUMBER
                || sorte == TokenKind.IDENTIFIER
                || sorte == TokenKind.KEYWORD;
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(JsonKind.AUTRE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        if (slice.kind(0) == TokenKind.OPEN_BRACKET) {
            return contenant(slice, 0, slice.size()).noeud;
        }
        return SynNode.of(JsonKind.AUTRE, NodeCategory.OTHER, 0,
                Jetons.finDe(slice, slice.size() - 1), new SynNode[0], null,
                false);
    }

    /** Un contenant analysé et l'indice du jeton qui le suit. */
    private static final class Analyse {
        final SynNode noeud;
        final int apres;

        Analyse(SynNode noeud, int apres) {
            this.noeud = noeud;
            this.apres = apres;
        }
    }

    private Analyse contenant(TokenSlice slice, int ouvert, int vers) {
        boolean objet = "{".equals(Jetons.texteCrochet(slice, ouvert));
        int fermante = slice.matchOf(ouvert);
        List<SynNode> enfants = new ArrayList<>();
        boolean erreur = fermante < 0;
        int borne = fermante < 0 ? vers : fermante;
        int i = ouvert + 1;
        while (i < borne) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.CLOSE_BRACKET) {
                break;
            }
            if (objet && sorte == TokenKind.STRING && i + 1 < borne
                    && ":".equals(Jetons.mot(slice, i + 1))) {
                i = paire(slice, i, borne, enfants);
            } else if (sorte == TokenKind.OPEN_BRACKET) {
                Analyse enfant = contenant(slice, i, borne);
                enfants.add(enfant.noeud);
                i = Math.max(enfant.apres, i + 1);
            } else {
                i = scalaire(slice, i, borne, enfants);
            }
        }
        int fin = fermante < 0 ? Jetons.finDe(slice, vers - 1)
                : slice.end(fermante);
        if (fermante < 0) {
            enfants.add(SynNode.of(JsonKind.ERREUR, NodeCategory.ERROR,
                    slice.start(ouvert), fin, new SynNode[0], null, true));
        }
        SynNode noeud = SynNode.of(objet ? JsonKind.OBJET : JsonKind.TABLEAU,
                NodeCategory.TAG, slice.start(ouvert), fin,
                enfants.toArray(new SynNode[0]), null, erreur);
        return new Analyse(noeud, fermante < 0 ? vers : fermante + 1);
    }

    /** La paire «&#x2f;"clé" : valeur&#x2f;» : la clé nomme le nœud FIELD. */
    private int paire(TokenSlice slice, int cle, int borne,
                      List<SynNode> enfants) {
        int i = cle + 2;
        List<SynNode> valeur = new ArrayList<>();
        if (i < borne && slice.kind(i) == TokenKind.OPEN_BRACKET) {
            Analyse enfant = contenant(slice, i, borne);
            valeur.add(enfant.noeud);
            i = Math.max(enfant.apres, i + 1);
        } else {
            i = scalaire(slice, i, borne, valeur);
        }
        String brut = Jetons.mot(slice, cle);
        String nom = brut.length() >= 2 && brut.charAt(0) == '"'
                && brut.charAt(brut.length() - 1) == '"'
                        ? brut.substring(1, brut.length() - 1) : brut;
        enfants.add(SynNode.of(JsonKind.PAIRE, NodeCategory.FIELD,
                slice.start(cle), Jetons.finDe(slice, i - 1),
                valeur.toArray(new SynNode[0]), nom, false));
        return i;
    }

    /** Les scalaires d'une valeur, jusqu'à la virgule ou la clôture. */
    private int scalaire(TokenSlice slice, int i, int borne,
                         List<SynNode> enfants) {
        int fin = i;
        while (fin < borne && slice.kind(fin) != TokenKind.PUNCTUATION
                && slice.kind(fin) != TokenKind.CLOSE_BRACKET) {
            fin++;
        }
        if (fin > i) {
            enfants.add(SynNode.of(JsonKind.VALEUR, NodeCategory.OTHER,
                    slice.start(i), Jetons.finDe(slice, fin - 1),
                    new SynNode[0], null, false));
        }
        if (fin < borne && ",".equals(Jetons.mot(slice, fin))) {
            return fin + 1;
        }
        return Math.max(fin, i + 1);
    }
}
