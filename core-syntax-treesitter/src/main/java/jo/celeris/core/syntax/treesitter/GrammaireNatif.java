package jo.celeris.core.syntax.treesitter;

import java.util.Objects;

import com.itsaky.androidide.treesitter.TSLanguage;

/**
 * La grammaire servie par la voie (ADR-0025, généralisée par
 * l'ADR-0033) : une langue tree-sitter de la liaison et SA requête
 * de surbrillance ({@code highlights.scm}). La grammaire vit chez
 * L'HÔTE — le module ne porte que le châssis : l'hôte qui veut une
 * langue en dépend par Maven (l'artefact
 * {@code com.itsaky.androidide.treesitter:tree-sitter-&lt;langue&gt;})
 * et l'enregistre ici par
 * {@link TreeSitterGrammars#enregistrer(String, TSLanguageSupplier, String)}
 * — le modèle « la grammaire suit la dépendance » de l'éditeur de
 * référence : chaque langue se paie SEULEMENT si on l'embarque.
 *
 * <p>La requête {@code highlights.scm} se lit par
 * {@link LecteurRequetes} (sans la liaison — les bancs JVM y
 * servent) : chaque entrée y pose sa teinte, la couleur vient de la
 * requête du langage, jamais d'une table codée en dur. La requête
 * vide laisse les heuristiques de repli servir — rien ne casse.</p>
 *
 * @param id      l'identifiant du langage servi (celui du paquet : "json")
 * @param langue le fournisseur de la langue tree-sitter de la liaison
 * @param requete le texte de la requête highlights.scm du langage
 */
public record GrammaireNatif(String id,
        TSLanguageSupplier langue,
        String requete) {

    /** Constructeur canonique : id valide, fournisseur présent,
     *  requête jamais null (vide autorisé : heuristiques de repli). */
    public GrammaireNatif {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(langue, "langue");
        requete = requete == null ? "" : requete;
        if (id.isEmpty()) {
            throw new IllegalArgumentException("empty grammar id");
        }
    }
}
