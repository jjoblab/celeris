package jo.celeris.core.syntax;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le moteur du niveau 2 (ADR-020) : la source native y analyse le
 * document ENTIER — l'arbre de la passe précédente sert le re-parse
 * incrémental (l'édition s'y rapporte, l'arbre s'y édite), la
 * matérialisation Java en sort le lexique et les chunks. La VOIE EST
 * BORNÉE : au-delà de deux cent kilo-octets, l'arbre natif ne survit
 * pas à la passe (la discipline mémoire de l'ADR — la re-analyse s'y
 * paie au plein, les budgets de l'ADR-004 ne tiennent que sous la
 * borne).
 *
 * <p>La publication suit la voie du socle : sans exécuteur, elle se
 * paie sur le fil d'appel (la première demande la sert) ; avec
 * exécuteur (ADR-007), le travail s'y poste et les auditeurs y
 * partent — l'hôte y invalide. La fenêtre visible ne la concerne
 * jamais : la voie sert l'entier, la borne fait la fenêtre.</p>
 */
public final class MoteurNatif {

    /** La borne de la voie, en octets — le régime mesuré du spike
     *  (0,5 ms incrémental à 190 Ko) : au-delà, l'arbre ne survit pas. */
    static final int BORNE_OCTETS = 200 * 1024;

    private final TextSession session;
    private final @Nullable Executor executeur;
    private final long parser;
    private final String[] symboles;
    private final List<Consumer<Analysis>> auditeurs = new ArrayList<>();
    private final EditListener ecouteur = this::surEdition;

    /** L'arbre de la passe précédente — 0 sans (borné ou abandonné). */
    private long arbrePrecedent;
    /** Le document de la dernière passe — l'AVANT de l'édition suivante. */
    private @Nullable Document documentAnalyse;
    /** L'édition en attente de rapport : ses bornes, ses textes. */
    private int debutEdite;
    private int finEdite;
    private String retireEdite = "";
    private String insereEdite = "";
    private boolean editionEnAttente;
    private @Nullable Analysis publiee;

    private MoteurNatif(TextSession session, SyntaxeNatif source,
            @Nullable Executor executeur) {
        this.session = session;
        this.executeur = executeur;
        this.parser = LiaisonArbre.parserNew(source.langue());
        this.symboles = LiaisonArbre.symboles(parser);
        session.addEditListener(ecouteur);
    }

    /**
     * Le moteur d'une session — null sans bibliothèque partagée.
     *
     * @param session   la session d'édition suivie
     * @param source    la source native du langage
     * @param executeur l'exécuteur du travail, null pour la voie synchrone
     * @return le moteur prêt, null sans bibliothèque
     */
    public static @Nullable MoteurNatif sur(TextSession session,
            SyntaxeNatif source, @Nullable Executor executeur) {
        if (session == null || !LiaisonArbre.presente()) {
            return null;
        }
        return new MoteurNatif(session, source, executeur);
    }

    /**
     * Le moteur de la voie du niveau 2 : null quand la source n'est pas
     * native, que la session manque ou que la bibliothèque absente — le
     * pont y branche sans lire la source lui-même.
     *
     * @param source    la source syntaxique du langage, ou null
     * @param session   la session d'édition suivie
     * @param executeur l'exécuteur du travail, null pour la voie synchrone
     * @return le moteur prêt, null hors de la voie
     */
    public static @Nullable MoteurNatif de(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executeur) {
        return source instanceof SyntaxeNatif natif
                ? sur(session, natif, executeur) : null;
    }

    /** La publication courante — null avant la première passe. */
    public synchronized @Nullable Analysis analysePubliee() {
        return publiee;
    }

    /**
     * La publication de la révision donnée — null sans publication ou
     * si la publication date d'une autre révision : jamais le lexique
     * d'une autre révision ne se sert (PIEGE-20).
     *
     * @param revision la révision de l'instantané affiché
     * @return la publication à jour, ou null
     */
    public synchronized @Nullable Analysis analyseDeRevision(int revision) {
        Analysis courante = publiee;
        return courante != null && courante.revision() == revision
                ? courante : null;
    }

    /** Re-analyse le document courant : l'incrémental s'y sert de
     *  l'arbre précédent quand l'édition s'y rapporte. */
    public void reanalyser() {
        Executor fil = executeur;
        if (fil != null) {
            fil.execute(this::analyser);
        } else {
            analyser();
        }
    }

    /** La fenêtre visible ne concerna jamais la voie : elle sert l'entier. */
    public void fenetre(int premiereLigne, int derniereLigne) {
        // la voie analyse le document entier — la borne fait la fenêtre
    }

    /**
     * L'auditeur des publications : chaque analyse y part, sur le fil
     * qui l'a servie.
     *
     * @param auditeur le consommateur d'analyses
     */
    public synchronized void ajouterAuditeur(Consumer<Analysis> auditeur) {
        auditeurs.add(auditeur);
    }

    /** Referme le moteur : l'arbre et l'analyseur se libèrent. */
    public void close() {
        session.removeEditListener(ecouteur);
        if (arbrePrecedent != 0) {
            LiaisonArbre.treeFree(arbrePrecedent);
            arbrePrecedent = 0;
        }
        LiaisonArbre.parserFree(parser);
    }

    /**
     * L'édition s'enregistre : une seule vit en attente — la deuxième
     * qui arrive abandonne l'arbre (le re-parse repart au plein, la
     * composition d'éditions ne se devine jamais).
     */
    private synchronized void surEdition(int start, int end, String removed,
            String inserted) {
        if (editionEnAttente) {
            if (arbrePrecedent != 0) {
                LiaisonArbre.treeFree(arbrePrecedent);
                arbrePrecedent = 0;
            }
        }
        debutEdite = start;
        finEdite = end;
        retireEdite = removed;
        insereEdite = inserted;
        editionEnAttente = true;
    }

    /** La passe d'analyse : l'arbre précédent sert l'incrémental, la
     *  matérialisation Java en sort l'analyse, la borne décide de la
     *  survie de l'arbre. */
    private synchronized void analyser() {
        Document document = session.document();
        byte[] octets = document.text().getBytes(StandardCharsets.UTF_8);
        long arbre = parser(document, octets);
        if (arbre == 0) {
            publiee = null;
            return;
        }
        int[] aplatit = LiaisonArbre.aplatir(arbre);
        publiee = MaterialisationNatif.de(document, aplatit, symboles,
                document.revision());
        documentAnalyse = document;
        if (octets.length <= BORNE_OCTETS) {
            arbrePrecedent = arbre;
        } else {
            LiaisonArbre.treeFree(arbre);
            arbrePrecedent = 0;
        }
        editionEnAttente = false;
        notifier();
    }

    /** La passe elle-même : l'incrémental quand l'arbre précédent vit
     *  et que l'édition s'y rapporte, le plein sinon. */
    private long parser(Document document, byte[] octets) {
        long precedent = arbrePrecedent;
        Document avant = documentAnalyse;
        if (precedent == 0 || !editionEnAttente || avant == null) {
            return LiaisonArbre.parse(parser, octets, octets.length);
        }
        LiaisonArbre.treeEdit(precedent,
                octetsDe(avant, 0, debutEdite),
                octetsDe(avant, 0, debutEdite)
                        + retireEdite.getBytes(StandardCharsets.UTF_8).length,
                octetsDe(avant, 0, debutEdite)
                        + insereEdite.getBytes(StandardCharsets.UTF_8).length,
                ligneDe(avant, debutEdite),
                octetsDe(avant, ligneDe(avant, debutEdite), debutEdite),
                ligneDe(avant, finEdite),
                octetsDe(avant, ligneDe(avant, finEdite), finEdite),
                ligneDe(document, debutEdite + insereEdite.length()),
                octetsDe(document,
                        ligneDe(document, debutEdite + insereEdite.length()),
                        debutEdite + insereEdite.length()));
        long arbre = LiaisonArbre.reparse(parser, precedent, octets,
                octets.length);
        LiaisonArbre.treeFree(precedent);
        arbrePrecedent = 0;
        return arbre;
    }

    /** La ligne d'un caractère dans le document donné. */
    private static int ligneDe(Document document, int caractere) {
        return document.lineForOffset(Math.min(caractere,
                Math.max(0, document.length() - 1)));
    }

    /** Les octets d'une tranche de document, préfixe compris. */
    private static int octetsDe(Document document, int debut, int fin) {
        int borne = Math.min(fin, document.length());
        return document.subText(debut, Math.max(debut, borne))
                .getBytes(StandardCharsets.UTF_8).length;
    }

    /** Les auditeurs partent sur le fil qui a servi la passe. */
    private void notifier() {
        Analysis publiee = this.publiee;
        if (publiee == null) {
            return;
        }
        for (Consumer<Analysis> auditeur : List.copyOf(auditeurs)) {
            auditeur.accept(publiee);
        }
    }
}
