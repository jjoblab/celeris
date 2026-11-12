package jo.celeris.serveur.java;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;

import jo.celeris.serveur.java.JavaSymbols.Symbol;
/**
 * Le survol du mini serveur : le mot sous le curseur reçoit sa
 * documentation — celle du mot-clé du langage, ou celle du symbole
 * déclaré dans le document (la ligne de sa déclaration, mise en
 * exergue). Aucun mot, aucun survol : la réponse reste null, le
 * protocole dit son vide.
 */
public final class JavaHover {

    private JavaHover() {
    }

    /**
     * Le survol d'une position : le mot autour du curseur décide. Le
     * nom DÉCLARÉ se survole sur son site ; un USAGE du nom reçoit
     * la documentation de SA déclaration — la ligne de la
     * déclaration, mise en exergue, la plage ancre le mot survolé
     * (site d'appel : les bornes du mot ; site de déclaration : les
     * bornes du nom). Aucun mot, aucun survol : la réponse reste
     * null, le protocole dit son vide.
     *
     * @param source  le document suivi
     * @param symbols les symboles déjà extraits du document
     * @param offset  le décalage du curseur
     * @return le survol, ou null sans mot ni symbole
     */
    public static @Nullable Hover at(JavaSource source,
            List<Symbol> symbols, int offset) {
        String mot = source.wordAround(offset);
        if (mot == null) {
            return null;
        }
        // le symbole ne se survole que sur SON NOM : le corps d'une
        // classe ne vole pas le survol des mots-clés qu'il contient
        JavaSymbols.Symbol symbole = porteLeNom(symbols, offset);
        boolean surDeclaration = symbole != null;
        if (symbole == null) {
            // le site d'USAGE rend sa cible : le mot qui nomme un
            // symbole déclaré reçoit la documentation de SA
            // déclaration — le survol au bon moment, hors déclaration
            symbole = parSonNom(symbols, mot);
        }
        if (symbole != null) {
            JavaSource.Spot debut;
            JavaSource.Spot fin;
            String genre = genreDe(symbole.kind());
            String ligneDeclaree = source.line(
                    source.lineOf(symbole.start())).strip();
            if (surDeclaration) {
                // sur le site de déclaration : les bornes du nom déclaré
                debut = source.spot(symbole.nameStart());
                fin = source.spot(symbole.nameEnd());
                return new Hover(new org.eclipse.lsp4j.MarkupContent(
                        MarkupKind.MARKDOWN,
                        "**" + symbole.name() + "** — " + genre
                                + " dans ce document :\n\n```java\n"
                                + ligneDeclaree + "\n```"),
                        new Range(new Position(debut.line(), debut.column()),
                                new Position(fin.line(), fin.column())));
            }
            // sur un site d'usage : les bornes du mot survolé, la
            // déclaration citée dans le corps
            int debutMot = borneMotAvant(source, offset);
            debut = source.spot(debutMot);
            fin = source.spot(debutMot + mot.length());
            return new Hover(new org.eclipse.lsp4j.MarkupContent(
                    MarkupKind.MARKDOWN,
                    "**" + mot + "** — " + genre
                            + " : déclaré ainsi dans ce document :\n\n```java\n"
                            + ligneDeclaree + "\n```"),
                    new Range(new Position(debut.line(), debut.column()),
                            new Position(fin.line(), fin.column())));
        }
        String documentation = documentationMotCle(mot);
        if (documentation == null) {
            return null;
        }
        // les bornes du mot se scannent autour du curseur : le
        // milieu d'un mot tient le mot entier
        int debutMot = borneMotAvant(source, offset);
        int finMot = borneMotApres(source, offset);
        JavaSource.Spot debut = source.spot(debutMot);
        JavaSource.Spot fin = source.spot(finMot);
        return new Hover(new org.eclipse.lsp4j.MarkupContent(MarkupKind.MARKDOWN,
                "**" + mot + "** — " + documentation + "."),
                new Range(new Position(debut.line(), debut.column()),
                        new Position(fin.line(), fin.column())));
    }

    /** Le premier symbole (parcours en profondeur) qui porte ce nom. */
    private static @Nullable Symbol parSonNom(
            List<Symbol> symbols, String nom) {
        for (Symbol symbole : symbols) {
            if (symbole.name().equals(nom)) {
                return symbole;
            }
            Symbol imbrique = parSonNom(symbole.children(), nom);
            if (imbrique != null) {
                return imbrique;
            }
        }
        return null;
    }

    /** Le début du mot autour du curseur — le milieu tient le mot entier. */
    private static int borneMotAvant(JavaSource source, int offset) {
        int debutMot = offset;
        while (debutMot > 0 && Character.isJavaIdentifierPart(
                source.text().charAt(debutMot - 1))) {
            debutMot--;
        }
        return debutMot;
    }

    /** La fin du mot autour du curseur. */
    private static int borneMotApres(JavaSource source, int offset) {
        int finMot = offset;
        while (finMot < source.text().length()
                && Character.isJavaIdentifierPart(
                source.text().charAt(finMot))) {
            finMot++;
        }
        return finMot;
    }

    /** Le symbole le plus intime dont le NOM couvre le décalage. */
    private static @Nullable Symbol porteLeNom(
            List<Symbol> symbols, int offset) {
        for (Symbol symbole : symbols) {
            if (symbole.nameStart() <= offset
                    && offset <= symbole.nameEnd()) {
                return symbole;
            }
            Symbol imbrique = porteLeNom(symbole.children(),
                    offset);
            if (imbrique != null) {
                return imbrique;
            }
        }
        return null;
    }

    private static String genreDe(int kind) {
        return switch (kind) {
            case 5 -> "une classe déclarée";
            case 6 -> "une méthode déclarée";
            case 8 -> "un champ déclaré";
            case 9 -> "un constructeur déclaré";
            case 10 -> "une énumération déclarée";
            case 11 -> "une interface déclarée";
            case 23 -> "un enregistrement déclaré";
            default -> "un symbole";
        };
    }

    /** La documentation des mots-clés : une table, pas un aiguillage. */
    private static final Map<String, String> MOTS_CLES = Map.ofEntries(
            Map.entry("class", "déclaration de classe"),
            Map.entry("interface", "déclaration d'interface"),
            Map.entry("enum", "déclaration d'énumération"),
            Map.entry("record", "déclaration d'enregistrement"),
            Map.entry("public", "modificateur d'accès"),
            Map.entry("private", "modificateur d'accès"),
            Map.entry("protected", "modificateur d'accès"),
            Map.entry("static", "membre de la classe, non de l'instance"),
            Map.entry("final", "un seul lien, une seule valeur"),
            Map.entry("void", "méthode sans valeur de retour"),
            Map.entry("int", "type entier primitif"),
            Map.entry("long", "type entier primitif"),
            Map.entry("short", "type entier primitif"),
            Map.entry("byte", "type entier primitif"),
            Map.entry("boolean", "type primitif booléen"),
            Map.entry("char", "type primitif caractère"),
            Map.entry("float", "type à virgule flottante"),
            Map.entry("double", "type à virgule flottante"),
            Map.entry("if", "condition"),
            Map.entry("else", "condition"),
            Map.entry("for", "boucle"),
            Map.entry("while", "boucle"),
            Map.entry("do", "boucle"),
            Map.entry("switch", "aiguillage"),
            Map.entry("case", "aiguillage"),
            Map.entry("default", "aiguillage"),
            Map.entry("try", "surveillance d'exceptions"),
            Map.entry("catch", "surveillance d'exceptions"),
            Map.entry("finally", "surveillance d'exceptions"),
            Map.entry("throw", "exception lancée ou déclarée"),
            Map.entry("throws", "exception lancée ou déclarée"),
            Map.entry("return", "retour de méthode"),
            Map.entry("new", "instanciation"),
            Map.entry("this", "l'objet courant"),
            Map.entry("super", "la classe mère"),
            Map.entry("extends", "héritage"),
            Map.entry("implements", "réalisation d'interface"),
            Map.entry("import", "nom de paquet"),
            Map.entry("package", "nom de paquet"),
            Map.entry("var", "inférence de variable locale"));

    private static @Nullable String documentationMotCle(String mot) {
        return MOTS_CLES.get(mot);
    }
}
