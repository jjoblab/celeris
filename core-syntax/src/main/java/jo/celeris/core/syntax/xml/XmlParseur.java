package jo.celeris.core.syntax.xml;

import java.util.ArrayList;
import java.util.List;
import jo.celeris.core.syntax.Tokens;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;

/**
 * Parseur XML tolérant (ADR-004) : chaque élément devient un nœud TAG
 * nommé et repliable — attributs couverts par l'envergure, enfants
 * imbriqués, texte et sections CDATA couverts sans nœud propre. La
 * fermeture manquante ou désassortie devient un nœud d'erreur et
 * l'analyse continue ; le prologue et les commentaires d'un chunk sans
 * élément restent une enveloppe. Jamais d'exception.
 */
final class XmlParseur implements LanguageParser {

    @Override
    public int chunkLevel() {
        return 0;
    }

    @Override
    public boolean startsChunk(TokenSlice slice, int index, int depth) {
        if (depth != 0 || index == 0) {
            return false;
        }
        return ouvreElement(slice, index);
    }

    @Override
    public SynNode parseChunk(TokenSlice slice) {
        if (slice.size() == 0) {
            return SynNode.of(XmlKind.AUTRE, NodeCategory.OTHER, 0, 0,
                    new SynNode[0], null, false);
        }
        if (ouvreElement(slice, 0)) {
            List<SynNode> racines = new ArrayList<>();
            element(slice, 0, slice.size(), racines);
            if (racines.size() == 1) {
                return racines.get(0);
            }
        }
        return SynNode.of(XmlKind.AUTRE, NodeCategory.OTHER, 0,
                Tokens.finDe(slice, slice.size() - 1), new SynNode[0], null,
                false);
    }

    /**
     * Analyse l'élément débutant à {@code depuis}, l'ajoute à
     * {@code enfants} et renvoie l'indice du jeton suivant.
     */
    private int element(TokenSlice slice, int depuis, int vers,
                        List<SynNode> enfants) {
        String nom = Tokens.mot(slice, depuis).substring(1);
        int i = depuis + 1;
        while (i < vers && slice.kind(i) != TokenKind.PUNCTUATION
                && slice.kind(i) != TokenKind.CLOSE_BRACKET) {
            i++;
        }
        if (i >= vers) {
            enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                    slice.start(depuis), Tokens.finDe(slice, vers - 1),
                    new SynNode[0], nom, true));
            return vers;
        }
        if (slice.kind(i) == TokenKind.CLOSE_BRACKET) {
            enfants.add(SynNode.of(XmlKind.ELEMENT, NodeCategory.TAG,
                    slice.start(depuis), slice.end(i), new SynNode[0], nom,
                    false));
            return i + 1;
        }
        return body(slice, depuis, nom, i + 1, vers, enfants);
    }

    private int body(TokenSlice slice, int start, String nom, int depuis,
                      int vers, List<SynNode> freres) {
        List<SynNode> enfants = new ArrayList<>();
        boolean erreur = false;
        int j = depuis;
        while (j < vers) {
            TokenKind sorte = slice.kind(j);
            if (sorte == TokenKind.OPEN_BRACKET && ouvreElement(slice, j)) {
                j = element(slice, j, vers, enfants);
            } else if (sorte == TokenKind.CLOSE_BRACKET
                    && Tokens.mot(slice, j).startsWith("</")) {
                break;
            } else {
                j++;
            }
        }
        int end;
        if (j < vers) {
            if (!fermetureAssortie(slice, j, nom)) {
                enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                        slice.start(j), slice.end(j), new SynNode[0], null,
                        true));
                erreur = true;
            }
            end = slice.end(j);
            j = j + 1;
        } else {
            enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                    slice.start(start), Tokens.finDe(slice, vers - 1),
                    new SynNode[0], null, true));
            erreur = true;
            end = Tokens.finDe(slice, vers - 1);
            j = vers;
        }
        freres.add(SynNode.of(XmlKind.ELEMENT, NodeCategory.TAG,
                slice.start(start), end, enfants.toArray(new SynNode[0]), nom,
                erreur));
        return j;
    }

    /** Vrai quand le jeton ouvre un élément : «&#x2f;» suivi d'une lettre. */
    private static boolean ouvreElement(TokenSlice slice, int index) {
        if (slice.kind(index) != TokenKind.OPEN_BRACKET) {
            return false;
        }
        String text = Tokens.mot(slice, index);
        return text.length() > 1 && text.charAt(0) == '<'
                && Character.isLetter(text.charAt(1));
    }

    private static boolean fermetureAssortie(TokenSlice slice, int index,
                                             String nom) {
        return Tokens.mot(slice, index).equals("</" + nom + ">");
    }
}
