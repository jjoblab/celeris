package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.GraviteDiagnostic;
import jo.celeris.android.widgets.BadgeGenre;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.android.widgets.ErrorBandModel;
import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.ListeCompletion;

import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Les décisions de contenu GÉNÉRIQUES des popups de l'éditeur (le
 * déménagement du 2026-11-14 : les algorithmes qu'aucune table ne
 * porte) : QUI s'ouvre et QUOI — le symbole à l'offset, le préfixe
 * tapé, la complétion des mots du document, les candidats typés de la
 * réponse du protocole, le chemin du fil d'Ariane dans le plan, les
 * issues et gravités de la bande d'erreurs. Pur et testé sans
 * appareil : les TABLES de contenu (mots réservés, signatures connues,
 * documentation) restent chez l'hôte — {@link CoachPopups} les porte.
 */
public final class ContenuPopups {

    /** Le nombre maximal de candidats montrés par la liste. */
    public static final int MAX_CANDIDATS = 8;

    /** Le genre de badge par genre du protocole (CompletionItemKind)
     *  — la table remplace les branches, le hors-table est variable. */
    private static final Map<Integer, BadgeGenre.Genre> GENRES = Map.ofEntries(
            Map.entry(2, BadgeGenre.Genre.METHODE),
            Map.entry(3, BadgeGenre.Genre.METHODE),
            Map.entry(4, BadgeGenre.Genre.METHODE),
            Map.entry(5, BadgeGenre.Genre.CHAMP),
            Map.entry(10, BadgeGenre.Genre.CHAMP),
            Map.entry(21, BadgeGenre.Genre.CHAMP),
            Map.entry(7, BadgeGenre.Genre.CLASSE),
            Map.entry(23, BadgeGenre.Genre.CLASSE),
            Map.entry(25, BadgeGenre.Genre.CLASSE),
            Map.entry(8, BadgeGenre.Genre.INTERFACE),
            Map.entry(13, BadgeGenre.Genre.ENUM),
            Map.entry(20, BadgeGenre.Genre.ENUM),
            Map.entry(9, BadgeGenre.Genre.PAQUET),
            Map.entry(17, BadgeGenre.Genre.PAQUET),
            Map.entry(19, BadgeGenre.Genre.PAQUET),
            Map.entry(14, BadgeGenre.Genre.MOT_CLE),
            Map.entry(15, BadgeGenre.Genre.EXTRAIT));

    /** Le constructeur des décisions — purement statique, il reste muet. */
    private ContenuPopups() {
    }

    /**
     * Le symbole à l'offset — l'identifiant qui le porte, null au
     * vide entre deux mots.
     *
     * @param document le document ouvert
     * @param offset   la position visitée
     * @return l'identifiant complet, null si aucun
     */
    public static @Nullable String symboleA(Document document, int offset) {
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
     * Le DÉBUT du mot qui finit au caret — l'acceptation d'un candidat
     * y remplace le mot en entier.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return l'offset du début du mot, le caret sans mot
     */
    public static int debutMot(Document document, int caret) {
        String symbole = symboleA(document, caret);
        return symbole == null ? caret : caret - symbole.length();
    }

    /**
     * Le serveur passe-t-il PREMIER pour la complétion ? Vrai quand il
     * vit ET la sert — sa réponse brute deviendra les candidats typés ;
     * sinon le repli local (les mots du code, le coach) propose.
     *
     * @param serveur la session de langage branchée, null pour aucune
     * @return vrai quand la complétion passe par le serveur
     */
    public static boolean leServeurPassePremier(@Nullable LanguageSession serveur) {
        return serveur != null && serveur.sertCompletion();
    }

    /**
     * La complétion se rouvre-t-elle APRÈS une édition ? La PORTE des
     * réglages peut la rendre silencieuse (l'audit du propriétaire du
     * 2026-11-14 : « activer, désactiver certaines fonctionnalités »)
     * et l'acceptation d'un candidat ne repropose rien — le candidat
     * retenu ne se repropose pas ; sinon la frappe propose comme tout
     * éditeur.
     *
     * @param completionAuto la porte des réglages, ouverte ou fermée
     * @param acceptation    vrai quand l'édition accepte un candidat
     * @param document       le document ouvert
     * @param caret          la position du curseur
     * @return vrai quand la liste se propose
     */
    public static boolean doitProposerApresEdition(boolean completionAuto,
            boolean acceptation, Document document, int caret) {
        return completionAuto && !acceptation
                && doitOuvrirALaFrappe(document, caret);
    }

    /**
     * La complétion s'ouvre-t-elle à la frappe ? OUI dès que le
     * curseur termine un identifiant d'au moins un caractère — comme
     * tout éditeur, le premier caractère tapé propose ; le blanc, la
     * ponctuation et les déclencheurs à venir ne proposent rien sur
     * ce canal.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return vrai quand la liste se propose à la frappe
     */
    public static boolean doitOuvrirALaFrappe(Document document, int caret) {
        String texte = document.text();
        int borne = Math.min(caret, texte.length());
        return borne > 0 && estIdentifiant(texte.charAt(borne - 1));
    }

    /**
     * La complétion des mots du document : les identifiants et les
     * mots réservés de l'hôte, filtrés par le préfixe tapé — le mot
     * sous le curseur ne se propose pas à lui-même.
     *
     * @param document      le document ouvert
     * @param caret         la position du curseur
     * @param motsReserves  les mots réservés du langage, servis par
     *                      l'hôte ({@link CoachPopups#motsReserves()})
     * @return les candidats distincts, triés sans casse, capés
     */
    public static List<String> completionMots(Document document, int caret,
            List<String> motsReserves) {
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
        for (String mot : motsReserves) {
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
     * Le préfixe tapé au curseur — les caractères qui matchent dans
     * les libellés passent à l'accent et au gras.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return le préfixe, vide hors de tout identifiant
     */
    public static String prefixeCourant(Document document, int caret) {
        String texte = document.text();
        int borne = Math.min(caret, texte.length());
        return texte.substring(debutIdentifiant(texte, borne), borne);
    }

    /**
     * Les candidats TYPÉS du serveur : la réponse brute du protocole
     * gagne ses genres de badge — le détail sert d'identité qualifiée,
     * la première ligne de documentation sinon — capés au plafond.
     *
     * @param reponse la liste de complétion du protocole, null sûre
     * @return les candidats typés, même ordre que le serveur
     */
    public static List<CompletionListView.Candidat> candidats(
            @Nullable ListeCompletion reponse) {
        if (reponse == null) {
            return List.of();
        }
        List<CompletionListView.Candidat> types = new ArrayList<>();
        for (ElementCompletion element : reponse.elements()) {
            if (types.size() >= MAX_CANDIDATS) {
                break;
            }
            types.add(new CompletionListView.Candidat(element.libelle(),
                    identiteDe(element), genreDe(element.genre())));
        }
        return types;
    }

    /**
     * La documentation d'un élément du serveur — le modèle du flanc
     * de la liste, null sans documentation.
     *
     * @param reponse la liste de complétion du protocole, null sûre
     * @param index   l'index de l'élément documenté
     * @return le modèle de documentation, null si l'élément n'en dit rien
     */
    public static @Nullable CompletionDocModel docDe(
            @Nullable ListeCompletion reponse, int index) {
        if (reponse == null || index < 0
                || index >= reponse.elements().size()) {
            return null;
        }
        ElementCompletion element = reponse.elements().get(index);
        return element.documentation() == null ? null
                : CompletionDocModel.of(element.libelle(),
                        element.documentation());
    }

    /**
     * Le fil d'Ariane : les entrées du plan qui englobent le curseur,
     * de la plus externe à la plus interne.
     *
     * @param entrees le plan du fichier ouvert
     * @param caret   la position du curseur
     * @return les noms du chemin, vide hors de tout symbole
     */
    public static List<String> pathFor(List<OutlineEntry> entrees, int caret) {
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

    /**
     * Les issues de syntaxe des diagnostics du fichier ouvert — la
     * bande d'erreurs s'y abreuve.
     *
     * @param diagnostics les diagnostics de l'hôte
     * @param nomFichier  le nom du fichier ouvert — le filtre s'y fait
     * @param document    le document ouvert (offsets de ligne)
     * @return les issues, dans l'ordre des diagnostics
     */
    /**
     * Les problèmes du LIEU (l'audit Android Studio du 2026-11-14 :
     * la fiche de l'IDE de référence empile TOUTES les signatures de
     * la position — jamais une bulle par diagnostic) : les diagnostics
     * du fichier ouvert portés par la ligne, chacun son message et sa
     * sévérité.
     *
     * @param diagnostics les diagnostics de l'hôte
     * @param nomFichier  le nom du fichier ouvert — le filtre s'y fait
     * @param ligne       la ligne visitée, à partir de zéro
     * @return les problèmes de la ligne, vides sans diagnostic
     */
    public static List<DiagnosticTooltipView.Probleme> problemesDe(
            List<DiagnosticHote> diagnostics, String nomFichier, int ligne) {
        List<DiagnosticTooltipView.Probleme> problemes = new ArrayList<>();
        for (DiagnosticHote diagnostic : diagnostics) {
            if (diagnostic.ligne() == ligne + 1
                    && diagnostic.chemin().nom().equals(nomFichier)) {
                problemes.add(new DiagnosticTooltipView.Probleme(
                        diagnostic.message(),
                        diagnostic.gravite() == GraviteDiagnostic.ERREUR
                                ? DiagnosticTooltipView.Severity.ERROR
                                : DiagnosticTooltipView.Severity.WARNING));
            }
        }
        return problemes;
    }

    public static List<SyntaxIssue> issues(List<DiagnosticHote> diagnostics,
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
    public static int[] gravites(List<DiagnosticHote> diagnostics,
            String nomFichier) {
        List<DiagnosticHote> duFichier = diagnostics(diagnostics, nomFichier);
        int[] gravites = new int[duFichier.size()];
        for (int i = 0; i < duFichier.size(); i++) {
            gravites[i] = duFichier.get(i).gravite()
                    == GraviteDiagnostic.ERREUR
                    ? ErrorBandModel.GRAVITE_ERREUR : 2;
        }
        return gravites;
    }

    /** L'identité qualifiée d'un élément du serveur : son détail, la
     *  première ligne de sa documentation sinon, vide sans rien. */
    private static String identiteDe(ElementCompletion element) {
        if (element.detail() != null && !element.detail().isEmpty()) {
            return element.detail();
        }
        if (element.documentation() != null) {
            String premiere = element.documentation().strip();
            int finLigne = premiere.indexOf('\n');
            return finLigne < 0 ? premiere
                    : premiere.substring(0, finLigne).strip();
        }
        return "";
    }

    /** Le genre de badge du protocole (CompletionItemKind) au
     *  vocabulaire du kit — le hors-table reste variable. */
    private static BadgeGenre.Genre genreDe(int kind) {
        return GENRES.getOrDefault(kind, BadgeGenre.Genre.VARIABLE);
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
