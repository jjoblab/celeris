package jo.celeris.core.language;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.core.syntax.LanguageSyntax;

import org.jspecify.annotations.Nullable;

/**
 * Le chargeur de paquets (M7.6) : lit les fichiers d'un paquet, les
 * valide (schéma, taille maximale, profondeur, expressions
 * compilables), compile la grammaire par la fabrique de l'hôte et
 * rend un {@link LanguagePack} — ou refuse le paquet entier avec le
 * message précis de la première faute, jamais à moitié chargé. Le
 * paquet compilé se garde en cache, indexé par l'empreinte SHA-256 de
 * ses fichiers : recharger un paquet inchangé ne recompile rien.
 *
 * <p>Le chargeur travaille hors fil UI : c'est l'hôte qui l'appelle
 * sur son exécuteur de travail (ADR-007) ; ici, aucune attente, aucun
 * verrou bloquant au-delà du cache.
 */
public final class PackLoader {

    /** La taille maximale d'un fichier de paquet. */
    static final int MAX_FILE_BYTES = 1024 * 1024;

    /** La taille maximale de l'ensemble des fichiers d'un paquet. */
    static final int MAX_PACK_BYTES = 4 * MAX_FILE_BYTES;

    private final @Nullable GrammarFactory grammarFactory;

    private final Map<String, LanguagePack> cache = new ConcurrentHashMap<>();

    private PackLoader(@Nullable GrammarFactory grammarFactory) {
        this.grammarFactory = grammarFactory;
    }

    /**
     * Un chargeur sans fabrique de grammaires : les paquets sans
     * grammaire se chargent, ceux qui en portent une se refusent avec
     * le message qui nomme la fabrique manquante.
     *
     * @return le chargeur
     */
    public static PackLoader create() {
        return new PackLoader(null);
    }

    /**
     * Un chargeur muni de sa fabrique de grammaires.
     *
     * @param factory la fabrique de la voie de grammaires de l'hôte
     * @return le chargeur
     */
    public static PackLoader with(GrammarFactory factory) {
        return new PackLoader(factory);
    }

    /**
     * Charge un paquet : lecture, validation, compilation, empreinte.
     * Un paquet déjà chargé à l'identique (même empreinte) rend
     * l'instance en cache.
     *
     * @param files les fichiers du paquet
     * @return le paquet validé, grammaire compilée
     * @throws IOException si un fichier ne se lit pas
     * @throws IllegalArgumentException si le paquet est invalide, avec
     *                                  le message précis de la faute
     */
    public LanguagePack load(PackSource.PackFiles files) throws IOException {
        byte[] manifestBytes = read(files, "pack.json");
        byte[] languageBytes = read(files, "language.json");
        byte[] grammarBytes = files.has("grammar.json")
                ? read(files, "grammar.json")
                : null;
        byte[] snippetsBytes = files.has("snippets.json")
                ? read(files, "snippets.json")
                : null;
        bornerLaTaille(files, manifestBytes, languageBytes, grammarBytes,
                snippetsBytes);
        String fingerprint = fingerprint(files.name(), manifestBytes,
                languageBytes, grammarBytes, snippetsBytes);
        LanguagePack enCache = cache.get(fingerprint);
        if (enCache != null) {
            return enCache;
        }
        LanguagePack charge = assembler(files, manifestBytes, languageBytes,
                grammarBytes, snippetsBytes, fingerprint);
        cache.put(fingerprint, charge);
        return charge;
    }

    /** L'empreinte d'un paquet, ou null s'il n'est pas en cache. */
    public @Nullable LanguagePack cached(String fingerprint) {
        return cache.get(fingerprint);
    }

    /** Le paquet assemblé : analyse, cohérence, compilation. */
    private LanguagePack assembler(PackSource.PackFiles files,
            byte[] manifestBytes, byte[] languageBytes,
            byte @Nullable [] grammarBytes, byte @Nullable [] snippetsBytes,
            String fingerprint) {
        PackManifest manifest = PackParser.manifest(texte(manifestBytes));
        String languageJson = texte(languageBytes);
        LanguageDescriptor descriptor = PackParser.descriptor(languageJson);
        if (!manifest.id().equals(descriptor.id())) {
            throw new IllegalArgumentException(files.name()
                    + ": pack.json id " + manifest.id()
                    + " differs from language.json id " + descriptor.id());
        }
        LanguageConfiguration configuration =
                PackParser.configuration(languageJson);
        LanguagePack.SyntaxPreference preference =
                PackParser.syntaxPreference(languageJson);
        List<LanguageInjection> injections = PackParser.injections(languageJson);
        List<PackSnippet> snippets = snippetsBytes == null
                ? List.of()
                : PackParser.snippets(texte(snippetsBytes));
        LanguageSyntax grammar = null;
        if (grammarBytes != null) {
            grammar = compiler(files, manifest.id(), texte(grammarBytes));
        }
        return new LanguagePack(manifest, descriptor, configuration, grammar,
                grammarBytes == null ? null : texte(grammarBytes), snippets,
                injections, preference, fingerprint);
    }

    /** Compile la grammaire : la fabrique manque ou refuse, tout se dit. */
    private LanguageSyntax compiler(PackSource.PackFiles files, String id,
            String grammarJson) {
        if (grammarFactory == null) {
            throw new IllegalArgumentException(files.name()
                    + ": grammar.json needs a grammar factory "
                    + "(PackLoader.with)");
        }
        try {
            return grammarFactory.compile(id, grammarJson);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    files.name() + ": grammar.json rejected: " + e.getMessage(), e);
        }
    }

    /** Lit un fichier borné : le nom du paquet nomme l'erreur. */
    private static byte[] read(PackSource.PackFiles files, String file)
            throws IOException {
        byte[] octets = files.read(file);
        if (octets.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(files.name() + ": " + file
                    + " exceeds " + MAX_FILE_BYTES + " bytes");
        }
        return octets;
    }

    /** L'empreinte SHA-256 des fichiers du paquet, dans l'ordre des noms. */
    private static String fingerprint(String name, byte[] manifest,
            byte[] language, byte @Nullable [] grammar, byte @Nullable [] snippets) {
        MessageDigest condenseur = condenseur();
        condenseur.update(name.getBytes(StandardCharsets.UTF_8));
        condenseur.update((byte) 0);
        condenseur.update(manifest);
        condenseur.update((byte) 0);
        condenseur.update(language);
        if (grammar != null) {
            condenseur.update((byte) 0);
            condenseur.update(grammar);
        }
        if (snippets != null) {
            condenseur.update((byte) 0);
            condenseur.update(snippets);
        }
        return HexFormat.of().formatHex(condenseur.digest());
    }

    /** Le plafond du paquet tient : la somme des fichiers se borne. */
    private static void bornerLaTaille(PackSource.PackFiles files,
            byte[] manifest, byte[] language, byte @Nullable [] grammar,
            byte @Nullable [] snippets) {
        int total = manifest.length + language.length
                + (grammar == null ? 0 : grammar.length)
                + (snippets == null ? 0 : snippets.length);
        if (total > MAX_PACK_BYTES) {
            throw new IllegalArgumentException(files.name() + ": pack exceeds "
                    + MAX_PACK_BYTES + " bytes (" + total + " read)");
        }
    }

    /** Le condenseur SHA-256 — présent par construction dans la JVM. */
    private static MessageDigest condenseur() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Les octets en texte UTF-8. */
    private static String texte(byte[] octets) {
        return new String(octets, StandardCharsets.UTF_8);
    }
}
