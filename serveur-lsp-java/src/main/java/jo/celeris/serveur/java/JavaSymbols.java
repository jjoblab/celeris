package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

/**
 * Les symboles du mini serveur : types déclarés (classes, interfaces,
 * énumérations, enregistrements), leurs méthodes, leurs constructeurs
 * et leurs champs — extraits du code dépouillé par des motifs
 * déterministes, avec leurs plages (le membre tout entier) et leurs
 * plages de sélection (le nom).
 *
 * <p>Les faux amis se neutralisent : les mots-clés d'en-têtes de blocs
 * ne déclarent pas de méthode, les noms précédés de {@code new} ou
 * d'un point ne déclarent rien. Les champs exigent un modificateur —
 * les variables locales, qui n'en portent jamais, restent dehors.</p>
 */
public final class JavaSymbols {

    /** Un symbole : nom, sorte du protocole, plages et enfants. */
    public record Symbol(String name, int kind, int start, int end,
            int nameStart, int nameEnd, List<Symbol> children) {

        /** Cherche un symbole par le nom, en largeur d'abord. */
        public boolean isNamed(String candidate) {
            return name.equals(candidate);
        }
    }

    /** Sortes du protocole : classe, interface, énumération, enregistrement. */
    private static final int KIND_CLASS = 5;

    private static final int KIND_METHOD = 6;

    private static final int KIND_FIELD = 8;

    private static final int KIND_CONSTRUCTOR = 9;

    private static final int KIND_ENUM = 10;

    private static final int KIND_INTERFACE = 11;

    private static final int KIND_RECORD = 23;

    private static final List<String> NOT_A_METHOD = List.of(
            "for", "if", "while", "switch", "catch", "synchronized",
            "new", "return", "throw", "yield", "do", "else", "try",
            "case", "default");

    private JavaSymbols() {
    }

    /**
     * Extrait les symboles d'un document : les types au premier rang,
     * les types imbriqués dedans, leurs membres au plus près.
     *
     * @param source le document suivi
     * @return les symboles du premier rang, éventuellement vides
     */
    public static List<Symbol> of(JavaSource source) {
        String texte = source.stripped().text();
        List<Symbol> types = types(source, texte);
        List<Symbol> membres = members(source, texte);
        return attache(types, membres);
    }

    private static List<Symbol> types(JavaSource source, String texte) {
        List<Symbol> types = new ArrayList<>();
        java.util.regex.Matcher motif = java.util.regex.Pattern
                .compile("\\b(class|interface|enum|record)\\s+"
                        + "([A-Za-z_$][A-Za-z0-9_$]*)")
                .matcher(texte);
        while (motif.find()) {
            String sorte = motif.group(1);
            String nom = motif.group(2);
            int corps = premiereOuvrante(texte, motif.end());
            int fin = corps >= 0 ? fermante(texte, corps) + 1 : texte.length();
            if (fin <= corps) {
                fin = texte.length();
            }
            types.add(new Symbol(nom, sorteDe(sorte), debutLigne(source,
                    texte, motif.start()), fin, motif.start(2),
                    motif.end(2), new ArrayList<>()));
        }
        return types;
    }

    private static List<Symbol> members(JavaSource source, String texte) {
        List<Symbol> membres = new ArrayList<>();
        java.util.regex.Matcher motif = java.util.regex.Pattern
                .compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\("
                        + "((?:[^()]|\\([^()]*\\))*)\\)"
                        + "\\s*(?:throws\\s+[A-Za-z0-9_.,\\s]+)?\\{")
                .matcher(texte);
        while (motif.find()) {
            String nom = motif.group(1);
            if (NOT_A_METHOD.contains(nom) || precedePointOuNew(texte,
                    motif.start(1))) {
                continue;
            }
            int ouvrante = texte.indexOf('{', motif.end() - 1);
            int fin = fermante(texte, ouvrante) + 1;
            if (fin <= ouvrante) {
                fin = texte.length();
            }
            membres.add(new Symbol(nom, KIND_METHOD, debutLigne(source,
                    texte, motif.start()), fin, motif.start(1),
                    motif.end(1), List.of()));
        }
        membres.addAll(fields(texte));
        return membres;
    }

    /** Les champs : une déclaration MUNIE d'un modificateur, finie. */
    private static List<Symbol> fields(String texte) {
        List<Symbol> champs = new ArrayList<>();
        java.util.regex.Matcher motif = java.util.regex.Pattern
                .compile("(?m)^[ \\t]*(?:(?:public|private|protected|"
                        + "static|final|volatile|transient)\\s+)+"
                        + "[A-Za-z_$][A-Za-z0-9_$.]*(?:<[^;=]*>)?"
                        + "(?:\\[\\])*\\s+([A-Za-z_$][A-Za-z0-9_$]*)"
                        + "\\s*(?:=[^;\\n]*)?;")
                .matcher(texte);
        while (motif.find()) {
            champs.add(new Symbol(motif.group(1), KIND_FIELD,
                    motif.start(), motif.end(), motif.start(1),
                    motif.end(1), List.of()));
        }
        return champs;
    }

    /**
     * Les types s'imbriquent d'abord (le plus intime gagne — par
     * INDICE, jamais par identité), puis les membres rejoignent le
     * type qui les contient, au plus près.
     */
    private static List<Symbol> attache(List<Symbol> types,
            List<Symbol> membres) {
        List<Symbol> racines = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            Symbol hote = null;
            for (int j = 0; j < types.size(); j++) {
                if (j == i) {
                    continue;
                }
                Symbol autre = types.get(j);
                if (autre.start() <= types.get(i).start()
                        && types.get(i).end() <= autre.end()
                        && (hote == null || autre.start() >= hote.start())) {
                    hote = autre;
                }
            }
            if (hote == null) {
                racines.add(types.get(i));
            } else {
                hote.children().add(types.get(i));
            }
        }
        for (Symbol membre : membres) {
            Symbol hote = conteneur(racines, membre);
            if (hote != null) {
                if (membre.kind() == KIND_METHOD
                        && membre.name().equals(hote.name())) {
                    membre = new Symbol(membre.name(), KIND_CONSTRUCTOR,
                            membre.start(), membre.end(), membre.nameStart(),
                            membre.nameEnd(), membre.children());
                }
                hote.children().add(membre);
            }
        }
        return racines.stream().map(JavaSymbols::figer).toList();
    }

    /** Fige un symbole et sa descendance : listes immuables. */
    private static Symbol figer(Symbol symbole) {
        List<Symbol> enfants = new ArrayList<>();
        for (Symbol enfant : symbole.children()) {
            enfants.add(figer(enfant));
        }
        return new Symbol(symbole.name(), symbole.kind(), symbole.start(),
                symbole.end(), symbole.nameStart(), symbole.nameEnd(),
                List.copyOf(enfants));
    }

    /** Le type le plus intime qui contient la cible. */
    private static @org.jspecify.annotations.Nullable Symbol conteneur(
            List<Symbol> types, Symbol cible) {
        Symbol hote = null;
        for (Symbol type : types) {
            if (type.start() <= cible.start()
                    && cible.end() <= type.end()) {
                Symbol imbrique = conteneur(type.children(), cible);
                if (imbrique != null) {
                    return imbrique;
                }
                if (hote == null || type.start() >= hote.start()) {
                    hote = type;
                }
            }
        }
        return hote;
    }

    /** La première accolade ouvrante à partir d'un décalage. */
    private static int premiereOuvrante(String texte, int depuis) {
        return texte.indexOf('{', depuis);
    }

    /** La fermante qui apparie l'ouvrante donnée, ou -1. */
    private static int fermante(String texte, int ouvrante) {
        int profondeur = 0;
        for (int i = ouvrante; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '{') {
                profondeur++;
            } else if (c == '}') {
                profondeur--;
                if (profondeur == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Le début du code de la ligne (hors retrait) d'un décalage. */
    private static int debutLigne(JavaSource source, String texte,
            int offset) {
        int ligne = source.lineOf(offset);
        int debut = source.lineStart(ligne);
        while (debut < texte.length()
                && (texte.charAt(debut) == ' ' || texte.charAt(debut) == '\t')) {
            debut++;
        }
        return debut;
    }

    private static boolean precedePointOuNew(String texte, int nom) {
        int avant = nom - 1;
        while (avant >= 0 && Character.isWhitespace(texte.charAt(avant))) {
            avant--;
        }
        if (avant < 0) {
            return false;
        }
        if (texte.charAt(avant) == '.' || texte.charAt(avant) == ':') {
            return true;
        }
        if (Character.isJavaIdentifierPart(texte.charAt(avant))) {
            int debut = avant;
            while (debut > 0
                    && Character.isJavaIdentifierPart(
                    texte.charAt(debut - 1))) {
                debut--;
            }
            return texte.startsWith("new", debut);
        }
        return false;
    }

    private static int sorteDe(String sorte) {
        return switch (sorte) {
            case "interface" -> KIND_INTERFACE;
            case "enum" -> KIND_ENUM;
            case "record" -> KIND_RECORD;
            default -> KIND_CLASS;
        };
    }
}
