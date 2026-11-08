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
 * dégradées, sous-motifs de capture réduits au nom, drapeaux tolérés,
 * injections racine ignorées. Un motif invalide écarte sa règle — comme
 * la référence — une forme catastrophique visible rejette le paquet.
 */
final class LecteurGrammaire {

    private final Map<String, RegleListe> referentiel;
    private final RegleListe racine;
    private final Map<String, Grammaire> externes;
    private final Map<String, RegleListe> externesLiees = new HashMap<>();
    private final RapportGrammaire.Tisseur rapport;

    LecteurGrammaire(Map<String, RegleListe> referentiel, RegleListe racine,
            Map<String, Grammaire> externes, RapportGrammaire.Tisseur rapport) {
        this.referentiel = referentiel;
        this.racine = racine;
        this.externes = externes;
        this.rapport = rapport;
    }

    /**
     * Les règles d'une entrée du référentiel : enveloppe
     * {@code patterns}, ou règle isolée.
     *
     * @param brut le contenu de l'entrée
     * @param ou   le nom du contexte, pour les messages
     * @return les règles de l'entrée, figées
     */
    List<Regle> reglesDe(@Nullable Object brut, String ou) {
        if (brut instanceof Map<?, ?> champs && champs.containsKey("patterns")) {
            return aplatir(champs.get("patterns"), ou);
        }
        List<Regle> une = new ArrayList<>(1);
        regle(brut, une, ou);
        return List.copyOf(une);
    }

    /** Aplatit une liste de motifs bruts : chaque élément devient règle ou référence. */
    List<Regle> aplatir(@Nullable Object brut, String ou) {
        if (!(brut instanceof List<?> elements)) {
            throw new IllegalArgumentException(ou + " : un tableau de règles est attendu");
        }
        List<Regle> regles = new ArrayList<>(elements.size());
        for (Object element : elements) {
            regle(element, regles, ou);
        }
        return List.copyOf(regles);
    }

    /** Construit une règle — motif, bloc, inclusion, ou conteneur aplati. */
    private void regle(@Nullable Object brut, List<Regle> sortie, String ou) {
        if (!(brut instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException(ou + " : les règles sont des objets");
        }
        Object inclusion = champs.get("include");
        if (inclusion != null) {
            RegleListe liee = inclusion(texte(inclusion, ou + " include"), ou);
            if (liee != null) {
                sortie.add(liee);
            }
            return;
        }
        if (champs.containsKey("begin")) {
            bloc(champs, sortie, ou);
            return;
        }
        if (champs.containsKey("match")) {
            simple(champs, sortie, ou);
            return;
        }
        if (champs.containsKey("patterns")) {
            sortie.addAll(aplatir(champs.get("patterns"), ou));
        }
    }

    /** Construit un motif simple, écarté si son expression est invalide. */
    private void simple(Map<?, ?> champs, List<Regle> sortie, String ou) {
        Regex motif = tenter(texte(champs.get("match"), ou + " match"));
        if (motif == null) {
            return;
        }
        sortie.add(new RegleSimple(motif, nom(champs),
                captures(champs.get("captures"), ou)));
    }

    /**
     * Construit un bloc début/fin. La règle {@code begin/while} sans
     * fermeture se dégrade en motif simple — comptée, jamais silencieuse.
     */
    private void bloc(Map<?, ?> champs, List<Regle> sortie, String ou) {
        String sourceDebut = texte(champs.get("begin"), ou + " begin");
        if (champs.containsKey("while") && !champs.containsKey("end")) {
            whileDegrade(champs, sortie, sourceDebut);
            return;
        }
        String sourceFin = texte(champs.get("end"), ou + " end");
        Regex debut = tenter(sourceDebut);
        boolean references = references(sourceFin);
        Regex fin = tenter(references ? Expressions.modele(sourceFin) : sourceFin);
        if (debut == null || fin == null) {
            return;
        }
        drapeaux(champs);
        sortie.add(new RegleBloc(debut, sourceFin, fin, references,
                nom(champs), contenu(champs),
                captures(champs.get("beginCaptures"), ou),
                captures(champs.get("endCaptures"), ou),
                champs.containsKey("patterns")
                        ? aplatir(champs.get("patterns"), ou + " bloc") : List.of()));
    }

    /** La règle while dégradée : l'ouverture devient motif, comptée au rapport. */
    private void whileDegrade(Map<?, ?> champs, List<Regle> sortie, String sourceDebut) {
        Regex debut = tenter(sourceDebut);
        rapport.regleWhile();
        if (debut == null) {
            return;
        }
        sortie.add(new RegleSimple(debut, nom(champs),
                captures(champs.get("beginCaptures"), ou(champs))));
    }

    /** Les drapeaux tolérés : while en surnombre, applyEndPatternLast. */
    private void drapeaux(Map<?, ?> champs) {
        if (champs.containsKey("while") || champs.containsKey("applyEndPatternLast")) {
            rapport.regleDrapeau();
        }
    }

    /**
     * Résout une inclusion : {@code #clé} du référentiel (inconnue :
     * grammaire mal formée, refusée), {@code $self} et {@code $base} la
     * racine, une portée externe par le registre du chargeur (absente :
     * l'inclusion se saute, comptée, comme la référence tolère).
     */
    private @Nullable RegleListe inclusion(String cible, String ou) {
        if (cible.startsWith("#")) {
            RegleListe liste = referentiel.get(cible.substring(1));
            if (liste == null) {
                throw new IllegalArgumentException(ou + " : inclusion inconnue " + cible);
            }
            return liste;
        }
        if ("$self".equals(cible) || "$base".equals(cible)) {
            return racine;
        }
        return externe(cible);
    }

    /** L'inclusion externe liée, par portée racine de la grammaire visée. */
    private @Nullable RegleListe externe(String portee) {
        RegleListe liee = externesLiees.get(portee);
        if (liee != null) {
            return liee;
        }
        Grammaire grammaire = externes.get(portee);
        if (grammaire == null) {
            rapport.inclusionExterieure();
            return null;
        }
        RegleListe nouvelle = new RegleListe();
        nouvelle.fixer(grammaire.motifs());
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
        for (Map.Entry<?, ?> entree : groupes.entrySet()) {
            captures.put(numero(String.valueOf(entree.getKey()), ou),
                    capture(entree.getValue(), ou));
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
            rapport.captureSousMotifs();
        }
        Object nom = champs.get("name");
        return nom == null ? Capture.SANS_PORTEE : new Capture(texte(nom, ou));
    }

    /** Le numéro d'une capture, entier positif. */
    private static int numero(String cle, String ou) {
        try {
            return Integer.parseInt(cle);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ou + " : numéro de capture invalide « " + cle + " »");
        }
    }

    /**
     * Compile une expression en comptant au rapport : une forme
     * catastrophique visible rejette le paquet, un motif invalide écarte
     * sa règle.
     */
    private @Nullable Regex tenter(String motif) {
        if (GardeCatastrophique.catastrophique(motif)) {
            throw new IllegalArgumentException(
                    "forme catastrophique refusée (quantificateurs non bornés imbriqués) : "
                            + motif);
        }
        try {
            Regex compilee = Expressions.compiler(motif);
            rapport.accepte();
            return compilee;
        } catch (IllegalArgumentException e) {
            rapport.refuse();
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
    private static String texte(@Nullable Object brut, String ou) {
        if (brut == null) {
            throw new IllegalArgumentException(ou + " : une chaîne est attendue");
        }
        return String.valueOf(brut);
    }
}
