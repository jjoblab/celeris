package jo.celeris.core.language;

import org.jspecify.annotations.Nullable;

/**
 * Un contributeur de langage (M7.6) : les composants Java spécifiques
 * d'un langage — l'intelligence de Java, Kotlin, XML Android —
 * s'enregistrent ici, explicitement, sans réflexion. Sur Android,
 * l'hôte appelle ses contributeurs connus ; sur JVM, le cli les
 * découvre par {@code ServiceLoader}. L'interface vit dans le module
 * du bas pour que les deux mondes la voient.
 */
public interface LanguageContributor {

    /**
     * Déclare les capacités Java des langages du contributeur.
     *
     * @param contribution le récepteur des capacités déclarées
     */
    void contribute(Contribution contribution);

    /** Le récepteur des capacités d'un contributeur. */
    interface Contribution {

        /**
         * Ajoute un fournisseur de capacité à un langage.
         *
         * @param languageId l'identifiant du langage servi
         * @param key        la clé de la capacité
         * @param provider   le fournisseur
         * @param <T>        le type du fournisseur
         */
        <T extends LanguageCapability> void capability(String languageId,
                Class<T> key, T provider);

        /**
         * Le fournisseur déjà déclaré pour un langage et une clé — les
         * contributeurs composent entre eux, ou null.
         *
         * @param languageId l'identifiant du langage
         * @param key        la clé de la capacité
         * @param <T>        le type du fournisseur
         * @return le fournisseur déclaré, ou null
         */
        <T extends LanguageCapability> @Nullable T declared(String languageId,
                Class<T> key);
    }
}
