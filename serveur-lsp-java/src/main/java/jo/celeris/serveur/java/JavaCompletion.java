package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.MarkupContent;

/**
 * La complétion du mini serveur : les mots-clés du langage, puis les
 * symboles DÉCLARÉS dans le document — classes, méthodes, constructeurs
 * et champs — filtrés par le préfixe en cours de frappe.
 *
 * <p>L'ordre est déterministe : mots-clés d'abord, symboles ensuite,
 * chacun par ordre alphabétique — un même document rend toujours la
 * même liste. Chaque élément porte sa documentation en markdown : la
 * carte de complétion de l'éditeur s'y éprouve pour de vrai.</p>
 */
public final class JavaCompletion {

    /** Un candidat de complétion : libellé, sorte, détail, documentation. */
    private record Candidate(String label, CompletionItemKind kind,
            String detail, String documentation) {
    }

    private static final List<Candidate> KEYWORDS = List.of(
            mot("abstract", "modificateur de classe ou de méthode"),
            mot("assert", "assertion d'exécution"),
            mot("boolean", "type primitif booléen"),
            mot("break", "sortie de boucle ou d'aiguillage"),
            mot("byte", "type primitif entier court"),
            mot("case", "branche d'aiguillage"),
            mot("catch", "interception d'exception"),
            mot("char", "type primitif caractère"),
            mot("class", "déclaration de classe"),
            mot("const", "réservé, jamais utilisé"),
            mot("continue", "reprise de boucle"),
            mot("default", "branche par défaut, méthode d'interface"),
            mot("do", "boucle à condition finale"),
            mot("double", "type primitif à double précision"),
            mot("else", "branche alternative"),
            mot("enum", "déclaration d'énumération"),
            mot("extends", "héritage, borne de type"),
            mot("final", "un seul lien, une seule valeur"),
            mot("finally", "bloc toujours exécuté"),
            mot("float", "type primitif à simple précision"),
            mot("for", "boucle à compteur ou à collection"),
            mot("if", "condition"),
            mot("implements", "réalisation d'interface"),
            mot("import", "importation de nom"),
            mot("instanceof", "test de type"),
            mot("int", "type primitif entier"),
            mot("interface", "déclaration d'interface"),
            mot("long", "type primitif entier long"),
            mot("native", "implémentation hors machine"),
            mot("new", "instanciation"),
            mot("package", "déclaration de paquet"),
            mot("private", "membre de la classe seule"),
            mot("protected", "membre de la famille"),
            mot("public", "membre de tous"),
            mot("record", "déclaration d'enregistrement"),
            mot("return", "retour de méthode"),
            mot("short", "type primitif entier court"),
            mot("static", "membre de la classe"),
            mot("super", "la classe mère"),
            mot("switch", "aiguillage"),
            mot("synchronized", "verrou d'exclusion"),
            mot("this", "l'objet courant"),
            mot("throw", "lancement d'exception"),
            mot("throws", "exceptions déclarées"),
            mot("try", "bloc surveillé"),
            mot("var", "inférence de variable locale"),
            mot("void", "sans valeur de retour"),
            mot("volatile", "visibilité inter-fils"),
            mot("while", "boucle à condition initiale"));

    private JavaCompletion() {
    }

    /**
     * Les complétions d'une position : le préfixe en frappe filtre les
     * candidats (insensible à la casse) ; sans préfixe, tous se
     * proposent.
     *
     * @param source   le document suivi
     * @param symbols  les symboles déjà extraits du document
     * @param offset   le décalage du curseur
     * @return les éléments de complétion, ordonnés et figés
     */
    public static List<CompletionItem> at(JavaSource source,
            List<JavaSymbols.Symbol> symbols, int offset) {
        String prefixe = source.identifierPrefix(offset)
                .toLowerCase(java.util.Locale.ROOT);
        List<CompletionItem> elements = new ArrayList<>();
        for (Candidate candidat : KEYWORDS) {
            if (candidat.label().toLowerCase(java.util.Locale.ROOT)
                    .startsWith(prefixe)) {
                elements.add(element(candidat));
            }
        }
        for (Candidate candidat : candidatsDocument(symbols)) {
            if (candidat.label().toLowerCase(java.util.Locale.ROOT)
                    .startsWith(prefixe)) {
                elements.add(element(candidat));
            }
        }
        return List.copyOf(elements);
    }

    private static List<Candidate> candidatsDocument(
            List<JavaSymbols.Symbol> symbols) {
        List<Candidate> candidats = new ArrayList<>();
        for (JavaSymbols.Symbol symbole : symbols) {
            candidats.add(new Candidate(symbole.name(), sorteDe(
                    symbole.kind()), detailDe(symbole.kind()),
                    "Déclaré dans ce document : " + symbole.name() + "."));
            candidats.addAll(candidatsDocument(symbole.children()));
        }
        candidats.sort(java.util.Comparator.comparing(Candidate::label));
        return candidats;
    }

    private static CompletionItem element(Candidate candidat) {
        CompletionItem element = new CompletionItem();
        element.setLabel(candidat.label());
        element.setKind(candidat.kind());
        element.setDetail(candidat.detail());
        element.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN,
                candidat.documentation()));
        return element;
    }

    private static CompletionItemKind sorteDe(int kind) {
        return switch (kind) {
            case 5 -> CompletionItemKind.Class;
            case 6 -> CompletionItemKind.Method;
            case 8 -> CompletionItemKind.Field;
            case 9 -> CompletionItemKind.Constructor;
            case 10 -> CompletionItemKind.Enum;
            case 11 -> CompletionItemKind.Interface;
            case 23 -> CompletionItemKind.Struct;
            default -> CompletionItemKind.Text;
        };
    }

    private static String detailDe(int kind) {
        return switch (kind) {
            case 5 -> "classe déclarée dans le document";
            case 6 -> "méthode déclarée dans le document";
            case 8 -> "champ déclaré dans le document";
            case 9 -> "constructeur déclaré dans le document";
            case 10 -> "énumération déclarée dans le document";
            case 11 -> "interface déclarée dans le document";
            case 23 -> "enregistrement déclaré dans le document";
            default -> "symbole du document";
        };
    }

    private static Candidate mot(String label, String documentation) {
        return new Candidate(label, CompletionItemKind.Keyword,
                "mot-clé du langage", documentation + ".");
    }
}
