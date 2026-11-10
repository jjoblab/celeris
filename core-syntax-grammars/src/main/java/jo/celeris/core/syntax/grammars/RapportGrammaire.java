package jo.celeris.core.syntax.grammars;

/**
 * Le rapport de chargement d'une grammaire (ADR-014) : ce que le moteur
 * a accepté, écarté ou dégradé — jamais en silence. Le taux
 * d'acceptation (motifs compilés sur motifs rencontrés) devient le seuil
 * du banc de conformité : une montée de version du moteur qui
 * rejetterait un motif que les grammaires livrée utilisent échoue la CI.
 *
 * @param motifs              les expressions rencontrées (match, begin, end)
 * @param acceptes            les expressions compilées par joni
 * @param refuses             les règles écartées — motif invalide ou forme catastrophique
 * @param inclusionsExterieures les inclusions de portée externe absentes du registre
 * @param inclusionsInternes  les inclusions de référentiel inconnues — sautées,
 *                            comptées (les grammaires du commerce s'en
 *                            portent auteurs, VS Code les tolère aussi)
 * @param reglesWhile         les règles {@code begin/while} dégradées en motif simple
 * @param reglesDrapeaux      les drapeaux tolérés ({@code applyEndPatternLast} ignoré)
 * @param capturesSousMotifs  les captures à sous-motifs, dégradées au seul nom
 */
public record RapportGrammaire(int motifs, int acceptes, int refuses,
        int inclusionsExterieures, int inclusionsInternes, int reglesWhile,
        int reglesDrapeaux, int capturesSousMotifs) {

    /** Le constructeur canonique refuse les compteurs négatifs. */
    public RapportGrammaire {
        if (motifs < 0 || acceptes < 0 || refuses < 0 || inclusionsExterieures < 0
                || inclusionsInternes < 0 || reglesWhile < 0 || reglesDrapeaux < 0
                || capturesSousMotifs < 0) {
            throw new IllegalArgumentException("les compteurs sont positifs");
        }
    }

    /**
     * Le taux d'acceptation : la part des expressions rencontrées que le
     * moteur a compilées. Une grammaire sans expression est pleinement
     * acceptée.
     *
     * @return le taux, entre 0 et 1
     */
    public double acceptation() {
        return motifs == 0 ? 1.0 : (double) acceptes / motifs;
    }

    /** Le tisseur du rapport — interne au chargement, figé à la fin. */
    static final class Tisseur {

        private int motifs;
        private int acceptes;
        private int refuses;
        private int inclusionsExterieures;
        private int inclusionsInternes;
        private int reglesWhile;
        private int reglesDrapeaux;
        private int capturesSousMotifs;

        /** Une expression rencontrée et compilée. */
        void accepte() {
            motifs++;
            acceptes++;
        }

        /** Une expression rencontrée, refusée par le moteur. */
        void refuse() {
            motifs++;
            refuses++;
        }

        /** Une inclusion de portée externe sans grammaire au registre. */
        void inclusionExterieure() {
            inclusionsExterieures++;
        }

        /** Une inclusion de référentiel inconnue — sautée, comptée. */
        void inclusionInterne() {
            inclusionsInternes++;
        }

        /** Une règle begin/while dégradée en motif simple. */
        void regleWhile() {
            reglesWhile++;
        }

        /** Un drapeau toléré sans effet. */
        void regleDrapeau() {
            reglesDrapeaux++;
        }

        /** Une capture à sous-motifs, dégradée au seul nom. */
        void captureSousMotifs() {
            capturesSousMotifs++;
        }

        /** Le rapport figé. */
        RapportGrammaire tisser() {
            return new RapportGrammaire(motifs, acceptes, refuses, inclusionsExterieures,
                    inclusionsInternes, reglesWhile, reglesDrapeaux,
                    capturesSousMotifs);
        }
    }
}
