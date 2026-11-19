package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * La génération de code (B-24) : constructeur, accesseurs,
 * equals/hashCode/toString — générés depuis les champs que l'arbre
 * déclare, conformes aux conventions Java. La position d'insertion vit
 * avant l'accolade fermante de la classe porteuse.
 */
public final class Generateurs {

    /** Un champ déclaré : son type et son nom. */
    public record Champ(String type, String nom) {
    }

    private Generateurs() {
    }

    /**
     * Les champs d'une classe, lus dans l'arbre — types compris, dans
     * l'ordre du source.
     *
     * @param doc       le document porteur
     * @param analyse   l'analyse courante du document
     * @param nomClasse le nom de la classe porteuse
     * @return ses champs, vide si la classe est inconnue ou vide
     */
    public static List<Champ> champs(Document doc, Analysis analyse, String nomClasse) {
        Symbole classe = classeDe(doc, analyse, nomClasse);
        if (classe == null) {
            return List.of();
        }
        List<Champ> champs = new ArrayList<>();
        for (OutlineEntry input : TreeOutline.entries(doc, analyse)) {
            if (input.category() == NodeCategory.FIELD
                    && input.startOffset() >= classe.start()
                    && input.endOffset() <= classe.end()) {
                String type = typeDuChamp(doc, analyse, input);
                champs.add(new Champ(type, input.name()));
            }
        }
        return List.copyOf(champs);
    }

    /** Le type d'un champ : les jetons qui précèdent son nom sur sa ligne, recomposés. */
    private static String typeDuChamp(Document doc, Analysis analyse, OutlineEntry input) {
        int line = doc.lineForOffset(input.startOffset());
        int base = doc.lineStart(line);
        Token[] tokens = analyse.lex().line(line).tokens();
        int indexNom = -1;
        for (int i = 0; i < tokens.length; i++) {
            String text = doc.subText(base + tokens[i].start(),
                    base + tokens[i].end()).trim();
            if (tokens[i].kind() == TokenKind.IDENTIFIER && text.equals(input.name())) {
                indexNom = i;
                break;
            }
        }
        StringBuilder type = new StringBuilder();
        for (int i = indexNom - 1; i >= 0; i--) {
            Token token = tokens[i];
            String text = doc.subText(base + token.start(), base + token.end()).trim();
            // les types à majuscule initiale sont des TYPE, les primitives
            // sont des mots réservés (KEYWORD) — les deux sont des types de
            // champ, jamais des noms de champ (les primitifs sont réservés)
            if (token.kind() == TokenKind.TYPE || estPrimitif(text)) {
                type.insert(0, text);
                continue;
            }
            if (token.kind() == TokenKind.OPERATOR
                    && ("<".equals(text) || ">".equals(text))) {
                type.insert(0, text);
                continue;
            }
            break;
        }
        return type.toString();
    }

    /**
     * La classe nommée, lue dans l'arbre.
     *
     * @param doc       le document porteur
     * @param analyse   l'analyse courante
     * @param nomClasse le nom cherché
     * @return la classe, ou null si le document n'en déclare aucune de ce nom
     */
    public static @org.jspecify.annotations.Nullable Symbole classeDe(Document doc,
            Analysis analyse, String nomClasse) {
        for (OutlineEntry input : TreeOutline.entries(doc, analyse)) {
            if (input.category() == NodeCategory.TYPE && input.name().equals(nomClasse)) {
                return new Symbole(input.name(), input.category(), "",
                        input.startOffset(), input.endOffset(), input.startLine());
            }
        }
        return null;
    }

    /** La position d'insertion du code généré : avant l'accolade qui ferme la classe. */
    public static int positionInsertion(Symbole classe) {
        return Math.max(classe.start(), classe.end() - 1);
    }

    /**
     * Le constructeur de la classe : un paramètre et une affectation par
     * champ.
     *
     * @param classe le nom de la classe
     * @param champs ses champs
     * @return le texte du constructeur
     */
    public static String constructeur(String classe, List<Champ> champs) {
        StringBuilder text = new StringBuilder("public ").append(classe).append('(');
        for (int i = 0; i < champs.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(champs.get(i).type()).append(' ').append(champs.get(i).nom());
        }
        text.append(") {\n");
        for (Champ champ : champs) {
            text.append("    this.").append(champ.nom()).append(" = ")
                    .append(champ.nom()).append(";\n");
        }
        return text + "}";
    }

    /**
     * Les accesseurs des champs : getter et setter par champ — le
     * booléen en {@code is}, jamais en {@code get}.
     *
     * @param champs les champs
     * @return le texte des accesseurs, vide sans champ
     */
    public static String accesseurs(List<Champ> champs) {
        StringBuilder text = new StringBuilder();
        for (Champ champ : champs) {
            String nomMajuscule = Character.toUpperCase(champ.nom().charAt(0))
                    + champ.nom().substring(1);
            String prefixe = "boolean".equals(champ.type()) ? "is" : "get";
            text.append("public ").append(champ.type()).append(' ')
                    .append(prefixe).append(nomMajuscule).append("() {\n")
                    .append("    return ").append(champ.nom()).append(";\n")
                    .append("}\n\n");
            text.append("public void set").append(nomMajuscule).append('(')
                    .append(champ.type()).append(' ').append(champ.nom()).append(") {\n")
                    .append("    this.").append(champ.nom()).append(" = ")
                    .append(champ.nom()).append(";\n")
                    .append("}\n\n");
        }
        return text.toString().stripTrailing();
    }

    /**
     * equals, hashCode et toString : tous les champs, les conventions
     * tenues — primitives comparées, objets à {@code Objects.equals},
     * hachage par {@code Objects.hash}.
     *
     * @param classe le nom de la classe
     * @param champs ses champs
     * @return le texte des trois méthodes
     */
    public static String equalsHashCodeToString(String classe, List<Champ> champs) {
        StringBuilder equals = new StringBuilder();
        for (int i = 0; i < champs.size(); i++) {
            if (i > 0) {
                equals.append(" && ");
            }
            Champ champ = champs.get(i);
            if (estPrimitif(champ.type())) {
                equals.append(champ.nom()).append(" == autre.").append(champ.nom());
            } else {
                equals.append("Objects.equals(").append(champ.nom())
                        .append(", autre.").append(champ.nom()).append(')');
            }
        }
        StringBuilder hash = new StringBuilder();
        StringBuilder toString = new StringBuilder();
        for (int i = 0; i < champs.size(); i++) {
            if (i > 0) {
                hash.append(", ");
                toString.append(" + \", ");
            }
            hash.append(champs.get(i).nom());
            toString.append(champs.get(i).nom()).append("=\" + ").append(champs.get(i).nom());
        }
        return "@Override\n"
                + "public boolean equals(Object o) {\n"
                + "    if (this == o) {\n"
                + "        return true;\n"
                + "    }\n"
                + "    if (!(o instanceof " + classe + " autre)) {\n"
                + "        return false;\n"
                + "    }\n"
                + "    return " + (equals.length() == 0 ? "true" : equals) + ";\n"
                + "}\n\n"
                + "@Override\n"
                + "public int hashCode() {\n"
                + "    return Objects.hash(" + hash + ");\n"
                + "}\n\n"
                + "@Override\n"
                + "public String toString() {\n"
                + "    return \"" + classe + "{" + toString + " + '}';\n"
                + "}";
    }

    private static boolean estPrimitif(String type) {
        return switch (type) {
            case "int", "long", "short", "byte", "char", "boolean", "float", "double" -> true;
            default -> false;
        };
    }
}
