package jo.celeris.core.ai;

/**
 * Une plage lue par la voie des outils (F-05, ADR-012) : le texte et
 * la révision de l'instantané qui l'a servi — l'agent qui veut écrire
 * rend cette révision en référence de sa transaction, la détection de
 * conflit s'y accroche.
 *
 * @param revision la révision de l'instantané servi
 * @param texte    le texte de la plage
 */
public record PlageLue(int revision, String texte) {
}
