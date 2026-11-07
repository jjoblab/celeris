package jo.celeris.core.ai;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * La proposition d'un agent (F-04, ADR-012) : une liste d'éditions
 * étiquetée, datée de l'instantané de référence contre lequel elle se
 * révise.
 *
 * <p>Les éditions vivent en coordonnées du document de référence —
 * la discipline des éditions d'espace de travail : ordonnées, sans
 * chevauchement, chaque texte retiré exact. C'est ce qui permet au
 * refus d'un bloc de ne rien décaloper : les blocs refusés se sautent,
 * les acceptés s'appliquent pareil, l'ordre du document décide.</p>
 *
 * @param etiquette l'auteur et l'intention, lus à la revue
 * @param revision  la révision de l'instantané de référence
 * @param edits     les éditions, ordonnées par position croissante,
 *                  plages disjointes
 */
public record PropositionAgent(EtiquetteAgent etiquette, int revision,
        List<TextEdit> edits) {

    /** Copie défensive — la proposition ne se modifie jamais après coup. */
    public PropositionAgent {
        edits = List.copyOf(edits);
    }

    /**
     * La proposition d'édits sur l'instantané donné, validée dès la
     * construction : plages dans les bornes, textes retirés exacts,
     * ordre croissant sans chevauchement — une proposition qui ne
     * colle pas au document de référence ne s'ouvre pas, la revue ne
     * répare pas ce qu'elle reçoit.
     *
     * @param reference l'instantané de référence, celui de la révision
     * @param etiquette l'auteur et l'intention
     * @param edits     les éditions proposées
     * @return la proposition validée
     * @throws IllegalArgumentException si une édition ne colle pas au
     *         document, si les plages se chevauchent ou désordonnent
     */
    public static PropositionAgent de(Document reference, EtiquetteAgent etiquette,
            List<TextEdit> edits) {
        int borne = reference.length();
        int finPrecedente = 0;
        for (TextEdit edition : edits) {
            if (edition.end() > borne) {
                throw new IllegalArgumentException(
                        "edition hors document : " + edition.start());
            }
            if (edition.start() < finPrecedente) {
                throw new IllegalArgumentException(
                        "edition desordonnee ou chevauchante : " + edition.start());
            }
            if (!reference.subText(edition.start(), edition.end())
                    .equals(edition.removed())) {
                throw new IllegalArgumentException(
                        "texte retire inexact a " + edition.start());
            }
            finPrecedente = edition.end();
        }
        return new PropositionAgent(etiquette, reference.revision(), edits);
    }
}
