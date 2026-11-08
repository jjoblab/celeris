package jo.celeris.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.widgets.BadgeGenre;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.ErrorBandModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;
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
     * La documentation BRUTE d'un symbole — le markdown à clôtures,
     * tel que le serveur de référence le sert au survol (M7.5, §8).
     *
     * @param symbole le nom du symbole
     * @return la documentation markdown, null si la table ne connaît pas
     */
    static String documentationBrute(String symbole) {
        return DOCUMENTATION.get(symbole);
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
     * La complétion s'ouvre-t-elle à la frappe ? (M7.5, redressement —
     * le constat du propriétaire : aucune suggestion en tapant) : OUI
     * dès que le curseur termine un identifiant d'au moins un caractère
     * — comme tout éditeur, le premier caractère tapé propose ; le
     * blanc, la ponctuation et les déclencheurs à venir ne proposent
     * rien sur ce canal.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return vrai quand la liste se propose à la frappe
     */
    static boolean doitOuvrirALaFrappe(Document document, int caret) {
        String texte = document.text();
        int borne = Math.min(caret, texte.length());
        return borne > 0 && estIdentifiant(texte.charAt(borne - 1));
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
     * Les candidats TYPÉS du curseur (le design du dépôt de départ) :
     * chaque libellé gagne son genre de badge — mot réservé, méthode
     * documentée avec sa signature en identité qualifiée, identifiant
     * du document en variable — et « String » en classe.
     *
     * @param bruts les candidats libellés du concentrateur, déjà filtrés
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
     * Le préfixe tapé au curseur — les caractères qui matchent dans
     * les libellés passent à l'accent et au gras.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return le préfixe, vide hors de tout identifiant
     */
    static String prefixeCourant(Document document, int caret) {
        String texte = document.text();
        int borne = Math.min(caret, texte.length());
        return texte.substring(debutIdentifiant(texte, borne), borne);
    }

    /**
     * Les issues de syntaxe des diagnostics du fichier ouvert — la
     * bande d'erreurs s'y abreuve.
     *
     * @param diagnostics les diagnostics de l'hôte
     * @param nomFichier  le nom du fichier ouvert — le filtre s'y fait
     * @param document    le document ouvert (offsets de ligne)
     * @return les issues, dans l'ordre des diagnostics
     */
    static List<SyntaxIssue> issues(List<DiagnosticHote> diagnostics,
            String nomFichier, Document document) {
        List<SyntaxIssue> issues = new ArrayList<>();
        for (DiagnosticHote diagnostic : diagnostics(diagnostics, nomFichier)) {
            int ligne = Math.max(0, diagnostic.ligne() - 1);
            int debut = document.lineStart(ligne);
            issues.add(new SyntaxIssue(debut, 1, diagnostic.message()));
        }
        return issues;
    }

    /**
     * Les gravités des diagnostics du fichier ouvert, même ordre que
     * {@link #issues} — la bande colore ses marques à la gravité.
     *
     * @param diagnostics les diagnostics de l'hôte
     * @param nomFichier  le nom du fichier ouvert — le filtre s'y fait
     * @return les gravités (3 erreur, 2 avertissement, autre information)
     */
    static int[] gravites(List<DiagnosticHote> diagnostics, String nomFichier) {
        List<DiagnosticHote> duFichier = diagnostics(diagnostics, nomFichier);
        int[] gravites = new int[duFichier.size()];
        for (int i = 0; i < duFichier.size(); i++) {
            gravites[i] = duFichier.get(i).gravite()
                    == jo.celeris.android.host.spi.GraviteDiagnostic.ERREUR
                    ? ErrorBandModel.GRAVITE_ERREUR : 2;
        }
        return gravites;
    }

    private static List<DiagnosticHote> diagnostics(
            List<DiagnosticHote> diagnostics, String nomFichier) {
        List<DiagnosticHote> duFichier = new ArrayList<>();
        for (DiagnosticHote diagnostic : diagnostics) {
            if (diagnostic.chemin().nom().equals(nomFichier)) {
                duFichier.add(diagnostic);
            }
        }
        return duFichier;
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
