package jo.celeris.core.language.packs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jo.celeris.core.language.JsonReader;

/**
 * Les sources de paquets connues (M7.6) : les ressources du module
 * (paquets intégrés, par l'index généré à la construction), un dossier
 * du stockage (chaque sous-dossier un paquet, les liens symboliques
 * refusés), une archive zip (un paquet, les entrées hors du dossier
 * refusées — zip-slip). Les hôtes peuvent écrire la leur derrière
 * {@link PackSource}.
 */
public final class Packs {

    private Packs() {
    }

    /**
     * La source des paquets intégrés : les ressources du module sous la
     * racine donnée, énumérées par l'index généré à la construction
     * (l'énumération directe du classpath ne traverse ni les jar ni les
     * dex de la même façon, l'index si).
     *
     * @param root la racine des ressources (par exemple {@code langages})
     * @return la source des paquets intégrés
     */
    public static PackSource classpath(String root) {
        return new ClasspathPacks(root);
    }

    /**
     * La source des paquets d'un dossier : chaque sous-dossier un
     * paquet, dans l'ordre alphabétique ; un lien symbolique se refuse.
     *
     * @param directory le dossier des paquets de l'utilisateur
     * @return la source du dossier
     */
    public static PackSource folder(Path directory) {
        return new FolderPacks(directory);
    }

    /**
     * La source d'un paquet archivé : un zip, un paquet — les entrées
     * doivent rester à la racine de l'archive, aucune ne remonte
     * ({@code ..} ou chemin absolu : zip-slip refusé).
     *
     * @param archive le fichier zip du paquet
     * @return la source de l'archive
     */
    public static PackSource archive(Path archive) {
        return new ArchivePacks(archive);
    }

    /** Les paquets intégrés du classpath, par l'index de la racine. */
    private static final class ClasspathPacks implements PackSource {

        private final String root;

        private ClasspathPacks(String root) {
            this.root = root;
        }

        @Override
        public List<PackFiles> packs() {
            String index = new String(read(root + "/index.json"),
                    StandardCharsets.UTF_8);
            Object lu = JsonReader.read(index);
            if (!(lu instanceof List<?> ids)) {
                throw new IllegalArgumentException(
                        "pack index must be an array: " + root + "/index.json");
            }
            List<PackFiles> packs = new ArrayList<>();
            for (Object id : ids) {
                String nom = String.valueOf(id);
                packs.add(new ClasspathPack(root + "/" + nom, nom));
            }
            return packs;
        }

        private byte[] read(String resource) {
            InputStream stream = Packs.class.getResourceAsStream("/" + resource);
            if (stream == null) {
                throw new IllegalArgumentException("missing pack resource: " + resource);
            }
            try (stream) {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Un paquet intégré : ses fichiers sont des ressources. */
        private final class ClasspathPack implements PackFiles {

            private final String prefix;

            private final String name;

            private ClasspathPack(String prefix, String name) {
                this.prefix = prefix;
                this.name = name;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean has(String file) {
                return Packs.class.getResourceAsStream("/" + prefix + "/" + file) != null;
            }

            @Override
            public byte[] read(String file) {
                return ClasspathPacks.this.read(prefix + "/" + file);
            }
        }
    }

    /** Les paquets d'un dossier du stockage, liens symboliques refusés. */
    private static final class FolderPacks implements PackSource {

        private final Path directory;

        private FolderPacks(Path directory) {
            this.directory = directory;
        }

        @Override
        public List<PackFiles> packs() throws IOException {
            if (!Files.isDirectory(directory)) {
                return List.of();
            }
            List<Path> dossiers = new ArrayList<>();
            try (var entrees = Files.list(directory)) {
                entrees.filter(Files::isDirectory).forEach(dossiers::add);
            }
            dossiers.sort(Comparator.comparing(Path::getFileName));
            List<PackFiles> packs = new ArrayList<>();
            for (Path dossier : dossiers) {
                if (Files.isSymbolicLink(dossier)) {
                    throw new IllegalArgumentException(
                            "symbolic links are not allowed in language packs: "
                                    + dossier);
                }
                packs.add(new FolderPack(dossier));
            }
            return packs;
        }

        /** Un paquet d'un sous-dossier : fichiers lus par chemin. */
        private record FolderPack(Path folder) implements PackFiles {

            @Override
            public String name() {
                return folder.getFileName().toString();
            }

            @Override
            public boolean has(String file) {
                return Files.isRegularFile(folder.resolve(file));
            }

            @Override
            public byte[] read(String file) throws IOException {
                Path chemin = folder.resolve(file);
                if (Files.isSymbolicLink(chemin)) {
                    throw new IllegalArgumentException(
                            "symbolic links are not allowed in language packs: "
                                    + chemin);
                }
                return Files.readAllBytes(chemin);
            }
        }
    }

    /** Un paquet archivé : les entrées lues en mémoire, zip-slip refusé. */
    private static final class ArchivePacks implements PackSource {

        private final Path archive;

        private ArchivePacks(Path archive) {
            this.archive = archive;
        }

        @Override
        public List<PackFiles> packs() throws IOException {
            Map<String, byte[]> entrees = new TreeMap<>();
            try (ZipInputStream zip =
                    new ZipInputStream(Files.newInputStream(archive))) {
                ZipEntry entree;
                while ((entree = zip.getNextEntry()) != null) {
                    if (entree.isDirectory()) {
                        continue;
                    }
                    String nom = entree.getName();
                    if (nom.startsWith("/") || nom.contains("..")) {
                        throw new IllegalArgumentException(
                                "zip entry escapes the pack: " + nom);
                    }
                    entrees.put(nom, zip.readAllBytes());
                }
            }
            return List.of(new ArchivePack(
                    archive.getFileName().toString(), entrees));
        }

        /** Les fichiers de l'archive, en mémoire, plats à la racine. */
        private record ArchivePack(String name, Map<String, byte[]> entries)
                implements PackFiles {

            @Override
            public boolean has(String file) {
                return entries.containsKey(file);
            }

            @Override
            public byte[] read(String file) {
                byte[] octets = entries.get(file);
                if (octets == null) {
                    throw new IllegalArgumentException(
                            "missing file in pack archive: " + file);
                }
                return octets;
            }
        }
    }
}
