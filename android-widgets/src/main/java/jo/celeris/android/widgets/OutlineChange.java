package jo.celeris.android.widgets;

/**
 * Le changement d'un plan à l'autre (B-16) : le préfixe intact se
 * conserve — les entrées avant le site d'édition sont identiques, la
 * mise à jour est incrémentale — et le consommateur reçoit le compte
 * de ce qui remplace et de ce qui s'ajoute.
 *
 * @param prefixeConserve le nombre d'entrées conservées en tête
 * @param remplacees      le nombre d'anciennes entrées remplacées
 * @param ajoutees        le nombre de nouvelles entrées en remplacement
 */
public record OutlineChange(int prefixeConserve, int remplacees, int ajoutees) {
}
