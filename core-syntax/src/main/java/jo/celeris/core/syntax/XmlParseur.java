package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

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
                Jetons.finDe(slice, slice.size() - 1), new SynNode[0], null,
                false);
    }

    /**
     * Analyse l'élément débutant à {@code depuis}, l'ajoute à
     * {@code enfants} et renvoie l'indice du jeton suivant.
     */
    private int element(TokenSlice slice, int depuis, int vers,
                        List<SynNode> enfants) {
        String nom = Jetons.mot(slice, depuis).substring(1);
        int i = depuis + 1;
        while (i < vers && slice.kind(i) != TokenKind.PUNCTUATION
                && slice.kind(i) != TokenKind.CLOSE_BRACKET) {
            i++;
        }
        if (i >= vers) {
            enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                    slice.start(depuis), Jetons.finDe(slice, vers - 1),
                    new SynNode[0], nom, true));
            return vers;
        }
        if (slice.kind(i) == TokenKind.CLOSE_BRACKET) {
            enfants.add(SynNode.of(XmlKind.ELEMENT, NodeCategory.TAG,
                    slice.start(depuis), slice.end(i), new SynNode[0], nom,
                    false));
            return i + 1;
        }
        return corps(slice, depuis, nom, i + 1, vers, enfants);
    }

    private int corps(TokenSlice slice, int debut, String nom, int depuis,
                      int vers, List<SynNode> freres) {
        List<SynNode> enfants = new ArrayList<>();
        boolean erreur = false;
        int j = depuis;
        while (j < vers) {
            TokenKind sorte = slice.kind(j);
            if (sorte == TokenKind.OPEN_BRACKET && ouvreElement(slice, j)) {
                j = element(slice, j, vers, enfants);
            } else if (sorte == TokenKind.CLOSE_BRACKET
                    && Jetons.mot(slice, j).startsWith("</")) {
                break;
            } else {
                j++;
            }
        }
        int fin;
        if (j < vers) {
            if (!fermetureAssortie(slice, j, nom)) {
                enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                        slice.start(j), slice.end(j), new SynNode[0], null,
                        true));
                erreur = true;
            }
            fin = slice.end(j);
            j = j + 1;
        } else {
            enfants.add(SynNode.of(XmlKind.ERREUR, NodeCategory.ERROR,
                    slice.start(debut), Jetons.finDe(slice, vers - 1),
                    new SynNode[0], null, true));
            erreur = true;
            fin = Jetons.finDe(slice, vers - 1);
            j = vers;
        }
        freres.add(SynNode.of(XmlKind.ELEMENT, NodeCategory.TAG,
                slice.start(debut), fin, enfants.toArray(new SynNode[0]), nom,
                erreur));
        return j;
    }

    /** Vrai quand le jeton ouvre un élément : «&#x2f;» suivi d'une lettre. */
    private static boolean ouvreElement(TokenSlice slice, int index) {
        if (slice.kind(index) != TokenKind.OPEN_BRACKET) {
            return false;
        }
        String texte = Jetons.mot(slice, index);
        return texte.length() > 1 && texte.charAt(0) == '<'
                && Character.isLetter(texte.charAt(1));
    }

    private static boolean fermetureAssortie(TokenSlice slice, int index,
                                             String nom) {
        return Jetons.mot(slice, index).equals("</" + nom + ">");
    }
}
