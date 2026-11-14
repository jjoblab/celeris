package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jo.celeris.android.host.popups.ContenuPopups;
import jo.celeris.android.widgets.BadgeGenre;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.SignatureAideModel;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Les tables du terrain de démonstration (le déménagement des popups,
 * décision du propriétaire du 2026-11-14 : les algorithmes génériques
 * vivent désormais dans {@link ContenuPopups} chez l'éditeur, le coach
 * ne porte plus que le CONTENU DE TEST) : les mots réservés Java, les
 * signatures et la documentation du serveur factice — le contenu que
 * la démo sert aux popups pour les éprouver. Pur et testé sans
 * appareil.
 */
final class PopupCoach {

    /** Le plafond de candidats — la décision vit chez l'éditeur. */
    static final int MAX_CANDIDATS = ContenuPopups.MAX_CANDIDATS;

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
     * La complétion du terrain : les mots du document et les mots
     * réservés de la table — l'algorithme vit chez l'éditeur.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return les candidats distincts, triés sans casse, capés
     */
    static List<String> completionFor(Document document, int caret) {
        return ContenuPopups.completionMots(document, caret, MOTS_RESERVES);
    }

    /**
     * Les candidats TYPÉS du terrain : chaque libellé gagne son genre
     * de badge — mot réservé, méthode documentée avec sa signature en
     * identité qualifiée, identifiant du document en variable — et
     * « String » en classe.
     *
     * @param bruts les candidats libellés, déjà filtrés
     * @return les candidats typés, même ordre
     */
    static List<CompletionListView.Candidat> candidats(List<String> bruts) {
        List<CompletionListView.Candidat> types = new ArrayList<>(bruts.size());
        for (String nom : bruts) {
            SignatureConnue signature = SIGNATURES.get(nom);
            if (signature != null) {
                types.add(new CompletionListView.Candidat(nom,
                        signature.libelle(), BadgeGenre.Genre.METHODE));
            } else if ("String".equals(nom)) {
                types.add(new CompletionListView.Candidat(nom, "",
                        BadgeGenre.Genre.CLASSE));
            } else if (MOTS_RESERVES.contains(nom)) {
                types.add(new CompletionListView.Candidat(nom, "",
                        BadgeGenre.Genre.MOT_CLE));
            } else {
                types.add(new CompletionListView.Candidat(nom, "",
                        BadgeGenre.Genre.VARIABLE));
            }
        }
        return types;
    }

    /**
     * La documentation BRUTE d'un symbole — le markdown à clôtures,
     * tel que le serveur de référence le sert au survol.
     *
     * @param symbole le nom du symbole
     * @return la documentation markdown, null si la table ne connaît pas
     */
    static @Nullable String documentationBrute(String symbole) {
        return DOCUMENTATION.get(symbole);
    }

    /**
     * La documentation d'un symbole — la table du serveur factice.
     *
     * @param symbole le nom du symbole
     * @return la documentation structurée, null si le serveur ne connaît pas
     */
    static @Nullable CompletionDocModel docFor(
            String symbole) {
        String doc = DOCUMENTATION.get(symbole);
        return doc == null ? null
                : CompletionDocModel.of(symbole, doc);
    }

    /**
     * Le symbole à l'offset — l'identifiant qui le porte, null au
     * vide entre deux mots.
     *
     * @param document le document ouvert
     * @param offset   la position visitée
     * @return l'identifiant complet, null si aucun
     */
    static String symboleA(Document document, int offset) {
        String texte = document.text();
        int borne = Math.min(offset, texte.length());
        int debut = debutIdentifiant(texte, borne);
        int fin = borne;
        while (fin < texte.length() && estIdentifiant(texte.charAt(fin))) {
            fin++;
        }
        return debut == fin ? null : texte.substring(debut, fin);
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
    static @Nullable SignatureAideModel signatureFor(
            Document document, int caret) {
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
        return SignatureAideModel.of(
                connue.libelle(), connue.parametres(), index);
    }

    /** Le début de l'identifiant qui finit à l'offset donné — la
     *  boucle inverse du coach du terrain. */
    private static int debutIdentifiant(String texte, int offset) {
        int i = Math.min(offset, texte.length());
        while (i > 0 && estIdentifiant(texte.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static boolean estIdentifiant(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
