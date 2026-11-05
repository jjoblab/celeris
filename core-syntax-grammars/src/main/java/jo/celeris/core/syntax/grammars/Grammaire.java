package jo.celeris.core.syntax.grammars;

import java.util.List;

/**
 * Une grammaire déclarative (format TextMate, ADR-004) : le nom du
 * langage, sa portée racine et ses règles de premier niveau — les
 * inclusions du référentiel ({@code #cle}, {@code $self}) sont aplaties
 * au chargement, chaque liste de motifs ne contient que des règles
 * simples ou des blocs. Immuable et sans état : le lexeur la partage
 * entre tous les documents du langage.
 *
 * @param nom         le nom affichable du langage
 * @param porteeRacine la portée racine (par exemple source.toml)
 * @param motifs      les règles de premier niveau, aplaties
 */
public record Grammaire(String nom, String porteeRacine, List<Regle> motifs) {

    /** Constructeur canonique : les motifs sont figés, jamais nuls. */
    public Grammaire {
        motifs = List.copyOf(motifs);
    }
}
