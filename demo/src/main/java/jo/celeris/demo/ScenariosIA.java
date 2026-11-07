package jo.celeris.demo;

import java.util.List;

/**
 * Les scénarios F-01 à F-10 de la matrice — l'IA native (jalon M8,
 * ADR-012) : texte fantôme et prédiction au faux modèle local gardé
 * par le contexte (consentement coupé par défaut, exclusions des
 * secrets), revue des modifications d'agent par blocs, API d'outils
 * pour agents et surface MCP, annulation et budgets de latence, aucun
 * trafic sans consentement — la démonstration reste neutre par
 * construction.
 */
final class ScenariosIA {

    private ScenariosIA() {
    }

    /** La catégorie F, dans l'ordre de la matrice. */
    static List<Scenario> tous() {
        List<Scenario> tous = new java.util.ArrayList<>(fantomeEtOutils());
        tous.addAll(gardeEtConsentement());
        return List.copyOf(tous);
    }

    /** F-01 à F-05 — fantôme, prédiction, menu, revue d'agent, outils. */
    private static List<Scenario> fantomeEtOutils() {
        return List.of(
                new Scenario(
                        "F-01",
                        "Texte fantôme multiligne",
                        "La suggestion se dessine éphémère à l'ancre, une "
                                + "cellule d'encre pleine par caractère sur la "
                                + "ligne de base du code ; l'acceptation se prend "
                                + "par mot (Tab, appui sur le fantôme), par ligne "
                                + "ou en bloc — le reste survit ré-ancré."),
                new Scenario(
                        "F-02",
                        "Prédiction de la prochaine édition",
                        "Le modèle prédit une opération, pas un texte ; "
                                + "l'application reste explicite — transaction "
                                + "ordinaire donc annulable, le refus (révision, "
                                + "protection) se lit."),
                new Scenario(
                        "F-03",
                        "Actions IA au menu d'intentions",
                        "Suggestion et prédiction vivent au menu de "
                                + "l'atelier, au même titre que lint, aperçu de "
                                + "définition et revues — même barre, même "
                                + "gestuelle."),
                new Scenario(
                        "F-04",
                        "Revue des modifications d'agent",
                        "La proposition arrive étiquetée, les blocs se "
                                + "décident un à un — accepter applique, refuser "
                                + "réaligne les suivants —, le point de contrôle "
                                + "revient en arrière, la fusion s'applique en "
                                + "une transaction annulable."),
                new Scenario(
                        "F-05",
                        "API d'outils pour agents",
                        "Lire des plages et l'instantané, lister les "
                                + "symboles et les diagnostics, appliquer une "
                                + "transaction avec détection de conflit — le "
                                + "refus porte son message, jamais de fusion "
                                + "silencieuse."));
    }

    /** F-06 à F-10 — MCP, exclusions, modèles, latence, consentement. */
    private static List<Scenario> gardeEtConsentement() {
        return List.of(
                new Scenario(
                        "F-06",
                        "Surface compatible MCP",
                        "Les six outils s'exposent par le connecteur sous "
                                + "leurs noms de serveur (celeris.*), chaque "
                                + "appel traverse la même méthode qu'en direct, "
                                + "les réponses se rendent en JSON — le transport "
                                + "reste à l'hôte."),
                new Scenario(
                        "F-07",
                        "Exclusions de contexte",
                        "Le fichier du dépôt déclare les chemins jamais "
                                + "envoyés à un modèle — secrets, clés ; le garde "
                                + "consulte avant chaque départ, le journal du "
                                + "faux modèle en témoigne ; les instructions "
                                + "d'agent se servent par les outils."),
                new Scenario(
                        "F-08",
                        "Modèles locaux ou distants",
                        "Le cœur ne connaît aucun fournisseur : le faux "
                                + "local sert synchrone, le faux distant ne livre "
                                + "que par l'exécuteur injecté — même contrat, "
                                + "journal des requêtes compris."),
                new Scenario(
                        "F-09",
                        "Annulation et budgets de latence",
                        "La frappe suivante annule le vol — mille demandes "
                                + "au modèle aveugle rendent la main à chaque "
                                + "fois ; le tracé du fantôme reste sous le "
                                + "budget d'une image, mesuré au minimum de trois "
                                + "tours."),
                new Scenario(
                        "F-10",
                        "Aucun trafic sans consentement",
                        "L'interrupteur des réglages reste coupé par "
                                + "défaut : sans lui, aucune requête ne part — "
                                + "pas même vers le modèle local ; le manifeste "
                                + "de la démonstration ne déclare d'ailleurs "
                                + "aucune prise réseau, la neutralité est "
                                + "structurelle."));
    }
}
