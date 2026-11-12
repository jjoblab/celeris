package jo.celeris.core.syntax.clike;

import jo.celeris.core.syntax.NodeKind;
/**
 * Sortes de nœuds communes aux langages de la famille C (M7.6, H-10) :
 * la factorisation des analyseurs écrits — Java, Kotlin, KTS, Groovy —
 * partage son socle, chaque langue garde ses règles par son dialecte.
 * Le plan, les plis et les diagnostics lisent la catégorie générique,
 * la sorte sert aux requêtes propres au langage ; les instantanés
 * ({@code ClikeSnapshotsTest}) prouvent que la factorisation ne change
 * rien à l'arbre produit.
 */
enum CLikeKind implements NodeKind {

    /** Déclaration de type (classe, interface, énumération, record, objet, trait). */
    TYPE,

    /** Méthode, fonction ou constructeur. */
    METHOD,

    /** Champ, propriété ou constante. */
    FIELD,

    /** Bloc d'initialisation ou bloc nu. */
    INITIALIZER,

    /** Instruction avec bloc (contrôle). */
    STATEMENT,

    /** Bloc d'accolades dans un corps. */
    BLOC,

    /** Structure malformée. */
    ERREUR,

    /** Trivia ou construction non structurée. */
    AUTRE
}
