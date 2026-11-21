package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joni.Regex;
import org.jspecify.annotations.Nullable;

/**
 * Le lecteur des règles d'une grammaire au format TextMate réel
 * (ADR-004 révisé, ADR-014) : les grammaires se chargent telles que les
 * extensions de VS Code les publient — captures en objets portant un
 * nom, inclusions du référentiel résolues en références (les listes se
 * parcourent au fil de la lexicographie, chaque liste d'un contexte ne
 * se considère qu'une fois), {@code $self} et {@code $base} pointent la
 * racine, les inclusions de portée externe se résolvent par le registre
 * du chargeur. Les formes que le moteur ne porte pas encore se
 * comptent au rapport, jamais en silence : règles {@code while}
 * dégradées, sous-motifs de capture réduits au nom, drapeaux tolérés.
 * Un motif invalide écarte sa règle — comme
 * la référence — une forme catastrophique visible rejette le paquet.
 */
final class GrammarReader {

    private final Map<String, ListRule> referentiel;
    private final ListRule racine;
    private final Map<String, Grammar> externes;
    private final Map<String, ListRule> externesLiees = new HashMap<>();
    private final GrammarReport.Tisseur report;
    /** La moisson des mots réservés (complétion de base) : chaque
     *  règle keyword au motif littéral y verse au chargement. */
    private final java.util.Set<String> keywords = new java.util.LinkedHashSet<>();

    GrammarReader(Map<String, ListRule> referentiel, ListRule racine,
            Map<String, Grammar> externes, GrammarReport.Tisseur report) {
        this.referentiel = referentiel;
        this.racine = racine;
        this.externes = externes;
        this.report = report;
    }

    /** Les mots réservés moissonnés au chargement — figés, triés
     *  sans casse ; la complétion de base les sert. */
    List<String> keywords() {
        return GrammarKeywords.figer(keywords);
    }

    /**
     * Les règles d'une entrée du référentiel : enveloppe
     * {@code patterns}, ou règle isolée. Une entrée qui porte
     * {@code begin} ou {@code match} est une règle COMPLÈTE —
     * l'inclusion y garde sa garde (l'audit coloration : le contexte
     * hover de csharp, {@code begin \A\u200B}, perdait sa garde à
     * l'inclusion et ses blocs à fin {@code (?!)} — jamais fermés par
     * conception — fuyaient au sommet : chaque {@code class} ouvrait
     * un bloc qui ne se fermait plus).
     *
     * @param brut le contenu de l'entrée
     * @param ou   le nom du contexte, pour les messages
     * @return les règles de l'entrée, figées
     */
    List<Rule> reglesDe(@Nullable Object brut, String ou) {
        if (brut instanceof Map<?, ?> champs && champs.containsKey("patterns")
                && !champs.containsKey("begin") && !champs.containsKey("match")
                && !champs.containsKey("include")) {
            return aplatir(champs.get("patterns"), ou);
        }
        List<Rule> une = new ArrayList<>(1);
        regle(brut, une, ou);
        return List.copyOf(une);
    }

    /** Aplatit une liste de motifs bruts : chaque élément devient règle ou référence. */
    List<Rule> aplatir(@Nullable Object brut, String ou) {
        if (!(brut instanceof List<?> elements)) {
            throw new IllegalArgumentException(ou + " : un tableau de règles est attendu");
        }
        List<Rule> regles = new ArrayList<>(elements.size());
        for (Object element : elements) {
            regle(element, regles, ou);
        }
        return List.copyOf(regles);
    }

    /** Construit une règle — motif, bloc, inclusion, ou conteneur aplati. */
    private void regle(@Nullable Object brut, List<Rule> output, String ou) {
        if (!(brut instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException(ou + " : les règles sont des objets");
        }
        Object inclusion = champs.get("include");
        if (inclusion != null) {
            ListRule liee = inclusion(text(inclusion, ou + " include"));
            if (liee != null) {
                output.add(liee);
            }
            return;
        }
        if (champs.containsKey("begin")) {
            bloc(champs, output, ou);
            return;
        }
        if (champs.containsKey("match")) {
            simple(champs, output, ou);
            return;
        }
        if (champs.containsKey("patterns")) {
            output.addAll(aplatir(champs.get("patterns"), ou));
        }
    }

    /** Construit un motif simple, écarté si son expression est invalide. */
    private void simple(Map<?, ?> champs, List<Rule> output, String ou) {
        String source = text(champs.get("match"), ou + " match");
        Regex pattern = tenter(source);
        if (pattern == null) {
            return;
        }
        String portee = nom(champs);
        // La moisson au fil du chargement : la portée keyword au motif
        // littéral verse ses mots à la complétion de base.
        GrammarKeywords.moissonner(source, portee, keywords);
        output.add(new SimpleRule(pattern, portee,
                captures(champs.get("captures"), ou)));
    }

    /**
     * Construit un bloc début/fin. La règle {@code begin/while} sans
     * fermeture se dégrade en motif simple — comptée, jamais silencieuse.
     */
    private void bloc(Map<?, ?> champs, List<Rule> output, String ou) {
        String sourceDebut = text(champs.get("begin"), ou + " begin");
        if (champs.containsKey("while") && !champs.containsKey("end")) {
            whileDegrade(champs, output, sourceDebut);
            return;
        }
        String sourceFin = text(champs.get("end"), ou + " end");
        Regex start = tenter(sourceDebut);
        boolean references = references(sourceFin);
        Regex end = tenter(references ? Expressions.modele(sourceFin) : sourceFin);
        if (start == null || end == null) {
            return;
        }
        drapeaux(champs);
        output.add(new BlockRule(start, sourceFin, end, references,
                nom(champs), contenu(champs),
                captures(champs.get("beginCaptures"), ou),
                captures(champs.get("endCaptures"), ou),
                champs.containsKey("patterns")
                        ? aplatir(champs.get("patterns"), ou + " bloc") : List.of()));
    }

    /** La règle while dégradée : l'ouverture devient motif, comptée au rapport. */
    private void whileDegrade(Map<?, ?> champs, List<Rule> output, String sourceDebut) {
        Regex start = tenter(sourceDebut);
        report.regleWhile();
        if (start == null) {
            return;
        }
        output.add(new SimpleRule(start, nom(champs),
                captures(champs.get("beginCaptures"), ou(champs))));
    }

    /** Les drapeaux tolérés : while en surnombre, applyEndPatternLast. */
    private void drapeaux(Map<?, ?> champs) {
        if (champs.containsKey("while") || champs.containsKey("applyEndPatternLast")) {
            report.regleDrapeau();
        }
    }

    /**
     * Résout une inclusion : {@code #clé} du référentiel (inconnue :
     * sautée et comptée — les grammaires du commerce s'en portent
     * auteurs, VS Code les tolère aussi ; le banc des paquets intégrés
     * exige leur absence), {@code $self} et {@code $base} la racine,
     * une portée externe par le registre du chargeur (absente :
     * l'inclusion se saute, comptée, comme la référence tolère).
     */
    private @Nullable ListRule inclusion(String target) {
        if (target.startsWith("#")) {
            ListRule liste = referentiel.get(target.substring(1));
            if (liste == null) {
                report.inclusionInterne();
                return null;
            }
            return liste;
        }
        if ("$self".equals(target) || "$base".equals(target)) {
            return racine;
        }
        return externe(target);
    }

    /** L'inclusion externe liée, par portée racine de la grammaire visée. */
    private @Nullable ListRule externe(String portee) {
        ListRule liee = externesLiees.get(portee);
        if (liee != null) {
            return liee;
        }
        Grammar grammaire = externes.get(portee);
        if (grammaire == null) {
            report.inclusionExterieure();
            return null;
        }
        ListRule nouvelle = new ListRule();
        nouvelle.fixer(grammaire.patterns());
        externesLiees.put(portee, nouvelle);
        return nouvelle;
    }

    /** Les captures en objets : un numéro vers son nom de portée. */
    private @Nullable Map<Integer, Capture> captures(@Nullable Object brut, String ou) {
        if (brut == null) {
            return null;
        }
        if (!(brut instanceof Map<?, ?> groupes)) {
            throw new IllegalArgumentException(ou + " : les captures sont des objets");
        }
        Map<Integer, Capture> captures = new TreeMap<>();
        for (Map.Entry<?, ?> input : groupes.entrySet()) {
            captures.put(number(String.valueOf(input.getKey()), ou),
                    capture(input.getValue(), ou));
        }
        return captures.isEmpty() ? null : captures;
    }

    /** Une capture : son nom ; les sous-motifs éventuels se dégradent, comptés. */
    private Capture capture(@Nullable Object brut, String ou) {
        if (!(brut instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException(
                    ou + " : la capture « " + brut + " » doit être un objet");
        }
        if (champs.containsKey("patterns")) {
            report.captureSousMotifs();
        }
        Object nom = champs.get("name");
        return nom == null ? Capture.SANS_PORTEE : new Capture(text(nom, ou));
    }

    /** Le numéro d'une capture, entier positif. */
    private static int number(String cle, String ou) {
        try {
            return Integer.parseInt(cle);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ou + " : numéro de capture invalide « " + cle + " »");
        }
    }

    /**
     * Compile une expression en comptant au rapport : une forme
     * catastrophique visible ÉCARTE SA RÈGLE — comptée comme les
     * motifs invalides, jamais le paquet entier (H-05 : les grammaires
     * du commerce portent des formes que l'analyse textuelle refuse à
     * tort ; la règle absente ne colore rien, le budget d'exécution
     * garde le reste) ; un motif invalide écarte sa règle aussi.
     */
    private @Nullable Regex tenter(String pattern) {
        if (CatastrophicGuard.catastrophique(pattern)) {
            report.refuse();
            return null;
        }
        try {
            Regex compilee = Expressions.compiler(pattern);
            report.accepte();
            return compilee;
        } catch (IllegalArgumentException e) {
            report.refuse();
            return null;
        }
    }

    /** Vrai si la fermeture référence les captures de l'ouverture. */
    private static boolean references(String source) {
        for (int i = 0; i + 1 < source.length(); i++) {
            if (source.charAt(i) == '\\' && source.charAt(i + 1) >= '1'
                    && source.charAt(i + 1) <= '9') {
                return true;
            }
        }
        return false;
    }

    /** Le nom de portée d'une règle, null si absent. */
    private static @Nullable String nom(Map<?, ?> champs) {
        Object nom = champs.get("name");
        return nom == null ? null : String.valueOf(nom);
    }

    /** Le nom de contenu d'une règle, null si absent. */
    private static @Nullable String contenu(Map<?, ?> champs) {
        Object contenu = champs.get("contentName");
        return contenu == null ? null : String.valueOf(contenu);
    }

    /** Le contexte d'une règle, pour ses messages. */
    private static String ou(Map<?, ?> champs) {
        Object nom = champs.get("name");
        return nom == null ? "règle" : String.valueOf(nom);
    }

    /** Exige une chaîne non nulle. */
    private static String text(@Nullable Object brut, String ou) {
        if (brut == null) {
            throw new IllegalArgumentException(ou + " : une chaîne est attendue");
        }
        return String.valueOf(brut);
    }
}
