package jo.celeris.core.lsp;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un indice en ligne du serveur (B-08) : le petit texte posé dans la
 * marge du code — un type inféré, un nom de paramètre — rendu pour la
 * plage visible. Le libellé vient en deux formes bornées : la chaîne
 * nue ou les parties typées, chaque partie pouvant porter sa cible de
 * navigation (le nom cliquable vers sa déclaration). Le dessin de la
 * cellule d'encre pleine, même ligne de base, vit à la vue (PIEGE-11
 * et PIEGE-12, livrés M3) : l'indice ne porte que sa géométrie. La
 * lecture du fil vit derrière la frontière protocolaire (ADR-015) —
 * les indices rejoindront la correspondance à la couverture nouvelle
 * (H-12).
 *
 * @param position          la position où l'indice se pose
 * @param parties           les parties du libellé, dans l'ordre
 * @param genre             le genre d'indice (1 type, 2 paramètre), 0
 *                          si le serveur n'en dit rien
 * @param remplissageGauche vrai pour une espace avant le libellé
 * @param remplissageDroite vrai pour une espace après le libellé
 */
public record IndiceLsp(PositionLsp position, List<Partie> parties, int genre,
                       boolean remplissageGauche, boolean remplissageDroite) {

    /** Une partie du libellé : sa valeur, sa cible de navigation si le serveur la donne. */
    public record Partie(String valeur, @Nullable LieuLsp cible) {
    }

    /** Les parties copiées, profondes pour l'auditeur. */
    public IndiceLsp {
        parties = List.copyOf(parties);
    }
}
