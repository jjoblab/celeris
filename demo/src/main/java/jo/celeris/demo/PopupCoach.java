package jo.celeris.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.Document;

/**
 * Les décisions de contenu des popups de la démonstration (M7.5, §7,
 * reprise) : QUI s'ouvre et avec QUOI — la complétion lit les
 * identifiants du document et les mots réservés, la signature cherche
 * l'appel ouvert sous le curseur dans le serveur factice, la
 * documentation et le survol servent la même table, le fil d'Ariane
 * descend le plan. Pur et testé sans appareil : l'atelier n'y ajoute
 * que les vues et les ancres.
 */
final class PopupCoach {

    /** Le nombre maximal de candidats montrés par la liste. */
    static final int MAX_CANDIDATS = 8;

    /** Les mots réservés du terrain de démonstration (Java). */
    private static final List<String> MOTS_RESERVES = List.of(
            "boolean", "final", "float", "int", "public", "private",
            "return", "static", "String", "void");

    /** Une signature du serveur factice : libellé et paramètres. */
    private record SignatureConnue(String libelle, List<String> parametres) {
    }

    /** Le serveur factice : les fonctions documentées du terrain. */
    private static final Map<String, SignatureConnue> SIGNATURES = Map.of(
            "ajouter", new SignatureConnue("ajouter(int a, int b)",
                    List.of("a", "b")),
            "mesurer", new SignatureConnue(
                    "mesurer(String unite, float valeur, boolean arrondir)",
                    List.of("unite", "valeur", "arrondir")),
            "concatener", new SignatureConnue(
                    "concatener(String premier, String second)",
                    List.of("premier", "second")));

    /** La documentation du serveur factice, en Markdown à clôtures. */
    private static final Map<String, String> DOCUMENTATION = Map.of(
            "ajouter", "Additionne deux entiers et rend la somme.\n\n"
                    + "@param a le premier terme\n@param b le second terme",
            "mesurer", "Mesure une valeur dans son unité.\n\n"
                    + "@param unite l'unité de mesure\n"
                    + "@param valeur la valeur mesurée\n"
                    + "@param arrondir vrai pour arrondir au cran",
            "concatener", "Joint deux chaînes bout à bout.\n\n"
                    + "@param premier la tête\n@param second la queue");

    /** Le constructeur du coach — purement statique, il reste muet. */
    private PopupCoach() {
    }

    /**
     * Les mots réservés du terrain de démonstration — la complétion
     * y joint les mots du code du fichier ouvert.
     *
     * @return les mots réservés servis à la complétion
     */
    static List<String> motsReserves() {
        return MOTS_RESERVES;
    }

    /**
     * La complétion du curseur : les identifiants du document et les
     * mots réservés, filtrés par le préfixe tapé — le mot sous le
     * curseur ne se propose pas à lui-même.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return les candidats distincts, triés sans casse, capés
     */
    static List<String> completionFor(Document document, int caret) {
        String texte = document.text();
        int debutMot = debutIdentifiant(texte, caret);
        String prefixe = texte.substring(debutMot, caret);
        if (prefixe.isEmpty()) {
            return List.of();
        }
        Map<String, Boolean> vus = new LinkedHashMap<>();
        for (String nom : identifiers(texte)) {
            ajouter(vus, nom, prefixe);
        }
        for (String mot : MOTS_RESERVES) {
            ajouter(vus, mot, prefixe);
        }
        List<String> candidats = new ArrayList<>(vus.keySet());
        candidats.sort((a, b) -> {
            int sansCasse = a.toLowerCase(Locale.ROOT)
                    .compareTo(b.toLowerCase(Locale.ROOT));
            return sansCasse != 0 ? sansCasse : a.compareTo(b);
        });
        return candidats.size() > MAX_CANDIDATS
                ? List.copyOf(candidats.subList(0, MAX_CANDIDATS))
                : List.copyOf(candidats);
    }

    /**
     * La signature de l'appel ouvert sous le curseur : la fonction
     * connue la plus proche, l'index du paramètre courant compté à
     * plat — null hors de tout appel connu.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return le modèle d'aide aux paramètres, null sans appel ouvert
     */
    static SignatureAideModel signatureFor(Document document, int caret) {
        String texte = document.text();
        int profondeur = 0;
        int ouverture = -1;
        for (int i = caret - 1; i >= 0; i--) {
            char c = texte.charAt(i);
            if (c == ')') {
                profondeur++;
            } else if (c == '(') {
                if (profondeur == 0) {
                    ouverture = i;
                    break;
                }
                profondeur--;
            }
        }
        if (ouverture <= 0) {
            return null;
        }
        String nom = texte.substring(debutIdentifiant(texte, ouverture),
                ouverture);
        SignatureConnue connue = SIGNATURES.get(nom);
        if (connue == null) {
            return null;
        }
        int index = 0;
        int niveau = 0;
        for (int i = ouverture + 1; i < caret; i++) {
            char c = texte.charAt(i);
            if (c == '(' || c == '[') {
                niveau++;
            } else if (c == ')' || c == ']') {
                niveau--;
            } else if (c == ',' && niveau == 0) {
                index++;
            }
        }
        return SignatureAideModel.of(connue.libelle(),
                connue.parametres(), index);
    }

    /**
     * La documentation d'un symbole — la table du serveur factice.
     *
     * @param symbole le nom du symbole
     * @return la documentation structurée, null si le serveur ne connaît pas
     */
    static CompletionDocModel docFor(String symbole) {
        String doc = DOCUMENTATION.get(symbole);
        return doc == null ? null : CompletionDocModel.of(symbole, doc);
    }

    /**
     * Le fil d'Ariane : les entrées du plan qui englobent le curseur,
     * de la plus externe à la plus interne.
     *
     * @param entrees le plan du fichier ouvert
     * @param caret   la position du curseur
     * @return les noms du chemin, vide hors de tout symbole
     */
    static List<String> pathFor(List<OutlineEntry> entrees, int caret) {
        List<OutlineEntry> contenantes = new ArrayList<>();
        for (OutlineEntry entree : entrees) {
            if (entree.startOffset() <= caret && caret < entree.endOffset()) {
                contenantes.add(entree);
            }
        }
        contenantes.sort((a, b) -> Integer.compare(
                b.endOffset() - b.startOffset(),
                a.endOffset() - a.startOffset()));
        List<String> noms = new ArrayList<>();
        for (OutlineEntry entree : contenantes) {
            noms.add(entree.name());
        }
        return List.copyOf(noms);
    }

    private static void ajouter(Map<String, Boolean> vus, String nom,
            String prefixe) {
        if (nom.length() >= prefixe.length()
                && nom.regionMatches(true, 0, prefixe, 0, prefixe.length())) {
            vus.putIfAbsent(nom, Boolean.TRUE);
        }
    }

    /** Le début de l'identifiant qui porte l'offset donné. */
    private static int debutIdentifiant(String texte, int offset) {
        int i = Math.min(offset, texte.length());
        while (i > 0 && estIdentifiant(texte.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /** Les identifiants du texte, dans l'ordre du document. */
    private static List<String> identifiers(String texte) {
        List<String> noms = new ArrayList<>();
        int i = 0;
        while (i < texte.length()) {
            if (estIdentifiant(texte.charAt(i))) {
                int debut = i;
                while (i < texte.length() && estIdentifiant(texte.charAt(i))) {
                    i++;
                }
                noms.add(texte.substring(debut, i));
            } else {
                i++;
            }
        }
        return noms;
    }

    private static boolean estIdentifiant(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
