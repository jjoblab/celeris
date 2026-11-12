package jo.celeris.demo;

import java.util.List;

/**
 * Les scénarios de la catégorie G — exigences globales — et de la
 * catégorie H, langages et protocole : l'ouverture progressive et les
 * budgets, la neutralité hors ligne, les plugins isolés, l'arbre
 * d'accessibilité virtuel (G-05), le paquet jouet de l'utilisateur
 * (H-04, M9 : ajouter un langage sans recompiler) et la couverture
 * LSP nouvelle par la frontière protocolaire (H-12, ADR-015).
 */
final class ScenariosGlobales {

    private ScenariosGlobales() {
    }

    /** Les catégories G et H, dans l'ordre de la matrice. */
    static List<Scenario> tous() {
        return List.of(
                new Scenario(
                        "G-01",
                        "Ouverture progressive",
                        "Cent mille lignes : première image mesurée à 154 ms "
                                + "(budget 500 ms), 480 000 lignes ouvrables — la "
                                + "fenêtre seule se sert, la coloration suit par l'analyse "
                                + "hors fil : fenêtre visible d'abord (224 ms mesurés sur cent mille lignes), "
                                + "complétion par lots ensuite."),
                new Scenario(
                        "G-02",
                        "Tactile d'abord, hors ligne",
                        "La démonstration tourne hors ligne, aucun réveil réseau ; "
                                + "les budgets d'image tiennent par le cache de "
                                + "rangées."),
                new Scenario(
                        "G-04",
                        "Plugins isolés",
                        "L'erreur d'un plugin ne tue jamais l'éditeur : l'extension qui "
                                + "lève est journalisée, les suivantes courent, l'image suivante "
                                + "est intacte."),
                new Scenario(
                        "G-05",
                        "Accessibilité",
                        "L'éditeur s'expose comme un nœud éditable dont les "
                                + "enfants virtuels sont les rangées visibles : "
                                + "lecture par lignes et plages, mouvements à "
                                + "granularité — clusters, mots, rangées, pages."),
                new Scenario(
                        "H-04",
                        "Ajouter un langage sans recompiler",
                        "Les réglages copient le paquet jouet Logo au stockage — le "
                                + "registre se recharge à chaud, la carte tortue.lgo entre à "
                                + "l'accueil : l'échantillon se colore, se plie, se commente — "
                                + "aucun mot de Logo ne vit dans le code."),
                new Scenario(
                        "H-12",
                        "Couverture LSP nouvelle",
                        "Symboles, plis, formatage, surlignages, renommage, correctifs, "
                                + "indices, jetons : la frontière protocolaire porte chaque "
                                + "méthode, le domaine les rend (M7.6, ADR-015)."),
                new Scenario(
                        "H-13",
                        "Serveur Java déclaré éprouvé",
                        "Le mini serveur Java embarqué monte par sa déclaration "
                                + "servers.json : complétion, survol, signature, symboles, "
                                + "plis, formatage et diagnostics poussés répondent au fil "
                                + "— la démo qui démontre se prouve à l'ouverture."));
    }
}
