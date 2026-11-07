package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.syntax.EditSite;

/**
 * Le remappage des lignes d'une publication périmée : compose les sites
 * d'édition survenus depuis la dernière analyse publiée et résout une
 * ligne du document COURANT en sa ligne du document PUBLIÉ.
 *
 * <p>La garantie est celle du site lui-même (ADR-004) : les lignes
 * avant l'édition sont identiques des deux côtés, celles après sont
 * décalées du delta de lignes mais de contenu identique — les jetons,
 * relatifs à leur ligne, restent donc EXACTS pour toute ligne dont le
 * contenu a survécu. La zone touchée d'un site, elle, ne se remappe
 * pas : son contenu a changé, aucune plage de la publication ne peut la
 * colorer — la rangée retombe à l'encre de base le temps que la
 * publication à jour arrive.</p>
 *
 * <p>PIEGE-20 par construction : jamais une ligne au contenu non prouvé
 * identique ne se colore — le remappeur ne devine pas, il démontre.</p>
 */
public final class RemapLignes {

    private final List<EditSite> sites;

    private RemapLignes(List<EditSite> sites) {
        this.sites = sites;
    }

    /**
     * Le remappeur d'une chaîne de sites, du plus ancien au plus récent.
     *
     * @param sites les sites d'édition depuis la publication, en ordre
     *              chronologique
     * @return le remappeur composé
     */
    public static RemapLignes de(List<EditSite> sites) {
        return new RemapLignes(List.copyOf(sites));
    }

    /**
     * La ligne du document publié qui correspond à la ligne courante.
     *
     * @param ligneCourante  la ligne du document affiché
     * @param lignesPubliees le nombre de lignes du document publié
     * @return l'index de la ligne publiée au contenu identique, -1 quand
     *         la ligne a été touchée ou sort des bornes
     */
    public int lignePubliee(int ligneCourante, int lignesPubliees) {
        int ligne = ligneCourante;
        // Du site le plus récent au plus ancien : chaque édition rejoue
        // son décalage sur les lignes qui la suivent.
        for (int i = sites.size() - 1; i >= 0; i--) {
            EditSite site = sites.get(i);
            if (ligne < site.firstLine()) {
                continue;
            }
            if (ligne > site.lastLine()) {
                ligne -= site.lineDelta();
                continue;
            }
            return -1;
        }
        if (ligne < 0 || ligne >= lignesPubliees) {
            return -1;
        }
        return ligne;
    }
}
