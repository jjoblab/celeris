package jo.celeris.core.language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jspecify.annotations.Nullable;

/**
 * Le registre des langages (M7.6) : l'index immuable des
 * {@link LanguageSupport} enregistrés et la détection d'un langage par
 * nom de fichier. La précédence est documentée et testée : réglage de
 * l'utilisateur, nom de fichier exact, motif de nom, extension composée
 * la plus longue (avec indice de contenu pour les extensions que
 * plusieurs langages revendiquent), première ligne.
 *
 * <p>Deux langages ne revendiquent jamais le même nom exact : le
 * bâtisseur le vérifie à la construction, tout comme l'unicité des
 * identifiants — un registre qui se bâtit rend ses violations
 * immédiatement, pas à la première détection.
 */
public final class LanguageRegistry {

    private final List<LanguageSupport> supports;

    private final Map<String, LanguageSupport> byId;

    private final Map<String, LanguageSupport> byFilename;

    private final Map<String, LanguageSupport> userChoices;

    private final List<CompiledPattern> filenamePatterns;

    private final List<CompiledPattern> firstLines;

    private final Map<String, Pattern> contentHints;

    private LanguageRegistry(List<LanguageSupport> supports,
            Map<String, LanguageSupport> byId,
            Map<String, LanguageSupport> byFilename,
            Map<String, LanguageSupport> userChoices,
            List<CompiledPattern> filenamePatterns,
            List<CompiledPattern> firstLines,
            Map<String, Pattern> contentHints) {
        this.supports = supports;
        this.byId = byId;
        this.byFilename = byFilename;
        this.userChoices = userChoices;
        this.filenamePatterns = filenamePatterns;
        this.firstLines = firstLines;
        this.contentHints = contentHints;
    }

    /**
     * Le bâtisseur d'un registre.
     *
     * @return le bâtisseur vide
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Les supports enregistrés, dans l'ordre d'inscription. */
    public List<LanguageSupport> supports() {
        return supports;
    }

    /**
     * Le support d'un identifiant.
     *
     * @param id l'identifiant du langage
     * @return son support, ou null si l'identifiant est inconnu
     */
    public @Nullable LanguageSupport byId(String id) {
        return byId.get(id);
    }

    /**
     * Détecte le langage d'un fichier par la précédence du registre.
     *
     * @param filename  le nom du fichier, chemin complet accepté
     * @param firstLine la première ligne du fichier, ou null
     * @param sample    un échantillon du contenu (les premiers milliers
     *                  de caractères suffisent), ou null
     * @return la détection : résolue, ambiguë ou sans langage
     */
    public LanguageDetection detect(String filename, @Nullable String firstLine,
            @Nullable String sample) {
        String nom = basename(filename).toLowerCase(Locale.ROOT);
        LanguageSupport reglage = userChoices.get(nom);
        if (reglage != null) {
            return new LanguageDetection.Resolved(reglage);
        }
        LanguageSupport exact = byFilename.get(nom);
        if (exact != null) {
            return new LanguageDetection.Resolved(exact);
        }
        List<LanguageSupport> parMotif = matches(filenamePatterns, nom, true);
        if (!parMotif.isEmpty()) {
            return parMotif.size() == 1
                    ? new LanguageDetection.Resolved(parMotif.get(0))
                    : new LanguageDetection.Ambiguous(parMotif);
        }
        List<LanguageSupport> parExtension = byExtension(nom);
        if (!parExtension.isEmpty()) {
            return byExtensionOuIndice(parExtension, sample);
        }
        if (firstLine != null && !firstLine.isEmpty()) {
            List<LanguageSupport> parLigne = matches(firstLines, firstLine, false);
            if (!parLigne.isEmpty()) {
                return parLigne.size() == 1
                        ? new LanguageDetection.Resolved(parLigne.get(0))
                        : new LanguageDetection.Ambiguous(parLigne);
            }
        }
        return new LanguageDetection.Unresolved();
    }

    /**
     * Le langage d'un nom de fichier, sans première ligne ni
     * échantillon — la commodité des hôtes qui n'ont que le nom.
     *
     * @param filename le nom du fichier, chemin complet accepté
     * @return le support résolu, ou null (ambiguë ou sans langage)
     */
    public @Nullable LanguageSupport byFilename(String filename) {
        LanguageDetection detection = detect(filename, null, null);
        return detection instanceof LanguageDetection.Resolved resolu
                ? resolu.support()
                : null;
    }

    /** Une revendication d'extension : l'unique gagne, l'indice départage l'égalité. */
    private LanguageDetection byExtensionOuIndice(List<LanguageSupport> candidats,
            @Nullable String sample) {
        if (candidats.size() == 1) {
            return new LanguageDetection.Resolved(candidats.get(0));
        }
        List<LanguageSupport> indices = byContentHint(candidats, sample);
        if (indices.size() == 1) {
            return new LanguageDetection.Resolved(indices.get(0));
        }
        return new LanguageDetection.Ambiguous(indices);
    }

    /** Les langages dont une extension revendiquée est la plus longue. */
    private List<LanguageSupport> byExtension(String nom) {
        Set<LanguageSupport> candidats = new LinkedHashSet<>();
        int longueurMax = 0;
        for (LanguageSupport support : supports) {
            for (String extension : support.descriptor().extensions()) {
                if (nom.endsWith("." + extension)
                        && extension.length() >= longueurMax) {
                    if (extension.length() > longueurMax) {
                        candidats.clear();
                        longueurMax = extension.length();
                    }
                    candidats.add(support);
                }
            }
        }
        return new ArrayList<>(candidats);
    }

    /**
     * Le départage par le contenu : les candidats dont l'indice
     * apparaît dans l'échantillon gagnent ; sans indice qui parle, les
     * candidats SANS indice l'emportent — ils se déclarent défaut de
     * leur extension (le {@code .h} sans marque C++ est du C) ; sans
     * échantillon, même règle : le défaut est déterministe. Sans
     * défaut ni indice, l'égalité reste ambiguë — l'interface peut la
     * montrer.
     */
    private List<LanguageSupport> byContentHint(List<LanguageSupport> candidats,
            @Nullable String sample) {
        List<LanguageSupport> indices = new ArrayList<>();
        List<LanguageSupport> defauts = new ArrayList<>();
        for (LanguageSupport candidat : candidats) {
            Pattern indice = contentHints.get(candidat.id());
            if (indice == null) {
                defauts.add(candidat);
            } else if (sample != null && !sample.isEmpty()
                    && indice.matcher(sample).find()) {
                indices.add(candidat);
            }
        }
        if (!indices.isEmpty()) {
            return indices;
        }
        if (!defauts.isEmpty()) {
            return defauts;
        }
        return candidats;
    }

    /** Les supports dont un motif correspond — en entier ou en extrait. */
    private static List<LanguageSupport> matches(List<CompiledPattern> motifs,
            String valeur, boolean enEntier) {
        List<LanguageSupport> trouves = new ArrayList<>();
        for (CompiledPattern motif : motifs) {
            boolean vu = enEntier
                    ? motif.pattern().matcher(valeur).matches()
                    : motif.pattern().matcher(valeur).find();
            if (vu) {
                trouves.add(motif.support());
            }
        }
        return trouves;
    }

    /** Le nom de fichier sans son chemin. */
    private static String basename(String filename) {
        int barre = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        return barre < 0 ? filename : filename.substring(barre + 1);
    }

    /** Un motif compilé vers son support, avec sa portée de correspondance. */
    private record CompiledPattern(Pattern pattern, boolean fullMatch,
            LanguageSupport support) {
    }

    /** Le bâtisseur de {@link LanguageRegistry}. */
    public static final class Builder {

        private final List<LanguageSupport> supports = new ArrayList<>();

        private final Map<String, String> userChoices = new LinkedHashMap<>();

        /** Bâtit le registre : unicité vérifiée, motifs compilés, choix résolus. */
        public LanguageRegistry build() {
            Map<String, LanguageSupport> byId = new LinkedHashMap<>();
            Map<String, LanguageSupport> byFilename = new LinkedHashMap<>();
            List<CompiledPattern> filenamePatterns = new ArrayList<>();
            List<CompiledPattern> firstLines = new ArrayList<>();
            Map<String, Pattern> contentHints = new HashMap<>();
            for (LanguageSupport support : supports) {
                verifierUnicite(support, byId, byFilename);
                compilerMotifs(support.descriptor(), support, filenamePatterns,
                        firstLines, contentHints);
            }
            Map<String, LanguageSupport> resolus =
                    resoudreChoix(userChoices, byId);
            return new LanguageRegistry(List.copyOf(supports), Map.copyOf(byId),
                    Map.copyOf(byFilename), Map.copyOf(resolus),
                    List.copyOf(filenamePatterns), List.copyOf(firstLines),
                    Map.copyOf(contentHints));
        }

        /**
         * Inscrit un support.
         *
         * @param support le support à inscrire
         * @return le bâtisseur
         */
        public Builder support(LanguageSupport support) {
            supports.add(support);
            return this;
        }

        /**
         * Inscrit des supports en bloc.
         *
         * @param ajoutes les supports à inscrire
         * @return le bâtisseur
         */
        public Builder supports(List<LanguageSupport> ajoutes) {
            supports.addAll(ajoutes);
            return this;
        }

        /**
         * Le réglage de l'utilisateur : ce fichier parle ce langage,
         * toute détection battue par ce choix.
         *
         * @param filename   le nom de fichier exact
         * @param languageId l'identifiant du langage imposé
         * @return le bâtisseur
         */
        public Builder userChoice(String filename, String languageId) {
            userChoices.put(filename.toLowerCase(Locale.ROOT), languageId);
            return this;
        }

        /** Identifiants et noms exacts uniques — la propriété du registre. */
        private static void verifierUnicite(LanguageSupport support,
                Map<String, LanguageSupport> byId,
                Map<String, LanguageSupport> byFilename) {
            LanguageDescriptor descriptor = support.descriptor();
            LanguageSupport doublon = byId.putIfAbsent(descriptor.id(), support);
            if (doublon != null) {
                throw new IllegalArgumentException(
                        "duplicate language id: " + descriptor.id());
            }
            for (String filename : descriptor.filenames()) {
                String cle = filename.toLowerCase(Locale.ROOT);
                LanguageSupport autre = byFilename.putIfAbsent(cle, support);
                if (autre != null) {
                    throw new IllegalArgumentException(
                            "filename " + filename + " claimed by " + autre.id()
                                    + " and " + descriptor.id());
                }
            }
        }

        /** Les choix de l'utilisateur résolus contre les identifiants connus. */
        private static Map<String, LanguageSupport> resoudreChoix(
                Map<String, String> choix, Map<String, LanguageSupport> byId) {
            Map<String, LanguageSupport> resolus = new LinkedHashMap<>();
            for (Map.Entry<String, String> entree : choix.entrySet()) {
                LanguageSupport support = byId.get(entree.getValue());
                if (support == null) {
                    throw new IllegalArgumentException(
                            "user choice for " + entree.getKey()
                                    + " names unknown language " + entree.getValue());
                }
                resolus.put(entree.getKey(), support);
            }
            return resolus;
        }

        /** Compile les motifs d'un support : jokers de nom, première ligne, indice. */
        private static void compilerMotifs(LanguageDescriptor descriptor,
                LanguageSupport support, List<CompiledPattern> filenamePatterns,
                List<CompiledPattern> firstLines, Map<String, Pattern> contentHints) {
            for (String motif : descriptor.filenamePatterns()) {
                filenamePatterns.add(new CompiledPattern(glob(motif), true, support));
            }
            if (descriptor.firstLine() != null) {
                firstLines.add(new CompiledPattern(
                        regex(descriptor.firstLine(), descriptor.id(), "firstLine"),
                        false, support));
            }
            if (descriptor.contentHint() != null) {
                contentHints.put(descriptor.id(),
                        regex(descriptor.contentHint(), descriptor.id(), "contentHint"));
            }
        }

        /** Un motif de nom de fichier : les jokers {@code *} et {@code ?}. */
        private static Pattern glob(String motif) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < motif.length(); i++) {
                char c = motif.charAt(i);
                if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append('.');
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        }

        /** Une expression régulière du descripteur, les erreurs nommées. */
        private static Pattern regex(String motif, String id, String role) {
            try {
                return Pattern.compile(motif);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(
                        "language " + id + ": invalid " + role + " pattern: " + motif, e);
            }
        }
    }
}
