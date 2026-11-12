package jo.celeris.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import java.util.ServiceLoader;

import jo.celeris.core.intelligence.Formateur;
import jo.celeris.core.intelligence.IndexSymboles;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.Renommage;
import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.LanguageContributor;
import jo.celeris.core.language.LanguageDetection;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.RuleOutline;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.structure.TreeDiagnostics;
import jo.celeris.core.syntax.structure.TreeFolds;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.syntax.grammars.PackGrammars;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Exécutable de celeris sans Android : l'éditeur en ligne de commande
 * (M7) — analyser un fichier (langage détecté, lignes, plan) et
 * chercher un motif (occurrences aux lignes et colonnes exactes).
 * La chaîne outillée s'y ajoute (G-03) : diagnostiquer (les plages
 * malformées de l'arbre, une par ligne), formater (le texte formaté,
 * seul sur la sortie — le résultat se dirige vers un fichier) et
 * refactorer renommer (le fichier renommé sur la sortie, les conflits
 * signalés sans être tranchés). La bannière sans argument reste la
 * fumée de la CI (critère de sortie M1).
 *
 * <p>Codes de sortie : 0 succès, 1 fichier illisible ou langage sans
 * analyse, 2 usage inconnu, 3 diagnostics trouvés, 4 conflit de
 * renommage. La sortie est déterministe : mêmes arguments, mêmes
 * lignes — les bancs et la CI s'y appuient (G-03).</p>
 */
public final class Celeris {

    /** La version affichée — celle de la sortie courante du dépôt (build racine). */
    private static final String VERSION = "1.1.0";

    private Celeris() {
    }

    /**
     * Le registre des langages du cli (M7.6) : les vingt-deux paquets
     * intégrés assemblés par la plateforme — les analyseurs écrits
     * s'y attachent par préférence, la voie de grammaires compile les
     * autres — et les contributeurs découverts par ServiceLoader
     * (le mécanisme réservé à la JVM, sans réflexion ailleurs).
     *
     * @return le registre assemblé
     */
    private static LanguageRegistry registre() {
        LanguagePlatform plateforme = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks();
        for (LanguageContributor contributeur
                : ServiceLoader.load(LanguageContributor.class)) {
            plateforme.contributor(contributeur);
        }
        return plateforme.build();
    }

    /** Modules du cœur, dans l'ordre des dépendances descendantes. */
    public static List<String> modules() {
        return List.of(
                "core-text",
                "core-syntax",
                "core-syntax-grammars",
                "core-language",
                "core-intelligence",
                "core-lsp",
                "core-ai");
    }

    /**
     * Point d'entrée : sans argument, la bannière ; sinon la commande.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        System.exit(executer(args, System.out));
    }

    /**
     * Exécute la commande et rend le code de sortie : 0 succès, 1
     * fichier illisible, 2 usage inconnu.
     *
     * @param args   les arguments, premier terme = la commande
     * @param sortie le flux de sortie des résultats
     * @return le code de sortie
     */
    public static int executer(String[] args, PrintStream sortie) {
        if (args.length == 0 || "version".equals(args[0])) {
            banniere(sortie);
            return 0;
        }
        return switch (args[0]) {
            case "analyse" -> analyser(args, sortie);
            case "recherche" -> chercher(args, sortie);
            case "diagnostiquer" -> diagnostiquer(args, sortie);
            case "formater" -> formater(args, sortie);
            case "refactorer" -> refactorer(args, sortie);
            default -> usage(sortie);
        };
    }

    private static void banniere(PrintStream sortie) {
        sortie.println("celeris " + VERSION);
        for (String module : modules()) {
            sortie.println("  " + module);
        }
    }

    private static int usage(PrintStream sortie) {
        sortie.println("usage : celeris [version | analyse <fichier> | "
                + "recherche <fichier> <motif> | diagnostiquer <fichier> | "
                + "formater <fichier> | refactorer <fichier> renommer "
                + "<ancien> <nouveau>]");
        return 2;
    }

    private static int analyser(String[] args, PrintStream sortie) {
        if (args.length != 2) {
            return usage(sortie);
        }
        String contenu = lire(args[1], sortie);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport langue = langageDe(args[1], contenu, registre());
        sortie.println("fichier : " + Path.of(args[1]).getFileName());
        sortie.println("langage : "
                + (langue == null ? "aucun" : langue.id()));
        sortie.println("lignes : " + lignesVisibles(document, contenu));
        sortie.println("caracteres : " + document.length());
        if (langue == null || langue.syntax() == null) {
            return 0;
        }
        Analysis analyse = SyntaxService.of(langue.syntax()).analyseFull(document);
        for (FoldSpan region : plis(langue, analyse, document)) {
            sortie.println("  pli : lignes " + (region.startLine() + 1) + "-"
                    + (region.endLine() + 1));
        }
        for (OutlineEntry entree : plan(langue, analyse, document)) {
            sortie.println("  plan : " + entree.name() + " (" + entree.category()
                    + ", ligne " + (entree.startLine() + 1) + ")");
        }
        return 0;
    }

    /** Les plis du fichier : l'arbre, les commentaires, l'indentation déclarée (H-08). */
    private static List<FoldSpan> plis(LanguageSupport langue, Analysis analyse,
            Document document) {
        LanguageConfiguration.Folding pliage = langue.configuration().folding();
        if (!pliage.byIndentation()) {
            return TreeFolds.regions(document, analyse);
        }
        return TreeFolds.regions(document, analyse, new IndentationFolds.Settings(
                langue.configuration().indentSize(), pliage.offSide()));
    }

    /** Le plan du fichier : les règles du paquet quand il en porte, l'arbre sinon (H-08). */
    private static List<OutlineEntry> plan(LanguageSupport langue, Analysis analyse,
            Document document) {
        if (langue.outlineRules().isEmpty()) {
            return TreeOutline.entries(document, analyse);
        }
        return RuleOutline.entries(document, langue.outlineRules(),
                plis(langue, analyse, document));
    }

    private static int chercher(String[] args, PrintStream sortie) {
        if (args.length != 3) {
            return usage(sortie);
        }
        String contenu = lire(args[1], sortie);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        String motif = args[2];
        if (motif.isEmpty()) {
            // le motif vide s'égale à chaque position : la boucle ne
            // finirait jamais — le refus se dit et le code d'usage revient
            sortie.println("le motif de recherche ne peut pas être vide");
            return 2;
        }
        int total = 0;
        int position = contenu.indexOf(motif);
        while (position >= 0) {
            int ligne = document.lineForOffset(position);
            int colonne = position - document.lineStart(ligne) + 1;
            sortie.println((ligne + 1) + ":" + colonne + " " + motif);
            total++;
            position = contenu.indexOf(motif, position + motif.length());
        }
        sortie.println(total + " occurrence(s)");
        return 0;
    }

    /**
     * Diagnostique le fichier (G-03) : chaque plage malformée de l'arbre
     * se dit par sa ligne et sa colonne, à la manière de la recherche —
     * le décompte clôt.
     */
    private static int diagnostiquer(String[] args, PrintStream sortie) {
        if (args.length != 2) {
            return usage(sortie);
        }
        String contenu = lire(args[1], sortie);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport langue = langageDe(args[1], contenu, registre());
        int total = 0;
        if (langue != null && langue.syntax() != null) {
            Analysis analyse = SyntaxService.of(langue.syntax()).analyseFull(document);
            for (SyntaxIssue issue : TreeDiagnostics.issues(document, analyse)) {
                int ligne = document.lineForOffset(issue.startOffset());
                int colonne = issue.startOffset() - document.lineStart(ligne) + 1;
                sortie.println((ligne + 1) + ":" + colonne + " " + issue.message());
                total++;
            }
        }
        sortie.println(total + " diagnostic(s)");
        return total == 0 ? 0 : 3;
    }

    /**
     * Formate le fichier (G-03) : le texte formaté paraît seul sur la
     * sortie — le résultat se dirige vers un fichier, quatre espaces
     * par niveau (le style de la maison).
     */
    private static int formater(String[] args, PrintStream sortie) {
        if (args.length != 2) {
            return usage(sortie);
        }
        String contenu = lire(args[1], sortie);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport langue = langageDe(args[1], contenu, registre());
        if (langue == null || langue.syntax() == null) {
            sortie.println("langage sans analyse : " + args[1]);
            return 1;
        }
        Analysis analyse = SyntaxService.of(langue.syntax()).analyseFull(document);
        sortie.print(Formateur.formater(document, analyse, IndentStyle.ESPACES_4));
        return 0;
    }

    /**
     * Refactore le fichier (G-03) : renommer un symbole — le fichier
     * renommé paraît seul sur la sortie ; les conflits se signalent sans
     * se trancher (B-19), l'ambiguïté comme la collision, et le code 4
     * les dit.
     */
    private static int refactorer(String[] args, PrintStream sortie) {
        if (args.length != 5 || !"renommer".equals(args[2])) {
            return usage(sortie);
        }
        String contenu = lire(args[1], sortie);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport langue = langageDe(args[1], contenu, registre());
        if (langue == null || langue.syntax() == null) {
            sortie.println("langage sans analyse : " + args[1]);
            return 1;
        }
        Analysis analyse = SyntaxService.of(langue.syntax()).analyseFull(document);
        IndexSymboles index = IndexSymboles.vide();
        index.remplacer(args[1], document, analyse);
        Renommage.PlanRenommage plan = Renommage.plan(index, args[3], args[4]);
        if (!plan.conflits().isEmpty()) {
            for (Renommage.ConflitRenommage conflit : plan.conflits()) {
                sortie.println("conflit : " + conflit.nom() + " " + conflit.raison());
            }
            return 4;
        }
        TextSession session = TextSession.of(contenu, System::nanoTime);
        List<TextEdit> editions = plan.editions().getOrDefault(args[1], List.of());
        if (!editions.isEmpty()) {
            session.apply(Transaction.of(editions));
        }
        sortie.print(session.text());
        return 0;
    }

    private static @Nullable String lire(String chemin, PrintStream sortie) {
        try {
            return Files.readString(Path.of(chemin), StandardCharsets.UTF_8);
        } catch (IOException echec) {
            sortie.println("fichier illisible : " + chemin);
            return null;
        }
    }

    /**
     * Les lignes visibles : le saut de ligne final termine sa ligne,
     * il n'en ouvre pas une fantôme — la convention de l'éditeur.
     */
    private static int lignesVisibles(Document document, String contenu) {
        if (contenu.isEmpty() || !contenu.endsWith("\n")) {
            return document.lineCount();
        }
        return Math.max(1, document.lineCount() - 1);
    }

    /**
     * Le langage d'un fichier par la détection du registre — nom,
     * première ligne et échantillon compris (le {@code .h} de C ou C++
     * se départage par son contenu), jamais par une table d'extensions.
     */
    private static @Nullable LanguageSupport langageDe(String chemin,
            String contenu, LanguageRegistry registre) {
        String nom = Path.of(chemin).getFileName().toString();
        int saut = contenu.indexOf('\n');
        String premiereLigne = saut < 0 ? contenu : contenu.substring(0, saut);
        LanguageDetection detection = registre.detect(nom, premiereLigne,
                contenu.length() > 8192 ? contenu.substring(0, 8192) : contenu);
        return detection instanceof LanguageDetection.Resolved resolu
                ? resolu.support()
                : null;
    }
}
