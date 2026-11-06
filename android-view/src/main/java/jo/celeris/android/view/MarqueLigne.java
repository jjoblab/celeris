package jo.celeris.android.view;

/**
 * La marque de modification d'une ligne (D-03) : ce que la gouttière
 * trace à côté du numéro face à la révision de référence — le vocabulaire
 * de la vue, numérotée à partir de zéro ; l'adaptateur de l'hôte traduit
 * les siennes, numérotées à partir de un.
 */
public enum MarqueLigne {

    /** La ligne existe dans la référence, inchangée : aucune marque. */
    INCHANGE,

    /** La ligne existe dans la référence, son contenu a changé. */
    MODIFIE,

    /** La ligne n'existe pas dans la référence. */
    AJOUTE
}
