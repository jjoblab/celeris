package jo.celeris.android.host.spi;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * L'intégration débogueur de l'hôte (E-03) : points d'arrêt, ligne
 * courante, valeurs en ligne et évaluation au survol — bornée à la
 * pause et à la pile en cours.
 */
public interface IntegrationDebogueur {

    /**
     * Pose un point d'arrêt : actif par défaut.
     *
     * @param fichier le chemin du fichier
     * @param ligne   la ligne, numérotée à partir de un
     */
    void poserArret(CheminDoc fichier, int ligne);

    /**
     * Retire le point d'arrêt : muet si l'arrêt n'existe pas.
     *
     * @param fichier le chemin du fichier
     * @param ligne   la ligne de l'arrêt
     */
    void retirerArret(CheminDoc fichier, int ligne);

    /**
     * Bascule l'état actif/inactif du point d'arrêt (D-04) : muet si
     * l'arrêt n'existe pas.
     *
     * @param fichier le chemin du fichier
     * @param ligne   la ligne de l'arrêt
     */
    void basculerArret(CheminDoc fichier, int ligne);

    /**
     * Tous les points d'arrêt posés, actifs et inactifs.
     *
     * @return les arrêts, non modifiables
     */
    List<Arret> arrets();

    /**
     * La ligne courante de l'exécution en pause : null hors pause
     * (D-05).
     *
     * @return le lieu de la pause, ou null
     */
    @Nullable LigneArret ligneCourante();

    /**
     * La pile d'appels de la pause : vide hors pause (D-05).
     *
     * @return les cadres du plus récent au plus ancien
     */
    List<CadrePile> pileCourante();

    /**
     * Évalue l'expression au point de survol — bornée : refuse hors
     * pause et sur une ligne hors de la pile courante.
     *
     * @param fichier    le chemin du fichier survolé
     * @param ligne      la ligne survolée
     * @param colonne    la colonne survolée, à partir de un
     * @param expression l'expression à évaluer, dérivée du texte
     * @return la valeur rendue, écriture libre
     * @throws IllegalStateException hors pause ou hors pile
     */
    String evaluerAuSurvol(CheminDoc fichier, int ligne, int colonne, String expression);

    /**
     * Les valeurs à afficher en marge de la ligne donnée.
     *
     * @param ligne la ligne, numérotée à partir de un
     * @return les valeurs enregistrées pour la ligne
     */
    List<ValeurEnLigne> valeursEnLigne(int ligne);
}
