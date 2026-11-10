package jo.celeris.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le paquet jouet de l'utilisateur (H-04, M9) : un langage ENTIER — le
 * Logo de la tortue, huit commandes et cinq couleurs — écrit en données
 * seules ({@code pack.json}, {@code language.json}, {@code grammar.json},
 * {@code outline.json}, {@code snippets.json}). Les réglages de la
 * démonstration le copient au stockage ({@code paquets/logo/}) ou
 * l'en retirent ; le registre le charge à chaud par le dossier des
 * paquets ({@link jo.celeris.core.language.Packs#folder}) — ajouter un
 * langage ne demande AUCUNE recompilation, c'est cela que la
 * démonstration démontre.
 *
 * <p>Le contenu vit ICI, en constantes : les réglages l'écrivent au
 * stockage comme un utilisateur le ferait, le banc des vérifications le
 * joue dans un dossier temporaire — une seule source de vérité, deux
 * terrains.</p>
 */
public final class PaquetJouet {

    /** L'identifiant du langage du paquet jouet. */
    public static final String LANGAGE = "logo";

    /** Le dossier des paquets de l'utilisateur, sous les fichiers de l'hôte. */
    public static final String DOSSIER = "paquets";

    /** Les fichiers du paquet, par leur nom — la donnée pure du langage. */
    private static final Map<String, String> FICHIERS = fichiers();

    private PaquetJouet() {
    }

    /** Le paquet en cartes : le nom du fichier et son contenu JSON. */
    static Map<String, String> contenu() {
        return FICHIERS;
    }

    /**
     * Vrai quand le paquet jouet vit au dossier des paquets : les
     * trois fichiers qui portent un langage y sont ({@code pack.json},
     * {@code language.json}, {@code grammar.json}).
     *
     * @param racine le dossier des paquets de l'utilisateur
     * @return vrai si le paquet est installé
     */
    public static boolean installe(Path racine) {
        Path logo = racine.resolve(LANGAGE);
        return Files.isRegularFile(logo.resolve("pack.json"))
                && Files.isRegularFile(logo.resolve("language.json"))
                && Files.isRegularFile(logo.resolve("grammar.json"));
    }

    /**
     * Copie le paquet jouet au dossier des paquets : les cinq fichiers
     * écrits tels quels — c'est la main de l'utilisateur, jouée par les
     * réglages de la démonstration.
     *
     * @param racine le dossier des paquets de l'utilisateur
     * @throws IOException l'écriture a échoué
     */
    public static void installer(Path racine) throws IOException {
        Path logo = racine.resolve(LANGAGE);
        Files.createDirectories(logo);
        for (Map.Entry<String, String> fichier : FICHIERS.entrySet()) {
            // L'écriture par octets UTF-8 : Files.writeString n'arrive
            // qu'au niveau 33, la démonstration vise le 26.
            Files.write(logo.resolve(fichier.getKey()),
                    fichier.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Retire le paquet jouet du dossier des paquets — le dossier ENTIER
     * du langage disparaît, le registre ne le verra plus au rechargement.
     *
     * @param racine le dossier des paquets de l'utilisateur
     * @throws IOException la suppression a échoué
     */
    public static void retirer(Path racine) throws IOException {
        Path logo = racine.resolve(LANGAGE);
        if (!Files.isDirectory(logo)) {
            return;
        }
        try (var entrees = Files.list(logo)) {
            for (Path entree : entrees.toList()) {
                Files.deleteIfExists(entree);
            }
        }
        Files.deleteIfExists(logo);
    }


    /** Le fichier pack.json du paquet jouet. */
    private static final String PACK = """
                {
                  "id": "logo",
                  "version": "1.0.0",
                  "schemaVersion": 1,
                  "license": "Apache-2.0",
                  "provenance": "paquet jouet — démonstration celeris (M9, H-04)"
                }
                """;
    /** Le fichier language.json du paquet jouet. */
    private static final String LANGUE = """
                {
                  "id": "logo",
                  "name": "Logo",
                  "extensions": [".lgo"],
                  "badge": "Lg",
                  "badgeColor": "#43A047",
                  "syntax": { "preferred": "grammar" },
                  "firstLine": "^;\\\\s*logo",
                  "comments": { "lineComment": "; " },
                  "brackets": [
                    ["[", "]"],
                    ["(", ")"]
                  ],
                  "autoClosingPairs": [
                    { "open": "[", "close": "]", "notIn": ["string"] },
                    { "open": "(", "close": ")", "notIn": ["string"] }
                  ],
                  "surroundingPairs": [
                    ["[", "]"],
                    ["(", ")"]
                  ],
                  "folding": { "byIndentation": true, "offSide": false }
                }
                """;
    /** Le fichier grammar.json du paquet jouet. */
    private static final String GRAMMAIRE = """
                {
                  "name": "Logo",
                  "scopeName": "source.logo",
                  "patterns": [
                    { "include": "#commentaires" },
                    { "include": "#motsCles" },
                    { "include": "#couleurs" },
                    { "include": "#nombres" },
                    { "include": "#ponctuation" }
                  ],
                  "repository": {
                    "commentaires": {
                      "patterns": [
                        { "match": ";.*$", "name": "comment.line.semicolon.logo" }
                      ]
                    },
                    "motsCles": {
                      "patterns": [
                        {
                          "match": "\\\\b(pour|fin|repete|avance|recule|tourne|baisse|leve|couleur)\\\\b",
                          "name": "keyword.control.logo"
                        }
                      ]
                    },
                    "couleurs": {
                      "patterns": [
                        {
                          "match": "\\\\b(rouge|vert|bleu|noir|blanc)\\\\b",
                          "name": "constant.language.color.logo"
                        }
                      ]
                    },
                    "nombres": {
                      "patterns": [
                        { "match": "\\\\b[0-9]+\\\\b", "name": "constant.numeric.logo" }
                      ]
                    },
                    "ponctuation": {
                      "patterns": [
                        {
                          "match": "[\\\\[\\\\]()]",
                          "name": "punctuation.section.brackets.logo"
                        }
                      ]
                    }
                  }
                }
                """;
    /** Le fichier outline.json du paquet jouet. */
    private static final String PLAN = """
                {
                  "rules": [
                    {
                      "pattern": "^\\\\s*pour\\\\s+([a-z0-9-]+)",
                      "category": "function",
                      "name": 1
                    }
                  ]
                }
                """;
    /** Le fichier snippets.json du paquet jouet. */
    private static final String GABARITS = """
                {
                  "carre": {
                    "prefix": "carre",
                    "body": ["repete 4 [", "  avance $1", "  tourne 90", "]", "$0"],
                    "description": "le carre de la tortue"
                  },
                  "boucle": {
                    "prefix": "rep",
                    "body": ["repete $1 [", "  $0", "]"],
                    "description": "la repetition de la tortue"
                  }
                }
                """;

    /** Les fichiers du paquet, dans l'ordre de lecture. */
    private static Map<String, String> fichiers() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put("pack.json", PACK);
        fichiers.put("language.json", LANGUE);
        fichiers.put("grammar.json", GRAMMAIRE);
        fichiers.put("outline.json", PLAN);
        fichiers.put("snippets.json", GABARITS);
        return fichiers;
    }

    /**
     * La liste des noms de fichiers du paquet — pour les tests et le
     * banc : le paquet se joue sur n'importe quel dossier.
     *
     * @return les noms, non modifiables
     */
    static List<String> noms() {
        return List.copyOf(FICHIERS.keySet());
    }
}
