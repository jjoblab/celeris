package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

/**
 * Le moteur d'analyse syntaxique VU PAR LE PONT DE VUE (ADR-0024) :
 * les DEUX voies de coloration — la voie des grammaires (TextMate,
 * embarquée) et la voie tree-sitter (module séparé, opt-in) — y
 * paraissent IDENTIQUES. Le pont ne connaît plus le moteur concret :
 * il reçoit l'implémentation que la plateforme a choisie (préférence
 * du paquet, ADR-017) et la pilote par cette seule surface.
 *
 * <p>Contrat commun aux deux voies : la publication (une analyse par
 * révision, jamais un lexique d'une autre révision ne colore un
 * document — PIEGE-20), la relance à la demande, la fenêtre visible
 * (la voie incrémentale y paie l'arbre d'abord, la voie entière
 * l'ignore : la borne fait la fenêtre), l'abonnement aux publications
 * et la libération idempotente. Les auditeurs partent sur le fil de
 * travail (ADR-007) — l'hôte y invalide.</p>
 *
 * <p>L'interface est close : elle porte exactement ce que le pont
 * appelle, rien d'autre. Un moteur qui exigerait plus que cette
 * surface n'entre pas dans le pont.</p>
 */
public interface MoteurSyntaxe extends AutoCloseable {

    /**
     * La dernière analyse publiée — null avant la première passe.
     *
     * @return l'analyse publiée, ou null
     */
    @Nullable Analysis analysePubliee();

    /**
     * L'analyse publiée pour la révision demandée — null si la
     * publication courante ne la sert pas exactement.
     *
     * @param revision la révision voulu
     * @return l'analyse à jour, ou null
     */
    @Nullable Analysis analyseDeRevision(long revision);

    /**
     * Relance l'analyse — la publication suit (le fil de travail si
     * un exécuteur y vit, le fil d'appel sinon).
     */
    void reanalyser();

    /**
     * Indique la fenêtre visible — la voie incrémentale y paie
     * l'arbre d'abord (G-01) ; le travail en vol ne s'annule pas.
     *
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     */
    void fenetre(int premiereLigne, int derniereLigne);

    /**
     * S'abonne aux publications — chaque analyse publiée y part.
     *
     * @param auditeur l'auditeur des publications, jamais null
     */
    void ajouterAuditeur(java.util.function.Consumer<Analysis> auditeur);

    /**
     * Referme le moteur : les écouteurs se détachent, l'état se
     * libère. Idempotent.
     */
    @Override
    void close();
}
