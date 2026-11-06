package jo.celeris.demo;

import java.util.List;

/**
 * Les scénarios E-01 à E-06 de la matrice — les contrats d'intégration
 * de l'hôte (jalon M7) : système de fichiers virtuel, VCS, débogueur,
 * exécution, problèmes de build et traces d'appel, chacun livré avec
 * son implémentation de référence testée sans appareil.
 */
final class ScenariosIntegration {

    private ScenariosIntegration() {
    }

    /** La catégorie E, dans l'ordre de la matrice. */
    static List<Scenario> tous() {
        return List.of(
                new Scenario(
                        "E-01",
                        "Système de fichiers virtuel",
                        "Chemins normalisés et validés contre la traversée dès la "
                                + "construction ; ouverture, écriture et listage par le "
                                + "contrat, l'implémentation de référence en mémoire sert "
                                + "les tests."),
                new Scenario(
                        "E-02",
                        "VCS : marques et blame",
                        "Marques par ligne — modifié, ajouté, inchangé —, plages "
                                + "supprimées aux bornes inclusives, blame ligne à ligne et "
                                + "historique servis par le faux VCS de référence."),
                new Scenario(
                        "E-03",
                        "Débogueur : arrêts et survol",
                        "Points d'arrêt posés, retirés, désactivés ; ligne courante et "
                                + "pile en pause ; évaluation au survol bornée — refus hors "
                                + "pause et hors pile."),
                new Scenario(
                        "E-04",
                        "Exécution : lancements",
                        "Chaque lancement se signale aux auditeurs ; l'état par "
                                + "fichier reste visible : prêt, en cours, terminé, échoué."),
                new Scenario(
                        "E-05",
                        "Problèmes de build",
                        "La sortie des compilateurs se convertit en diagnostics : "
                                + "fichier, ligne, colonne et gravité exacts — le bruit ne "
                                + "produit rien."),
                new Scenario(
                        "E-06",
                        "Traces d'appel",
                        "Les cadres « at » rendent leur fichier et leur ligne exacts — "
                                + "la matière du saut fichier:ligne ; causes et ellipses "
                                + "ignorées."));
    }
}
