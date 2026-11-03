package jo.celeris.cli;

import java.io.PrintStream;
import java.util.List;

/**
 * Exécutable de celeris sans Android : annonce la version et les modules
 * du cœur. L'édition en mode terminal arrive avec les jalons suivants ;
 * ce squelette prouve que la chaîne complète se construit sans SDK.
 */
public final class Celeris {

    private static final String VERSION = "0.1.0-SNAPSHOT";

    private Celeris() {
    }

    /** Modules du cœur, dans l'ordre des dépendances descendantes. */
    public static List<String> modules() {
        return List.of(
                "core-text",
                "core-syntax",
                "core-language",
                "core-intelligence",
                "core-lsp",
                "core-ai");
    }

    /**
     * Point d'entrée : bannière, version, modules.
     *
     * @param args arguments de ligne de commande, ignores à ce jalon
     */
    public static void main(String[] args) {
        PrintStream sortie = System.out;
        sortie.println("celeris " + VERSION);
        for (String module : modules()) {
            sortie.println("  " + module);
        }
    }
}
