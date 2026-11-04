package jo.celeris.core.language;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Registre des langages (core-language) : identification d'un langage par
 * nom de fichier, extension composée comprise — la plus longue gagne
 * ({@code build.gradle.kts} est du KTS, pas du script Groovy). Le registre
 * est une donnée pure : c'est l'hôte qui le peuple des langages qu'il
 * embarque.
 */
public final class LanguageRegistry {

    private final List<Language> languages;

    private LanguageRegistry(List<Language> languages) {
        this.languages = languages;
    }

    /**
     * Un registre des langages donnés.
     *
     * @param languages les langages embarqués
     * @return le registre prêt à identifier
     */
    public static LanguageRegistry of(Language... languages) {
        return new LanguageRegistry(List.of(languages));
    }

    /**
     * Identifie le langage d'un nom de fichier — extension composée la
     * plus longue d'abord.
     *
     * @param filename le nom de fichier, chemin complet accepté
     * @return le langage identifié, ou null si aucun ne correspond
     */
    public @Nullable Language byFilename(String filename) {
        String nom = basename(filename);
        Language meilleur = null;
        int longueurMax = 0;
        for (Language langage : languages) {
            for (String extension : langage.extensions()) {
                if (nom.endsWith("." + extension) && extension.length() > longueurMax) {
                    meilleur = langage;
                    longueurMax = extension.length();
                }
            }
        }
        return meilleur;
    }

    private static String basename(String filename) {
        int barre = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        return barre < 0 ? filename : filename.substring(barre + 1);
    }
}
