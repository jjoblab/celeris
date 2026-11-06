package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un indice en ligne du serveur (B-08) : le petit texte posé dans la
 * marge du code — un type inféré, un nom de paramètre — rendu pour la
 * plage visible. Le libellé vient en deux formes bornées : la chaîne
 * nue ou les parties typées, chaque partie pouvant porter sa cible de
 * navigation (le nom cliquable vers sa déclaration). Le dessin de la
 * cellule d'encre pleine, même ligne de base, vit à la vue (PIEGE-11
 * et PIEGE-12, livrés M3) : l'indice ne porte que sa géométrie.
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

    private static final IndiceLsp VIDE =
            new IndiceLsp(new PositionLsp(0, 0), List.of(), 0, false, false);

    /** Les parties copiées, profondes pour l'auditeur. */
    public IndiceLsp {
        parties = List.copyOf(parties);
    }

    /**
     * Analyse l'indice depuis sa forme JSON.
     *
     * @param json l'objet indice de la réponse
     * @return l'indice lu — vide si la forme est incomplète
     */
    public static IndiceLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return VIDE;
        }
        PositionLsp position = PositionLsp.de(objet.membreOu("position", JsonObjet.vide()));
        return new IndiceLsp(position, partiesDe(objet.membre("label")),
                entierOuZero(objet, "kind"),
                Boolean.TRUE.equals(objet.membreBooleen("paddingLeft")),
                Boolean.TRUE.equals(objet.membreBooleen("paddingRight")));
    }

    /**
     * Analyse la liste des indices de la réponse.
     *
     * @param json la valeur « result » de la requête
     * @return les indices lus, vide si la réponse est vide ou nulle
     */
    public static List<IndiceLsp> liste(ValeurJson json) {
        List<IndiceLsp> indices = new ArrayList<>();
        if (json instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                indices.add(de(tableau.element(i)));
            }
        }
        return List.copyOf(indices);
    }

    /** Les parties du libellé : la chaîne nue devient une partie seule. */
    private static List<Partie> partiesDe(@Nullable ValeurJson libelle) {
        List<Partie> parties = new ArrayList<>();
        if (libelle instanceof JsonChaine chaine) {
            parties.add(new Partie(chaine.texte(), null));
        } else if (libelle instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                parties.add(partieDe(tableau.element(i)));
            }
        }
        return parties;
    }

    private static Partie partieDe(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new Partie("", null);
        }
        String valeur = objet.membreTexte("value");
        LieuLsp cible = null;
        if (objet.membre("location") instanceof JsonObjet localisation) {
            cible = LieuLsp.de(localisation);
        }
        return new Partie(valeur == null ? "" : valeur, cible);
    }

    private static int entierOuZero(JsonObjet objet, String cle) {
        Long valeur = objet.membreEntier(cle);
        return valeur == null ? 0 : valeur.intValue();
    }
}
