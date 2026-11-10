package jo.celeris.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import java.util.ServiceLoader;

import jo.celeris.core.language.LanguageConfiguration;
import jo.celeris.core.language.LanguageContributor;
import jo.celeris.core.language.LanguageDetection;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.IndentationFolds;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.RuleOutline;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.syntax.grammars.PackGrammars;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Exécutable de celeris sans Android : l'éditeur en ligne de commande
 * (M7) — analyser un fichier (langage détecté, lignes, plan) et
 * chercher un motif (occurrences aux lignes et colonnes exactes). La
 * bannière sans argument reste la fumée de la CI (critère de sortie
 * M1) ; l'édition interactive viendra des jalons suivants.
 */
public final class Celeris {

    /** La version affichée — celle de la sortie courante du dépôt (build racine). */
    private static final String VERSION = "1.0.0";

    private Celeris() {
    }

    /**
     * Le registre des langages du cli (M7.6) : les seize paquets
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
                + "recherche <fichier> <motif>]");
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
