package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Opérations de recherche/remplacement : modes insensible à la casse, mot
 * entier et expressions régulières — portage du moteur du dépôt de départ
 * (registre de portage), dont les tests sont l'oracle.
 *
 * <p><b>Recherche regex SÛRE</b> (garde-fou du dépôt de départ) : le mode
 * regex est enveloppé d'un budget temps et d'un plafond de résultats. Une
 * expression à retour sur soi-même catastrophique ({@code (a+)+b} sur une
 * longue suite de « a ») ne fige plus le thread appelant : la vue
 * {@link GuardedCharSequence} interrompt le moteur au-delà du budget
 * ({@link #REGEX_BUDGET_MS}) et le résultat porte l'état
 * {@code tooCostly} ; au-delà de {@link #MAX_MATCHES} correspondances, la
 * collecte s'arrête et le résultat porte {@code truncated}. Les états sont
 * exposés pour signal à l'interface.</p>
 *
 * <p><b>Horloge injectée</b> (ADR-007) : le moteur reçoit une horloge en
 * millisecondes croissantes — il n'appelle jamais l'horloge système ; les
 * tests obtiennent un temps déterministe. La <b>casse insensible</b>
 * passe par la minuscule complète du texte et de la requête (voie rapide,
 * comportement hérité) ; quand cette minuscule change une longueur (cas
 * exotique tel U+0130), le repli par comparaison régionale conserve les
 * indices du texte original (correctif consigné au registre de portage).</p>
 *
 * <p>La passe de <b>remplacement</b> spécifie le comportement du
 * contrôleur du dépôt de départ : segments d'origine + remplacements
 * développés (groupes {@code $n} en mode regex) ; motif invalide ou trop
 * coûteux → AUCUN remplacement, le texte n'est jamais muté à moitié. Les
 * correspondances de remplacement ne se recouvrent jamais (adaptation
 * consignée) : la collecte peut recouvrir pour la surbrillance, la passe
 * avance au-delà de chaque occurrence remplacée.</p>
 */
public final class FindReplace {

    /**
     * Plafond de correspondances collectées — au-delà, la recherche est
     * tronquée (état {@code truncated} dans {@link SearchResult}).
     */
    public static final int MAX_MATCHES = 10_000;

    /**
     * Budget temps du mode regex (par recherche) — au-delà, la
     * correspondance est abandonnée (état {@code tooCostly}). Borné très
     * en dessous du seuil d'ANR : la frappe reste fluide.
     */
    public static final long REGEX_BUDGET_MS = 250;

    private final LongSupplier horloge;

    private FindReplace(LongSupplier horloge) {
        this.horloge = horloge;
    }

    /**
     * Crée le moteur sur l'horloge donnée.
     *
     * @param horloge l'horloge injectée, en millisecondes croissantes
     *                (ADR-007 : le moteur n'appelle jamais l'horloge système)
     * @return le moteur prêt à rechercher
     */
    public static FindReplace of(LongSupplier horloge) {
        return new FindReplace(Objects.requireNonNull(horloge, "horloge"));
    }

    /**
     * Résultat d'une recherche sûre : correspondances (bornées par
     * {@link #MAX_MATCHES}) et signaux d'état pour l'interface.
     *
     * @param matches   les correspondances trouvées, en ordre du document
     * @param truncated vrai si la collecte s'est arrêtée au plafond
     * @param tooCostly vrai si l'expression a dépassé le budget temps
     *                  (résultats partiels ou vides)
     */
    public record SearchResult(List<Match> matches, boolean truncated, boolean tooCostly) {

        /** Copie défensive : le résultat est immuable. */
        public SearchResult {
            matches = List.copyOf(matches);
        }
    }

    /**
     * Exception du garde-fou temps — levée par les matchers gardés
     * ({@link #guardedMatcher}) quand l'expression dépasse le budget ou que
     * le thread est interrompu. Publique : une passe de remplacement
     * l'attrape pour avorter proprement.
     */
    public static final class RegexBudgetExceededException extends RuntimeException {

        /** Identifiant de sérialisation (avertissement javac bloquant). */
        private static final long serialVersionUID = 1L;

        RegexBudgetExceededException(String message) {
            super(message);
        }
    }

    /**
     * Résultat d'une passe de remplacement : le texte reconstruit, les
     * éditions prêtes pour la voie de transaction et les signaux d'état.
     *
     * <p>Les offsets des éditions s'entendent dans le <em>texte d'origine</em>,
     * en ordre du document — la session les ajuste en deltas cumulés pour la
     * sémantique séquentielle de la {@link Transaction}. Une passe sans
     * remplacement (aucune occurrence, motif invalide, budget dépassé)
     * retourne des éditions vides et le texte d'origine. La passe est
     * plafonnée à {@link #MAX_MATCHES} remplacements — miroir du plafond de
     * collecte de la recherche : au-delà, les éditions s'arrêtent au
     * plafond, l'état {@code truncated} l'expose et le texte reconstruit
     * garde la queue d'origine (les éditions et le texte restent toujours
     * cohérents entre eux).</p>
     *
     * @param text      le texte reconstruit après remplacements
     * @param edits     les éditions, une par remplacement, en ordre du document
     * @param tooCostly vrai si la passe a avorté sur le budget temps
     * @param truncated vrai si la passe s'est arrêtée au plafond de remplacements
     */
    public record ReplaceResult(String text, List<TextEdit> edits, boolean tooCostly,
            boolean truncated) {

        /** Copie défensive : le résultat est immuable. */
        public ReplaceResult {
            edits = List.copyOf(edits);
        }

        /** Nombre de remplacements effectifs. */
        public int count() {
            return edits.size();
        }
    }

    /**
     * Trouve toutes les occurrences de la requête dans le texte — entrée
     * SÛRE : budget temps en mode regex, plafond de résultats dans tous les
     * modes, états {@code truncated} / {@code tooCostly} exposés pour
     * signal à l'interface.
     *
     * @param text  le texte parcouru
     * @param query la requête (jamais vide pour un résultat non vide)
     * @param opts  les options (casse, mot entier, regex)
     * @return le résultat de la recherche
     */
    public SearchResult find(CharSequence text, String query, FindOptions opts) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(opts, "opts");
        if (query.isEmpty()) {
            return new SearchResult(List.of(), false, false);
        }
        List<Match> matches = new ArrayList<>();
        boolean tronque;
        boolean tropCouteux = false;
        if (opts.regex()) {
            tropCouteux = findRegexGuarded(text, query, opts, matches);
            tronque = matches.size() >= MAX_MATCHES;
        } else {
            VueRecherche vue = vueRecherche(text, query, opts);
            if (opts.wholeWord()) {
                tronque = findWholeWord(vue, query, matches);
            } else {
                tronque = findPlain(vue, query, matches);
            }
        }
        return new SearchResult(matches, tronque, tropCouteux);
    }

    /**
     * Trouve toutes les occurrences de la requête dans le texte
     * (historique) — délègue à {@link #find} : la liste est bornée par
     * {@link #MAX_MATCHES}, les états d'avertissement ne sont pas
     * transportés.
     *
     * @param text  le texte parcouru
     * @param query la requête
     * @param opts  les options (casse, mot entier, regex)
     * @return les correspondances trouvées
     */
    public List<Match> findMatches(CharSequence text, String query, FindOptions opts) {
        return find(text, query, opts).matches();
    }

    /**
     * Remplace toutes les occurrences de la requête par le remplacement
     * donné : segments d'origine + remplacements développés (groupes
     * {@code $n} en mode regex). Un motif invalide, un remplacement mal
     * formé ou un budget dépassé ne remplacent RIEN — le texte n'est jamais
     * muté à moitié. La passe est plafonnée à {@link #MAX_MATCHES}
     * remplacements (état {@code truncated}), miroir du plafond de collecte
     * de la recherche.
     *
     * @param text        le texte parcouru
     * @param query       la requête
     * @param replacement le texte de remplacement (avec groupes {@code $n}
     *                    en mode regex)
     * @param opts        les options (casse, mot entier, regex)
     * @return le résultat de la passe (texte reconstruit, éditions, signaux)
     */
    public ReplaceResult replaceAll(CharSequence text, String query, String replacement,
            FindOptions opts) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(replacement, "replacement");
        Objects.requireNonNull(opts, "opts");
        String original = text.toString();
        if (query.isEmpty()) {
            return new ReplaceResult(original, List.of(), false, false);
        }
        if (opts.regex()) {
            return remplacerRegex(original, query, replacement, opts);
        }
        return remplacerLitteral(original, query, replacement, opts);
    }

    /**
     * Construction du {@link Pattern} — l'unique endroit : flags dérivés
     * des options (MULTILINE systématique, CASE_INSENSITIVE selon l'option).
     *
     * @param query la requête (expression régulière)
     * @param opts  les options
     * @return le motif compilé
     */
    public static Pattern compilePattern(String query, FindOptions opts) {
        int flags = Pattern.MULTILINE;
        if (opts != null && !opts.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(query, flags);
    }

    /**
     * Matcher GARDE-FOU pour les passes de remplacement — même budget que
     * la recherche : un motif catastrophique interrompt la passe par
     * {@link RegexBudgetExceededException} au lieu de figer le thread
     * appelant (l'appelant attrape et avorte proprement).
     *
     * @param pattern le motif compilé
     * @param text    le texte parcouru
     * @return le matcher enveloppé du garde-fou
     */
    public Matcher guardedMatcher(Pattern pattern, CharSequence text) {
        return pattern.matcher(new GuardedCharSequence(text, REGEX_BUDGET_MS, horloge));
    }

    /**
     * Trouve l'index de la correspondance à ou après la position du caret
     * donnée. Boucle au début si aucune correspondance n'est trouvée après
     * le caret.
     *
     * @param matches les correspondances, en ordre du document
     * @param caret   la position du caret
     * @return index de correspondance, ou -1 si aucune
     */
    public static int matchIndexFrom(List<Match> matches, int caret) {
        if (matches.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start() >= caret) {
                return i;
            }
        }
        return 0;
    }

    // ── Recherche littérale et mot entier ────────────────────────

    /** Collecte les correspondances littérales, qui peuvent se recouvrir. */
    private static boolean findPlain(VueRecherche vue, String query, List<Match> out) {
        int depuis = 0;
        while (depuis + query.length() <= vue.original().length()) {
            int idx = prochaineCandidate(vue, query, depuis);
            if (idx < 0) {
                return false;
            }
            out.add(new Match(idx, idx + query.length()));
            if (out.size() >= MAX_MATCHES) {
                return true;
            }
            depuis = idx + 1;
        }
        return false;
    }

    /** Collecte les correspondances littérales aux frontières de mot. */
    private static boolean findWholeWord(VueRecherche vue, String query, List<Match> out) {
        int depuis = 0;
        while (depuis + query.length() <= vue.original().length()) {
            int idx = prochaineCandidate(vue, query, depuis);
            if (idx < 0) {
                return false;
            }
            if (motEntier(vue, query.length(), idx)) {
                out.add(new Match(idx, idx + query.length()));
                if (out.size() >= MAX_MATCHES) {
                    return true;
                }
            }
            depuis = idx + 1;
        }
        return false;
    }

    // ── Recherche regex gardée ───────────────────────────────────

    /** Collecte les correspondances du motif gardé ; vrai si budget dépassé. */
    private boolean findRegexGuarded(CharSequence text, String query,
            FindOptions opts, List<Match> out) {
        try {
            Matcher matcher = guardedMatcher(compilePattern(query, opts), text);
            while (matcher.find()) {
                out.add(new Match(matcher.start(), matcher.end()));
                if (out.size() >= MAX_MATCHES) {
                    return false;
                }
            }
        } catch (PatternSyntaxException e) {
            // Regex invalide : aucune correspondance renvoyée (historique).
            return false;
        } catch (RegexBudgetExceededException e) {
            return true;
        }
        return false;
    }

    // ── Passe de remplacement ────────────────────────────────────

    /** Passe littérale (mot entier selon les options) : correspondances non recouvrantes, plafonnées. */
    private static ReplaceResult remplacerLitteral(String texte, String query,
            String remplacement, FindOptions opts) {
        VueRecherche vue = vueRecherche(texte, query, opts);
        StringBuilder sortie = new StringBuilder(texte.length());
        List<TextEdit> editions = new ArrayList<>();
        int copie = 0;
        int depuis = 0;
        boolean tronque = false;
        while (depuis + query.length() <= texte.length()) {
            int idx = prochaineCandidate(vue, query, depuis);
            if (idx < 0) {
                break;
            }
            if (opts.wholeWord() && !motEntier(vue, query.length(), idx)) {
                depuis = idx + 1;
                continue;
            }
            if (editions.size() >= MAX_MATCHES) {
                tronque = true;
                break;
            }
            sortie.append(texte, copie, idx).append(remplacement);
            editions.add(new TextEdit(idx, texte.substring(idx, idx + query.length()),
                    remplacement));
            copie = idx + query.length();
            depuis = copie;
        }
        sortie.append(texte, copie, texte.length());
        return new ReplaceResult(sortie.toString(), editions, false, tronque);
    }

    /**
     * Passe regex gardée : le matcher vit sur le texte d'origine,
     * {@code appendReplacement} développe les groupes à chaque occurrence
     * dans un tampon jetable. Le curseur de copie n'avance qu'à l'émission
     * (une correspondance de largeur nulle au remplacement vide ne copie
     * rien) tandis que la position d'ajout du matcher suit chaque
     * correspondance — la queue est copiée manuellement depuis le curseur,
     * jamais par {@code appendTail}.
     */
    private ReplaceResult remplacerRegex(String texte, String query,
            String remplacement, FindOptions opts) {
        StringBuilder sortie = new StringBuilder(texte.length());
        try {
            Matcher matcher = guardedMatcher(compilePattern(query, opts), texte);
            List<TextEdit> editions = new ArrayList<>();
            StringBuilder tampon = new StringBuilder(64);
            int copie = 0;
            int positionAppend = 0;
            boolean tronque = false;
            while (matcher.find()) {
                if (editions.size() >= MAX_MATCHES) {
                    tronque = true;
                    break;
                }
                int debut = matcher.start();
                tampon.setLength(0);
                matcher.appendReplacement(tampon, remplacement);
                String insere = tampon.substring(debut - positionAppend);
                String retire = matcher.group();
                if (!retire.isEmpty() || !insere.isEmpty()) {
                    sortie.append(texte, copie, debut).append(insere);
                    editions.add(new TextEdit(debut, retire, insere));
                    copie = matcher.end();
                }
                positionAppend = matcher.end();
            }
            sortie.append(texte, copie, texte.length());
            return new ReplaceResult(sortie.toString(), editions, false, tronque);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Motif ou remplacement invalide ($ orphelin, groupe absent) :
            // aucun remplacement — le texte n'est jamais muté à moitié.
            return new ReplaceResult(texte, List.of(), false, false);
        } catch (RegexBudgetExceededException e) {
            // Motif trop coûteux : aucun remplacement, signal exposé.
            return new ReplaceResult(texte, List.of(), true, false);
        }
    }

    // ── Vue de recherche (casse appliquée une seule fois) ────────

    /**
     * Vue de recherche : la casse est appliquée une seule fois. En voie
     * rapide, l'espace de recherche minuscule garde la longueur de
     * l'original (et l'aiguille la sienne) — les indices coïncident ;
     * sinon (une minuscule complète a changé une longueur), la recherche
     * insensible passe par comparaison régionale sur le texte original.
     */
    private record VueRecherche(String original, String espaceRecherche, String aiguille,
            boolean rapide) {
    }

    /** Construit la vue de recherche pour la requête et les options données. */
    private static VueRecherche vueRecherche(CharSequence text, String query, FindOptions opts) {
        String original = text.toString();
        if (opts.caseSensitive()) {
            return new VueRecherche(original, original, query, true);
        }
        String minuscule = original.toLowerCase(Locale.ROOT);
        String aiguille = query.toLowerCase(Locale.ROOT);
        boolean rapide = minuscule.length() == original.length()
                && aiguille.length() == query.length();
        return new VueRecherche(original, minuscule, aiguille, rapide);
    }

    /** Prochaine candidate à partir de {@code from}, -1 si aucune. */
    private static int prochaineCandidate(VueRecherche vue, String requeteOriginale, int from) {
        if (vue.rapide()) {
            return vue.espaceRecherche().indexOf(vue.aiguille(), from);
        }
        for (int i = from; i + requeteOriginale.length() <= vue.original().length(); i++) {
            if (vue.original().regionMatches(true, i, requeteOriginale, 0,
                    requeteOriginale.length())) {
                return i;
            }
        }
        return -1;
    }

    /** Frontières de mot du dépôt de départ : caractères non alphanumériques ou extrémités. */
    private static boolean motEntier(VueRecherche vue, int longueur, int idx) {
        boolean borneGauche = idx == 0
                || !Character.isLetterOrDigit(vue.original().charAt(idx - 1));
        boolean borneDroite = idx + longueur >= vue.original().length()
                || !Character.isLetterOrDigit(vue.original().charAt(idx + longueur));
        return borneGauche && borneDroite;
    }
}
