package jo.celeris.core.syntax.treesitter;

import jo.celeris.core.syntax.SyntaxEngine;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.Analysis;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.itsaky.androidide.treesitter.TSLanguage;
import com.itsaky.androidide.treesitter.TSParser;
import com.itsaky.androidide.treesitter.TSTree;

import java.util.logging.Logger;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le moteur du niveau 2 (ADR-020, révisé par l'ADR-0025 : la liaison
 * AndroidIDE vit par Maven) : la source native y analyse le document
 * ENTIER — l'arbre de la passe précédente sert le re-parse incrémental
 * (l'édition s'y rapporte, l'arbre s'y édite), la matérialisation Java
 * en sort le lexique et les chunks. La VOIE EST BORNÉE : au-delà de
 * deux cent kilo-octets, l'arbre natif ne survit pas à la passe (la
 * discipline mémoire de l'ADR — la re-analyse s'y paie au plein, les
 * budgets de l'ADR-004 ne tiennent que sous la borne).
 *
 * <p>La publication suit la voie du socle : sans exécuteur, elle se
 * paie sur le fil d'appel (la première demande la sert) ; avec
 * exécuteur (ADR-007), le travail s'y poste et les auditeurs y
 * partent — l'hôte y invalide. La fenêtre visible ne la concerne
 * jamais : la voie sert l'entier, la borne fait la fenêtre.</p>
 *
 * <p>Les positions vivent en offsets du modèle (UTF-16, ADR-0025) :
 * l'édition incrémentale se rapporte telle quelle — la liaison
 * reçoit ses unités doublées à la frontière {@link LiaisonArbre}.</p>
 */
public final class NativeEngine implements SyntaxEngine {

    /** Le journal de la voie — l'API du JDK, pas System.Logger : la
     *  seconde manque sur appareil avant le niveau 33 (R8 l'a révélé
     *  à la réduction — même discipline que le lexeur des grammaires). */
    private static final Logger JOURNAL =
            Logger.getLogger(NativeEngine.class.getName());

    /** La borne de la voie, en CARACTÈRES du modèle (audit D12 : le
     *  nom disait octets, la comparaison compte des caractères —
     *  {@code text.length()}) — le régime mesuré du spike : au-delà,
     *  l'arbre ne survit pas. */
    static final int BORNE_CARACTERES = 200 * 1024;

    private final TextSession session;
    private final @Nullable Executor executor;
    private final TSParser parser;
    private final TSLanguage language;
    private final GrammaireNatif grammaire;
    private final List<Consumer<Analysis>> auditeurs = new ArrayList<>();
    private final EditListener ecouteur = this::surEdition;

    /** L'arbre de la passe précédente — null sans (borné ou abandonné). */
    private @Nullable TSTree arbrePrecedent;
    /** Le document de la dernière passe — l'AVANT de l'édition suivante. */
    private @Nullable Document documentAnalyse;
    /** L'édition en attente de rapport : ses bornes, ses textes. */
    private int debutEdite;
    private int finEdite;
    private String retireEdite = "";
    private String insereEdite = "";
    private boolean editionEnAttente;
    private @Nullable Analysis published;
    /** Une passe attend son tour sur l'exécuteur — la rafale de
     *  frappes n'y empile pas : la dernière édition reprogramme la
     *  même passe, elle servira le document le plus récent. */
    private boolean passeEnAttente;
    /** Le moteur refermé ne reprogramme plus rien (C2 : aucun fil
     *  d'analyse vivant derrière la vue morte). */
    private boolean ferme;

    private NativeEngine(TextSession session, SyntaxeNatif source,
            @Nullable Executor executor) {
        this.session = session;
        this.executor = executor;
        this.grammaire = source.grammaire();
        this.language = grammaire.langue().langue();
        this.parser = TSParser.create();
        this.parser.setLanguage(language);
        session.addEditListener(ecouteur);
    }

    /**
     * Le moteur d'une session — null sans liaison chargée.
     *
     * @param session   la session d'édition suivie
     * @param source    la source native du langage
     * @param executor l'exécuteur du travail, null pour la voie synchrone
     * @return le moteur prêt, null sans liaison
     */
    public static @Nullable NativeEngine on(TextSession session,
            SyntaxeNatif source, @Nullable Executor executor) {
        if (session == null || !LiaisonArbre.presente()) {
            return null;
        }
        return new NativeEngine(session, source, executor);
    }

    /**
     * Le moteur de la voie du niveau 2 : null quand la source n'est pas
     * native, que la session manque ou que la liaison manque — le
     * pont y branche sans lire la source lui-même.
     *
     * @param source    la source syntaxique du langage, ou null
     * @param session   la session d'édition suivie
     * @param executor l'exécuteur du travail, null pour la voie synchrone
     * @return le moteur prêt, null hors de la voie
     */
    public static @Nullable NativeEngine of(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executor) {
        return source instanceof SyntaxeNatif natif
                ? on(session, natif, executor) : null;
    }

    /** La publication courante — null avant la première passe. */
    @Override
    public synchronized @Nullable Analysis analysePubliee() {
        return published;
    }

    /**
     * La publication de la révision donnée — null sans publication ou
     * si la publication date d'une autre révision : jamais le lexique
     * d'une autre révision ne se sert (PIEGE-20).
     *
     * @param revision la révision de l'instantané affiché
     * @return la publication à jour, ou null
     */
    @Override
    public synchronized @Nullable Analysis analyseDeRevision(long revision) {
        Analysis courante = published;
        return courante != null && courante.revision() == revision
                ? courante : null;
    }

    /** Re-analyse le document courant : l'incrémental s'y sert de
     *  l'arbre précédent quand l'édition s'y rapporte. */
    @Override
    public void reanalyser() {
        Executor thread = executor;
        if (thread != null) {
            thread.execute(this::analyser);
        } else {
            analyser();
        }
    }

    /** La fenêtre visible ne concerna jamais la voie : elle sert l'entier. */
    @Override
    public void window(int premiereLigne, int derniereLigne) {
        // la voie analyse le document entier — la borne fait la fenêtre
    }

    /**
     * L'auditeur des publications : chaque analyse y part, sur le fil
     * qui l'a servie.
     *
     * @param auditeur le consommateur d'analyses
     */
    @Override
    public synchronized void addListener(Consumer<Analysis> auditeur) {
        auditeurs.add(auditeur);
    }

    /** Referme le moteur : l'arbre et l'analyseur se libèrent. */
    @Override
    public void close() {
        synchronized (this) {
            ferme = true;
        }
        session.removeEditListener(ecouteur);
        TSTree arbre = arbrePrecedent;
        if (arbre != null) {
            arbre.close();
            arbrePrecedent = null;
        }
        parser.close();
    }

    /**
     * L'édition s'enregistre — puis REPROGRAMME la passe (feuille de
     * route A1.1) : la voie du socle poste son travail à chaque
     * édition, la voie native faisait attendre un appel externe de
     * {@code reanalyser()} que le pipeline réel ne fait jamais — la
     * publication périmait à la première frappe, pour toujours.
     * Une seule vit en attente : la deuxième qui arrive abandonne
     * l'arbre (le re-parse repart au plein, la composition d'éditions
     * ne se devine jamais).
     */
    private synchronized void surEdition(int start, int end, String removed,
            String inserted) {
        if (ferme) {
            return;
        }
        if (editionEnAttente && arbrePrecedent != null) {
            arbrePrecedent.close();
            arbrePrecedent = null;
        }
        debutEdite = start;
        finEdite = end;
        retireEdite = removed;
        insereEdite = inserted;
        editionEnAttente = true;
        programmer();
    }

    /**
     * La passe part sur l'exécuteur si elle n'y attend pas déjà — la
     * rafale de frappes ne l'y empile pas (une en attente suffit : la
     * passe lit TOUJOURS le document courant, elle servira la révision
     * la plus récente au moment de tourner). Sans exécuteur, la voie
     * synchrone reste servie par le {@code reanalyser()} du pont de
     * vue, qui le déclenche déjà à l'attachement et au rattrapage.
     * Appelé sous moniteur.
     */
    private void programmer() {
        Executor thread = executor;
        if (thread == null || passeEnAttente) {
            return;
        }
        passeEnAttente = true;
        thread.execute(() -> {
            try {
                analyser();
            } finally {
                synchronized (NativeEngine.this) {
                    passeEnAttente = false;
                    // Une édition arrivée PENDANT la passe redéclenche :
                    // la publication servie date peut-être déjà.
                    if (!ferme && editionEnAttente) {
                        programmer();
                    }
                }
            }
        });
    }

    /** La passe d'analyse : l'arbre précédent sert l'incrémental, la
     *  matérialisation Java en sort l'analyse, la borne décide de la
     *  survie de l'arbre. L'échec de parse NE FIGE PLUS la vue
     *  (feuille de route A1.3) : l'incident se journalise, la
     *  publication précédente reste, les auditeurs partent prévenus —
     *  la vue quitte son attente de couleur au lieu d'attendre un
     *  résultat qui ne viendra jamais. La REQUÊTE de surbrillance du
     *  langage s'y installe à la première passe (la teinte des
     *  feuilles vient de SA requête, ADR-0033). */
    private synchronized void analyser() {
        if (ferme) {
            return;
        }
        Document document = session.document();
        String text = document.text();
        TSTree arbre = parser(document, text);
        if (arbre == null) {
            JOURNAL.warning("la passe native n'a pas produit d'arbre — "
                    + "la publication precedente reste servie (document de "
                    + text.length() + " caracteres)");
            notifyListeners();
            return;
        }
        MaterialisationNatif.installerRequete(grammaire.id(),
                grammaire.requete());
        int[] aplatit = LiaisonArbre.aplatir(arbre, language);
        published = MaterialisationNatif.of(document, aplatit,
                LiaisonArbre.languageNames(language), document.revision(),
                grammaire.id());
        documentAnalyse = document;
        if (text.length() <= BORNE_CARACTERES) {
            arbrePrecedent = arbre;
        } else {
            arbre.close();
            arbrePrecedent = null;
        }
        editionEnAttente = false;
        notifyListeners();
    }

    /** La passe elle-même : l'incrémental quand l'arbre précédent vit
     *  et que l'édition s'y rapporte, le plein sinon. */
    private @Nullable TSTree parser(Document document, String text) {
        TSTree precedent = arbrePrecedent;
        Document avant = documentAnalyse;
        if (precedent == null || !editionEnAttente || avant == null) {
            return parser.parseString(text);
        }
        int nouvelleFin = debutEdite + insereEdite.length();
        precedent.edit(LiaisonArbre.edit(debutEdite, finEdite,
                nouvelleFin,
                lineOf(avant, debutEdite), colonneDe(avant, debutEdite),
                lineOf(avant, finEdite), colonneDe(avant, finEdite),
                lineOf(document, nouvelleFin),
                colonneDe(document, nouvelleFin)));
        TSTree arbre = parser.parseString(precedent, text);
        precedent.close();
        arbrePrecedent = null;
        return arbre;
    }

    /** La ligne d'un caractère dans le document donné. */
    private static int lineOf(Document document, int caractere) {
        return document.lineForOffset(Math.min(caractere,
                Math.max(0, document.length() - 1)));
    }

    /** La colonne (caractères) d'un caractère dans le document donné. */
    private static int colonneDe(Document document, int caractere) {
        int borne = Math.min(caractere, Math.max(0, document.length() - 1));
        return borne - document.lineStart(document.lineForOffset(borne));
    }

    /** Les auditeurs partent sur le fil qui a servi la passe. */
    private void notifyListeners() {
        Analysis published = this.published;
        if (published == null) {
            return;
        }
        for (Consumer<Analysis> auditeur : List.copyOf(auditeurs)) {
            auditeur.accept(published);
        }
    }
}
