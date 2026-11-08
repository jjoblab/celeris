package jo.celeris.core.syntax.grammars;

import java.util.List;

/**
 * Une grammaire TextMate (ADR-004 révisé, ADR-014) : le nom du langage,
 * sa portée racine, ses règles de premier niveau — les inclusions du
 * référentiel ({@code #clé}, {@code $self}, {@code $base}) restent des
 * références parcourues au fil de la lexicographie — et le rapport de
 * chargement : ce que le moteur a accepté, écarté ou dégradé, jamais en
 * silence. Immuable et sans état : le lexeur la partage entre tous les
 * documents du langage.
 *
 * @param nom          le nom affichable du langage
 * @param porteeRacine la portée racine (par exemple source.toml)
 * @param motifs       les règles de premier niveau
 * @param rapport      le rapport de chargement
 */
public record Grammaire(String nom, String porteeRacine, List<Regle> motifs,
        RapportGrammaire rapport) {

    /** Constructeur canonique : les motifs sont figés, jamais nuls. */
    public Grammaire {
        motifs = List.copyOf(motifs);
    }
}
