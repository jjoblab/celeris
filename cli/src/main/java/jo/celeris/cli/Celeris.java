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
     * Le registre des langages du cli (M7.6) : les trente-deux paquets
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
     * @param output le flux de sortie des résultats
     * @return le code de sortie
     */
    public static int executer(String[] args, PrintStream output) {
        if (args.length == 0 || "version".equals(args[0])) {
            banniere(output);
            return 0;
        }
        return switch (args[0]) {
            case "analyse" -> analyser(args, output);
            case "recherche" -> search(args, output);
            case "diagnostiquer" -> diagnostiquer(args, output);
            case "formater" -> formater(args, output);
            case "refactorer" -> refactorer(args, output);
            default -> usage(output);
        };
    }

    private static void banniere(PrintStream output) {
        output.println("celeris " + VERSION);
        for (String module : modules()) {
            output.println("  " + module);
        }
    }

    private static int usage(PrintStream output) {
        output.println("usage : celeris [version | analyse <fichier> | "
                + "recherche <fichier> <motif> | diagnostiquer <fichier> | "
                + "formater <fichier> | refactorer <fichier> renommer "
                + "<ancien> <nouveau>]");
        return 2;
    }

    private static int analyser(String[] args, PrintStream output) {
        if (args.length != 2) {
            return usage(output);
        }
        String contenu = read(args[1], output);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport language = langageDe(args[1], contenu, registre());
        output.println("fichier : " + Path.of(args[1]).getFileName());
        output.println("langage : "
                + (language == null ? "aucun" : language.id()));
        output.println("lignes : " + visibleLines(document, contenu));
        output.println("caracteres : " + document.length());
        if (language == null || language.syntax() == null) {
            return 0;
        }
        Analysis analyse = SyntaxService.of(language.syntax()).analyseFull(document);
        for (FoldSpan region : folds(language, analyse, document)) {
            output.println("  pli : lignes " + (region.startLine() + 1) + "-"
                    + (region.endLine() + 1));
        }
        for (OutlineEntry input : plan(language, analyse, document)) {
            output.println("  plan : " + input.name() + " (" + input.category()
                    + ", ligne " + (input.startLine() + 1) + ")");
        }
        return 0;
    }

    /** Les plis du fichier : l'arbre, les commentaires, l'indentation déclarée (H-08). */
    private static List<FoldSpan> folds(LanguageSupport language, Analysis analyse,
            Document document) {
        LanguageConfiguration.Folding pliage = language.configuration().folding();
        if (!pliage.byIndentation()) {
            return TreeFolds.regions(document, analyse);
        }
        return TreeFolds.regions(document, analyse, new IndentationFolds.Settings(
                language.configuration().indentSize(), pliage.offSide()));
    }

    /** Le plan du fichier : les règles du paquet quand il en porte, l'arbre sinon (H-08). */
    private static List<OutlineEntry> plan(LanguageSupport language, Analysis analyse,
            Document document) {
        if (language.outlineRules().isEmpty()) {
            return TreeOutline.entries(document, analyse);
        }
        return RuleOutline.entries(document, language.outlineRules(),
                folds(language, analyse, document));
    }

    private static int search(String[] args, PrintStream output) {
        if (args.length != 3) {
            return usage(output);
        }
        String contenu = read(args[1], output);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        String pattern = args[2];
        if (pattern.isEmpty()) {
            // le motif vide s'égale à chaque position : la boucle ne
            // finirait jamais — le refus se dit et le code d'usage revient
            output.println("le motif de recherche ne peut pas être vide");
            return 2;
        }
        int total = 0;
        int position = contenu.indexOf(pattern);
        while (position >= 0) {
            int line = document.lineForOffset(position);
            int column = position - document.lineStart(line) + 1;
            output.println((line + 1) + ":" + column + " " + pattern);
            total++;
            position = contenu.indexOf(pattern, position + pattern.length());
        }
        output.println(total + " occurrence(s)");
        return 0;
    }

    /**
     * Diagnostique le fichier (G-03) : chaque plage malformée de l'arbre
     * se dit par sa ligne et sa colonne, à la manière de la recherche —
     * le décompte clôt.
     */
    private static int diagnostiquer(String[] args, PrintStream output) {
        if (args.length != 2) {
            return usage(output);
        }
        String contenu = read(args[1], output);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport language = langageDe(args[1], contenu, registre());
        int total = 0;
        if (language != null && language.syntax() != null) {
            Analysis analyse = SyntaxService.of(language.syntax()).analyseFull(document);
            for (SyntaxIssue issue : TreeDiagnostics.issues(document, analyse)) {
                int line = document.lineForOffset(issue.startOffset());
                int column = issue.startOffset() - document.lineStart(line) + 1;
                output.println((line + 1) + ":" + column + " " + issue.message());
                total++;
            }
        }
        output.println(total + " diagnostic(s)");
        return total == 0 ? 0 : 3;
    }

    /**
     * Formate le fichier (G-03) : le texte formaté paraît seul sur la
     * sortie — le résultat se dirige vers un fichier, quatre espaces
     * par niveau (le style de la maison).
     */
    private static int formater(String[] args, PrintStream output) {
        if (args.length != 2) {
            return usage(output);
        }
        String contenu = read(args[1], output);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport language = langageDe(args[1], contenu, registre());
        if (language == null || language.syntax() == null) {
            output.println("langage sans analyse : " + args[1]);
            return 1;
        }
        Analysis analyse = SyntaxService.of(language.syntax()).analyseFull(document);
        output.print(Formateur.formater(document, analyse, IndentStyle.ESPACES_4));
        return 0;
    }

    /**
     * Refactore le fichier (G-03) : renommer un symbole — le fichier
     * renommé paraît seul sur la sortie ; les conflits se signalent sans
     * se trancher (B-19), l'ambiguïté comme la collision, et le code 4
     * les dit.
     */
    private static int refactorer(String[] args, PrintStream output) {
        if (args.length != 5 || !"renommer".equals(args[2])) {
            return usage(output);
        }
        String contenu = read(args[1], output);
        if (contenu == null) {
            return 1;
        }
        Document document = Document.of(contenu);
        LanguageSupport language = langageDe(args[1], contenu, registre());
        if (language == null || language.syntax() == null) {
            output.println("langage sans analyse : " + args[1]);
            return 1;
        }
        Analysis analyse = SyntaxService.of(language.syntax()).analyseFull(document);
        IndexSymboles index = IndexSymboles.vide();
        index.remplacer(args[1], document, analyse);
        Renommage.PlanRenommage plan = Renommage.plan(index, args[3], args[4]);
        if (!plan.conflits().isEmpty()) {
            for (Renommage.ConflitRenommage conflit : plan.conflits()) {
                output.println("conflit : " + conflit.nom() + " " + conflit.reason());
            }
            return 4;
        }
        TextSession session = TextSession.of(contenu, System::nanoTime);
        List<TextEdit> edits = plan.edits().getOrDefault(args[1], List.of());
        if (!edits.isEmpty()) {
            session.apply(Transaction.of(edits));
        }
        output.print(session.text());
        return 0;
    }

    private static @Nullable String read(String chemin, PrintStream output) {
        try {
            return Files.readString(Path.of(chemin), StandardCharsets.UTF_8);
        } catch (IOException echec) {
            output.println("fichier illisible : " + chemin);
            return null;
        }
    }

    /**
     * Les lignes visibles : le saut de ligne final termine sa ligne,
     * il n'en ouvre pas une fantôme — la convention de l'éditeur.
     */
    private static int visibleLines(Document document, String contenu) {
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
