/**
 * Les implantations internes de la vue (ADR-009, règle 1 : tout
 * paquet dont le nom contient « internal » est interne — hors du
 * manifeste d'API, hors du contrat public). Les parcs d'état par
 * session y vivent : le parc des moteurs d'analyse (la
 * tokenisation appartient au document) et le parc des mises en
 * page (la mesure appartient au document — feuille de route des
 * améliorations, C1). La vue les orchestre, personne d'autre.
 */
@NullMarked
package jo.celeris.android.view.internal;

import org.jspecify.annotations.NullMarked;
