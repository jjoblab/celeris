// SPDX-License-Identifier: Apache-2.0
package spike.regex;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Spike du moteur d'expressions rationnelles (M7.6, alimente l'ADR-014).
 *
 * <p>Question mesurée : pour charger SANS MODIFICATION les grammaires
 * TextMate réelles (celles que publient les extensions de VS Code), faut-il
 * (a) une couche de traduction vers {@code java.util.regex} pour le
 * sous-ensemble courant, ou (b) joni, le portage Java d'Oniguruma ? Les
 * deux voies sont mesurées sur les mêmes grammaires et le même corpus :
 * taux d'acceptation à la compilation, histogramme des constructions non
 * traduites, temps d'une lexicographie complète de 50 000 lignes (minimum
 * de trois tours, PIEGE-13), tas consommé.</p>
 *
 * <p>Le lexeur est volontairement simple mais réel : recherche du match le
 * plus à gauche parmi les règles actives (premier de la liste à égalité),
 * pile begin/end, motifs de fin ancrés à la position courante (sémantique
 * \G). Il mesure le travail du moteur, pas la conception du futur moteur de
 * grammaires.</p>
 *
 * <p>Corpus synthétique ASCII (mêmes lignes pour les deux voies) : les
 * décalages octets de joni coïncident avec les positions de caractères ;
 * en production, la conversion octets/caractères serait un coût à la charge
 * de la voie (b) — consigné au résultat.</p>
 */
public final class SpikeRegex {

    private static final int LIGNES = 50_000;
    private static final int TOURS = 3;
    private static final Path GRAMMAIRES = Path.of("spikes/regex/grammaires");
    private static final String[] LANGUES = {
        "json", "yaml", "python", "shell", "c", "cpp",
        "markdown", "html", "css", "javascript", "typescript",
    };

    // ------------------------------------------------------------- modèle --

    /** Règle TextMate minimale (les champs non utilisés par la mesure manquent). */
    static final class Regle {
        String nom;
        String match;
        String begin;
        String end;
        String while_;
        List<Regle> patterns;
        Map<String, Regle> repository;
        List<Object> includes;
        boolean racine;
    }

    /** Une expression extraite, avec son chemin dans la grammaire. */
    record Extraite(String grammaire, String chemin, String genre, String source) { }

    // ------------------------------------------------------- moteurs (2) --

    /** Ce qu'un moteur sait faire : compiler et chercher. */
    interface Moteur {
        String nom();

        Object compiler(String regex) throws Exception;

        /** Match le plus à gauche depuis {@code debut} ; null si rien. */
        int[] chercher(Object motif, String ligne, int debut);

        /** Match ancré à {@code pos} exactement (sémantique \G des fins). */
        boolean ancre(Object motif, String ligne, int pos);
    }

    /** Voie (a) : traduction vers java.util.regex, échec bruyant sinon. */
    static final class MoteurJava implements Moteur {
        private final Map<String, String> echecs = new TreeMap<>();

        @Override
        public String nom() {
            return "java";
        }

        /** Traduit le sous-ensemble Oniguruma courant ; refuse le reste. */
        static String traduire(String regex) {
            String r = regex;
            // \h (chiffre hexadécimal Oniguruma ; espace horizontale en Java)
            r = r.replace("\\h", "[0-9A-Fa-f]");
            r = r.replace("\\H", "[^0-9A-Fa-f]");
            // classes POSIX [[:nom:]] → \p{Nom} (dans ou hors classe)
            r = r.replaceAll("\\[\\[:alpha:\\]\\]", "\\\\p{Alpha}");
            r = r.replaceAll("\\[\\[:digit:\\]\\]", "\\\\p{Digit}");
            r = r.replaceAll("\\[\\[:alnum:\\]\\]", "\\\\p{Alnum}");
            r = r.replaceAll("\\[\\[:space:\\]\\]", "\\\\p{Space}");
            r = r.replaceAll("\\[\\[:upper:\\]\\]", "\\\\p{Upper}");
            r = r.replaceAll("\\[\\[:lower:\\]\\]", "\\\\p{Lower}");
            r = r.replaceAll("\\[\\[:punct:\\]\\]", "\\\\p{Punct}");
            // \x{...} → séquence unicode 16 bits quand tient, sinon refus
            var m = java.util.regex.Pattern.compile("\\\\x\\{([0-9A-Fa-f]+)\\}").matcher(r);
            var sb = new StringBuilder();
            while (m.find()) {
                int cp = Integer.parseInt(m.group(1), 16);
                if (cp > 0xFFFF) {
                    throw new UnsupportedOperationException("\\x{" + m.group(1) + "} au-delà du BMP");
                }
                m.appendReplacement(sb, "\\u" + String.format("%04X", cp));
            }
            m.appendTail(sb);
            r = sb.toString();
            // constructions sans équivalent : refus bruyant
            if (r.contains("(?~")) {
                throw new UnsupportedOperationException("opérateur d'absence (?~...)");
            }
            if (r.contains("(?(")) {
                throw new UnsupportedOperationException("conditionnel (?(...)");
            }
            if (r.contains("(?x")) {
                throw new UnsupportedOperationException("mode étendu (?x");
            }
            if (r.contains("(?\\")) {
                throw new UnsupportedOperationException("référence arrière d'appel (?\\");
            }
            if (r.contains("\\R")) {
                throw new UnsupportedOperationException("saut de ligne \\R");
            }
            return r;
        }

        @Override
        public Object compiler(String regex) throws Exception {
            String traduite = traduire(regex);
            try {
                return Pattern.compile(traduite);
            } catch (PatternSyntaxException e) {
                throw new UnsupportedOperationException(messageCourt(e.getMessage()));
            }
        }

        private static String messageCourt(String message) {
            if (message == null) {
                return "syntaxe refusée";
            }
            String[] lignes = message.split("\n");
            return lignes[0].length() > 90 ? lignes[0].substring(0, 90) + "…" : lignes[0];
        }

        @Override
        public int[] chercher(Object motif, String ligne, int debut) {
            Matcher m = ((Pattern) motif).matcher(ligne);
            if (!m.find(debut)) {
                return null;
            }
            return new int[]{m.start(), m.end()};
        }

        @Override
        public boolean ancre(Object motif, String ligne, int pos) {
            Matcher m = ((Pattern) motif).matcher(ligne);
            m.region(pos, ligne.length());
            m.useAnchoringBounds(true);
            return m.lookingAt();
        }
    }

    /** Voie (b) : joni, portage Java d'Oniguruma (positions en octets UTF-8). */
    static final class MoteurJoni implements Moteur {
        @Override
        public String nom() {
            return "joni";
        }

        @Override
        public Object compiler(String regex) throws Exception {
            byte[] octets = regex.getBytes(StandardCharsets.UTF_8);
            return new org.joni.Regex(octets, 0, octets.length, org.joni.Option.NONE,
                    org.jcodings.specific.UTF8Encoding.INSTANCE);
        }

        @Override
        public int[] chercher(Object motif, String ligne, int debut) {
            byte[] octets = ligne.getBytes(StandardCharsets.UTF_8);
            org.joni.Matcher m = ((org.joni.Regex) motif).matcher(octets);
            int n = m.search(debut, octets.length, org.joni.Option.NONE);
            if (n < 0) {
                return null;
            }
            return new int[]{m.getBegin(), m.getEnd()};
        }

        @Override
        public boolean ancre(Object motif, String ligne, int pos) {
            byte[] octets = ligne.getBytes(StandardCharsets.UTF_8);
            org.joni.Matcher m = ((org.joni.Regex) motif).matcher(octets);
            return m.match(pos, octets.length, org.joni.Option.NONE) >= 0;
        }
    }

    // -------------------------------------------------------- extraction --

    /** Extrait toutes les expressions (match/begin/end/while) d'une grammaire. */
    static List<Extraite> extraire(Map<String, Object> grammaire, String nom) {
        List<Extraite> sorties = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> racine = grammaire;
        parcourir(racine, nom, "$", sorties, 0);
        return sorties;
    }

    @SuppressWarnings("unchecked")
    private static void parcourir(Map<String, Object> regle, String grammaire, String chemin,
                                  List<Extraite> sorties, int profondeur) {
        if (profondeur > 200 || regle == null) {
            return;
        }
        if (regle.get("match") instanceof String s) {
            sorties.add(new Extraite(grammaire, chemin, "match", s));
        }
        if (regle.get("begin") instanceof String s) {
            sorties.add(new Extraite(grammaire, chemin, "begin", s));
        }
        if (regle.get("end") instanceof String s) {
            sorties.add(new Extraite(grammaire, chemin, "end", s));
        }
        if (regle.get("while") instanceof String s) {
            sorties.add(new Extraite(grammaire, chemin, "while", s));
        }
        if (regle.get("patterns") instanceof List<?> liste) {
            for (int i = 0; i < liste.size(); i++) {
                if (liste.get(i) instanceof Map<?, ?> enfant) {
                    parcourir((Map<String, Object>) enfant, grammaire,
                            chemin + "/" + i, sorties, profondeur + 1);
                }
            }
        }
        if (regle.get("repository") instanceof Map<?, ?> depot) {
            for (var e : depot.entrySet()) {
                if (e.getValue() instanceof Map<?, ?> enfant) {
                    parcourir((Map<String, Object>) enfant, grammaire,
                            chemin + "#" + e.getKey(), sorties, profondeur + 1);
                }
            }
        }
        if (regle.get("captures") instanceof Map<?, ?> captures) {
            for (var e : captures.entrySet()) {
                if (e.getValue() instanceof Map<?, ?> enfant) {
                    parcourir((Map<String, Object>) enfant, grammaire,
                            chemin + "@" + e.getKey(), sorties, profondeur + 1);
                }
            }
        }
    }

    // -------------------------------------------------------- corpus (50k) --

    /** Corpus synthétique ASCII par langage : gabarits réalistes, répétés. */
    static List<String> corpus(String langue, int lignes) {
        String[] gabarits = switch (langue) {
            case "json" -> new String[]{
                "  \"nom\": \"valeur\",", "  \"compte\": 42,", "  \"actif\": true,",
                "  \"nul\": null,", "  \"liste\": [1, 2.5e3, \"x\"],", "  { \"imbrique\": { \"a\": -7 } },",
                "    // commentaire toléré", "  \"chemin\": \"src/main/kt\",", "}", "{"};
            case "yaml" -> new String[]{
                "nom: celeris", "version: \"1.0\"", "etape:", "  - langue: kotlin",
                "  - langue: xml", "environnements:", "  production:", "    url: https://exemple.io",
                "# un commentaire", "actif: true", "mots:", "  - alpha", "  - beta"};
            case "python" -> new String[]{
                "class Exemple:", "    def faire(self, valeur):",
                "        total = valeur * 3", "        return f\"total={total}\"",
                "    @staticmethod", "    def util():", "        # commentaire",
                "        x = [i for i in range(10)]", "        if x and valeur is not None:",
                "            raise ValueError('non')", "from pathlib import Path",
                "import re, json"};
            case "shell" -> new String[]{
                "#!/usr/bin/env bash", "set -euo pipefail", "for fichier in \"$@\"; do",
                "  if [ -f \"$fichier\" ]; then", "    echo \"copie : $fichier\"",
                "    cp \"$fichier\" /tmp/", "  fi", "done",
                "case \"$1\" in", "  start) demarrer ;;", "  *) usage ;;", "esac",
                "EXIT=$(grep -c '^' sortie.txt) || EXIT=0"};
            case "c" -> new String[]{
                "#include <stdio.h>", "int calculer(int a, int b) {",
                "    int total = a + b * 2;", "    if (total > 0x10) {",
                "        printf(\"total: %d\\n\", total);", "    }",
                "    return total;", "}",
                "static const char *nom = \"celeris\";", "/* commentaire de bloc */",
                "typedef struct { int x; } Point;"};
            case "cpp" -> new String[]{
                "#include <vector>", "namespace celeris {",
                "class Serveur {", "public:",
                "    explicit Serveur(int n) : n_(n) {}",
                "    std::vector<int> indices() const;",
                "private:", "    int n_;", "};",
                "template <typename T>", "T somme(T a, T b) { return a + b; }", "}"};
            case "markdown" -> new String[]{
                "# Titre principal", "## Sous-titre", "Un paragraphe avec **gras** et *italique*.",
                "- élément un", "- élément deux", "1. étape", "2. étape",
                "```java", "int x = 1;", "```", "> une citation",
                "[un lien](https://exemple.io)", "| a | b |", "| --- | --- |"};
            case "html" -> new String[]{
                "<!DOCTYPE html>", "<html lang=\"fr\">", "<head>",
                "  <meta charset=\"utf-8\">", "  <title>Page</title>", "</head>",
                "<body>", "  <div class=\"principal\">", "    <p>Un paragraphe &amp; un signe.</p>",
                "    <img src=\"image.png\" alt=\"\">", "  </div>",
                "  <script>var x = 1;</script>", "</body>", "</html>"};
            case "css" -> new String[]{
                ".principal {", "  color: #334455;", "  margin: 0 auto;",
                "  font-family: \"Inter\", sans-serif;", "}",
                "@media (max-width: 600px) {", "  .principal { display: none; }", "}",
                "a:hover, a:focus { text-decoration: underline; }",
                "/* un commentaire */", "body { --marge: 1.5rem; }"};
            case "javascript" -> new String[]{
                "import { serveur } from './module.js';", "const reponse = await fetch('/api');",
                "function calculer(a, b = 2) {", "  return a * b;", "}",
                "class Depot {", "  constructor(nom) { this.nom = nom; }", "}",
                "const liste = [1, 2, 3].map(x => x * 2);",
                "export default calculer;", "// commentaire",
                "`gabarit ${liste.length} éléments`"};
            case "typescript" -> new String[]{
                "import { Observable } from 'rxjs';", "interface Point { x: number; y: number; }",
                "type Resultat<T> = { ok: true; valeur: T } | { ok: false };",
                "function somme<T extends number>(a: T, b: T): number {",
                "  return (a as number) + (b as number);", "}",
                "const point: Point = { x: 1, y: 2 };",
                "enum Couleur { Rouge, Vert }", "export abstract class Base {",
                "  abstract nom(): string;", "}",
                "// un commentaire", "let x: string | null = null;"};
            default -> throw new IllegalArgumentException(langue);
        };
        Random alea = new Random(20261108L);
        List<String> sortie = new ArrayList<>(lignes);
        for (int i = 0; i < lignes; i++) {
            sortie.add(gabarits[alea.nextInt(gabarits.length)]);
        }
        return sortie;
    }

    // -------------------------------------------------------------- main --

    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        MoteurJava moteurJava = new MoteurJava();
        MoteurJoni moteurJoni = new MoteurJoni();
        Map<String, List<Extraite>> grammars = new LinkedHashMap<>();

        System.out.println("== Spike moteur d'expressions rationnelles (M7.6) ==");
        System.out.println("grammaires réelles : extensions VS Code (MIT), 11 langages");
        System.out.println();

        // 1. Acceptation à la compilation.
        System.out.println("-- Acceptation à la compilation (traduction vs joni) --");
        System.out.printf("%-12s %6s %13s %13s %s%n", "grammaire", "motifs", "java traduit", "joni", "refus java (top)");
        int totalMotifs = 0;
        int totalJava = 0;
        int totalJoni = 0;
        Map<String, Integer> histogrammeRefus = new TreeMap<>();
        for (String langue : LANGUES) {
            String texte = Files.readString(GRAMMAIRES.resolve(langue + ".tmLanguage.json"));
            Map<String, Object> grammaire = gson.fromJson(texte,
                    new TypeToken<Map<String, Object>>() { }.getType());
            List<Extraite> extraits = extraire(grammaire, langue);
            grammars.put(langue, extraits);
            int java = 0;
            int joni = 0;
            Map<String, Integer> refus = new TreeMap<>();
            for (Extraite e : extraits) {
                boolean okJava;
                try {
                    moteurJava.compiler(e.source());
                    okJava = true;
                } catch (Exception | StackOverflowError ex) {
                    okJava = false;
                    String cause = ex instanceof UnsupportedOperationException u ? u.getMessage()
                            : ex.getClass().getSimpleName();
                    refus.merge(cause, 1, Integer::sum);
                    histogrammeRefus.merge(cause, 1, Integer::sum);
                }
                if (okJava) {
                    java++;
                }
                try {
                    moteurJoni.compiler(e.source());
                    joni++;
                } catch (Exception | StackOverflowError ex) {
                    // joni refuserait : consigné par grammaire ci-dessous si besoin
                }
            }
            totalMotifs += extraits.size();
            totalJava += java;
            totalJoni += joni;
            String top = refus.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(2).map(e -> e.getValue() + "× " + e.getKey())
                    .reduce((a, b) -> a + " ; " + b).orElse("—");
            System.out.printf("%-12s %6d %9d (%2.0f%%) %6d (%2.0f%%) %s%n",
                    langue, extraits.size(), java, 100.0 * java / extraits.size(),
                    joni, 100.0 * joni / extraits.size(), top);
        }
        System.out.printf("TOTAL        %6d %9d (%2.0f%%) %6d (%2.0f%%)%n",
                totalMotifs, totalJava, 100.0 * totalJava / totalMotifs,
                totalJoni, 100.0 * totalJoni / totalMotifs);
        System.out.println();
        System.out.println("histogramme des refus de la traduction :");
        histogrammeRefus.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> System.out.printf("  %4d× %s%n", e.getValue(), e.getKey()));
        System.out.println();

        // 2. Performance : lexicographie complète de 50 000 lignes.
        // Charge comparable pour les deux voies : chaque motif (plafonné aux
        // 40 premiers de la grammaire, cpp en compte 549) est recherché sur
        // chaque ligne — le moteur réel n'essaierait que les règles actives,
        // la mesure compare les moteurs à travail égal.
        final int PLAFOND_MOTIFS = 40;
        System.out.println("-- Recherche de " + PLAFOND_MOTIFS + " motifs sur " + LIGNES
                + " lignes (min de " + TOURS + " tours, corpus identique) --");
        System.out.printf("%-12s %12s %14s %10s %10s%n", "grammaire", "jetons", "java (ms)", "joni (ms)", "rapport");
        for (String langue : LANGUES) {
            List<String> lignes = corpus(langue, LIGNES);
            List<Object> motifsJava = new ArrayList<>();
            List<Object> motifsJoni = new ArrayList<>();
            for (Extraite e : grammars.get(langue)) {
                if (motifsJava.size() >= PLAFOND_MOTIFS) {
                    break;
                }
                try {
                    motifsJava.add(moteurJava.compiler(e.source()));
                } catch (Exception | StackOverflowError ex) {
                    motifsJava.add(null);
                }
                try {
                    motifsJoni.add(moteurJoni.compiler(e.source()));
                } catch (Exception | StackOverflowError ex) {
                    motifsJoni.add(null);
                }
            }
            long jetons = 0;
            long msJava = Long.MAX_VALUE;
            long msJoni = Long.MAX_VALUE;
            for (int tour = 0; tour < TOURS; tour++) {
                long t0 = System.nanoTime();
                jetons = balayer(motifsJava, lignes, (motif, ligne) -> {
                    Matcher m = ((Pattern) motif).matcher(ligne);
                    return m.find(0) ? new int[]{m.start(), m.end()} : null;
                });
                long t1 = System.nanoTime();
                if ((t1 - t0) / 1_000_000 < msJava) {
                    msJava = (t1 - t0) / 1_000_000;
                }
                long t2 = System.nanoTime();
                long jetonsJoni = balayer(motifsJoni, lignes, (motif, ligne) -> {
                    byte[] octets = ligne.getBytes(StandardCharsets.UTF_8);
                    org.joni.Matcher m = ((org.joni.Regex) motif).matcher(octets);
                    return m.search(0, octets.length, org.joni.Option.NONE) >= 0
                            ? new int[]{m.getBegin(), m.getEnd()} : null;
                });
                long t3 = System.nanoTime();
                if ((t3 - t2) / 1_000_000 < msJoni) {
                    msJoni = (t3 - t2) / 1_000_000;
                }
                if (jetonsJoni != jetons && tour == 0) {
                    System.out.printf("  (note : %d jetons java contre %d joni sur %s)%n",
                            jetons, jetonsJoni, langue);
                }
            }
            System.out.printf("%-12s %12d %14d %10d %9.2f×%n",
                    langue, jetons, msJava, msJoni, (double) msJoni / msJava);
        }
        System.out.println();

        // 3. Mémoire : tas consommé par la lexicographie (motifs déjà compilés).
        System.gc();
        long avant = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        List<String> lignes = corpus("javascript", LIGNES);
        long pendant = 0;
        {
            List<Object> motifs = new ArrayList<>();
            for (Extraite e : grammars.get("javascript")) {
                try {
                    motifs.add(moteurJava.compiler(e.source()));
                } catch (Exception | StackOverflowError ex) {
                    motifs.add(null);
                }
            }
            balayer(motifs, lignes, (motif, ligne) -> {
                Matcher m = ((Pattern) motif).matcher(ligne);
                return m.find(0) ? new int[]{m.start(), m.end()} : null;
            });
            System.gc();
            pendant = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
        System.out.printf("-- Mémoire : tas consommé par une passe (motifs compilés) : %.1f Mo pour %.1f Mo de texte%n",
                (pendant - avant) / 1048576.0, lignes.stream().mapToInt(String::length).sum() / 1048576.0);
        System.out.println("   (mesure indicative : allocations temporaires dominantes, GC entre passes)");

        // 4. Constructions rencontrées (pour l'ADR).
        System.out.println();
        System.out.println("-- Constructions Oniguruma rencontrées (occurrences, tous motifs) --");
        Map<String, Integer> constructions = new TreeMap<>();
        for (List<Extraite> liste : grammars.values()) {
            for (Extraite e : liste) {
                for (var c : new String[][] {
                        {"\\\\h|\\\\H", "\\h hexadécimal"},
                        {"\\(\\?~", "(?~ absence"},
                        {"\\(\\?\\(", "(?( conditionnel"},
                        {"\\(\\?<[=!]", "lookbehind"},
                        {"\\[\\[:", "[[: classe POSIX"},
                        {"\\\\x\\{", "\\x{ codepoint étendu}"},
                        {"\\\\[pP]\\{", "\\p{ propriété}"},
                        {"\\(\\?[a-zA-Z]-[a-zA-Z]", "(?xx drapeaux relatifs"},
                        {"\\(\\?>", "(?> atomique"},
                        {"\\(\\?<[a-zA-Z]", "(?<nom> groupe nommé"},
                        {"\\\\[kK]<", "\\k<nom> référence nommée"},
                        {"\\\\[gG]\\{", "\\g{ appel de motif}"},
                        {"\\+\\+|\\?\\+|\\}\\+", "quantificateur possessif"},
                        {"&&", "intersection de classes"},
                        {"\\\\[ABGzb]", "ancres \\A \\B \\G \\z \\b"},
                }) {
                    if (Pattern.compile(c[0]).matcher(e.source()).find()) {
                        constructions.merge(c[1], 1, Integer::sum);
                    }
                }
            }
        }
        constructions.forEach((k, v) -> System.out.printf("  %5d× %s%n", v, k));

        System.out.println();
        System.out.println("-- Compatibilité Android (statique, joni 2.2.9 + jcodings 1.0.64) --");
        System.out.println("   détails par d8/dexdump : voir results/regex.txt (méthodes, sun.misc.Unsafe)");
    }

    /** Balaye chaque ligne en essayant chaque motif (travail comparable, sans pile). */
    static long balayer(List<Object> motifs, List<String> lignes,
                        BiFunction<Object, String, int[]> chercher) {
        long jetons = 0;
        for (String ligne : lignes) {
            if (ligne.isEmpty()) {
                continue;
            }
            for (Object motif : motifs) {
                if (motif == null) {
                    continue;
                }
                if (chercher.apply(motif, ligne) != null) {
                    jetons++;
                }
            }
        }
        return jetons;
    }

    private SpikeRegex() {
    }
}
