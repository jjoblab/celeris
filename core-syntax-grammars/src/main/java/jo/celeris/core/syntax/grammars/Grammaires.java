package jo.celeris.core.syntax.grammars;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jspecify.annotations.Nullable;

/**
 * Chargeur de grammaires (format TextMate, ADR-004) : lit le JSON
 * déclaratif, compile les expressions et résout les inclusions du
 * référentiel ({@code #cle}) en références — la liste de chaque clé
 * existe avant sa propre résolution, les inclusions cycliques (un
 * tableau de valeurs qui contient des tableaux) se référencent sans
 * fin d'aplatissement. {@code $self} et {@code $base} sont refusés :
 * l'aplatissement n'y trouverait jamais son compte.
 */
public final class Grammaires {

    private static final String CHEMIN = "/grammaires/";

    private Grammaires() {
    }

    /**
     * Lit une grammaire embarquée du module ({@code /grammaires/<nom>.json}).
     *
     * @param nom le nom du fichier, sans extension
     * @return la grammaire compilée
     * @throws IllegalArgumentException si le nom est inconnu ou la grammaire mal formée
     */
    public static Grammaire ressource(String nom) {
        InputStream flux = Grammaires.class.getResourceAsStream(CHEMIN + nom + ".json");
        if (flux == null) {
            throw new IllegalArgumentException("unknown grammar resource: " + nom);
        }
        byte[] octets;
        try (flux) {
            octets = flux.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return lire(new String(octets, StandardCharsets.UTF_8));
    }

    /**
     * Lit une grammaire depuis son texte JSON déclaratif.
     *
     * @param json le texte de la grammaire
     * @return la grammaire compilée, référentiel résolu en références
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    public static Grammaire lire(String json) {
        Object racine = LecteurJson.lire(json);
        if (!(racine instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException("grammar root must be an object");
        }
        String nom = texte(champs.get("name"), "name");
        String porteeRacine = texte(champs.get("scopeName"), "scopeName");
        Map<String, RegleListe> referentiel = new LinkedHashMap<>();
        Map<String, Object> bruts = bruts(champs.get("repository"));
        for (String cle : bruts.keySet()) {
            referentiel.put(cle, new RegleListe());
        }
        for (Map.Entry<String, Object> entree : bruts.entrySet()) {
            RegleListe liste = java.util.Objects.requireNonNull(
                    referentiel.get(entree.getKey()), "liste manquante");
            liste.fixer(reglesDe(entree.getValue(), referentiel, "#" + entree.getKey()));
        }
        List<Regle> motifs = aplatir(champs.get("patterns"), referentiel, "patterns");
        return new Grammaire(nom, porteeRacine, motifs);
    }

    /** Le référentiel brut : chaque clé vers son contenu non résolu. */
    private static Map<String, Object> bruts(@Nullable Object brut) {
        Map<String, Object> bruts = new LinkedHashMap<>();
        if (brut == null) {
            return bruts;
        }
        if (!(brut instanceof Map<?, ?> cles)) {
            throw new IllegalArgumentException("repository must be an object");
        }
        for (Map.Entry<?, ?> entree : cles.entrySet()) {
            if (!(entree.getKey() instanceof String cle)) {
                throw new IllegalArgumentException("repository keys must be strings");
            }
            bruts.put(cle, entree.getValue());
        }
        return bruts;
    }

    /**
     * Les règles d'une entrée du référentiel : enveloppe
     * {@code patterns}, ou règle isolée.
     */
    private static List<Regle> reglesDe(@Nullable Object brut,
            Map<String, RegleListe> referentiel, String ou) {
        if (brut instanceof Map<?, ?> champs && champs.containsKey("patterns")) {
            return aplatir(champs.get("patterns"), referentiel, ou);
        }
        List<Regle> une = new ArrayList<>(1);
        regle(brut, referentiel, une, ou);
        return List.copyOf(une);
    }

    /**
     * Aplatit une liste de motifs bruts : les inclusions deviennent des
     * références, les motifs et blocs se compilent.
     */
    private static List<Regle> aplatir(@Nullable Object brut,
            Map<String, RegleListe> referentiel, String ou) {
        if (!(brut instanceof List<?> elements)) {
            throw new IllegalArgumentException(ou + " must be an array");
        }
        List<Regle> regles = new ArrayList<>(elements.size());
        for (Object element : elements) {
            regle(element, referentiel, regles, ou);
        }
        return List.copyOf(regles);
    }

    /** Construit une règle — motif, bloc ou référence d'inclusion. */
    private static void regle(@Nullable Object brut, Map<String, RegleListe> referentiel,
            List<Regle> regles, String ou) {
        if (!(brut instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException(ou + " rules must be objects");
        }
        Object inclusion = champs.get("include");
        if (inclusion != null) {
            String cible = texte(inclusion, ou + " include");
            if (!cible.startsWith("#")) {
                throw new IllegalArgumentException(
                        ou + ": unsupported include " + cible);
            }
            RegleListe liste = referentiel.get(cible.substring(1));
            if (liste == null) {
                throw new IllegalArgumentException(
                        ou + ": unknown include " + cible);
            }
            regles.add(liste);
            return;
        }
        if (champs.containsKey("begin")) {
            regles.add(bloc(champs, referentiel, ou));
            return;
        }
        regles.add(new RegleSimple(
                expression(texte(champs.get("match"), ou + " match")),
                champs.get("name") == null ? "meta.rule" : texte(champs.get("name"), ou),
                captures(champs.get("captures"))));
    }

    /** Construit un bloc début/fin, motifs de contenu aplaties. */
    private static RegleBloc bloc(Map<?, ?> champs, Map<String, RegleListe> referentiel,
            String ou) {
        Pattern debut = expression(texte(champs.get("begin"), ou + " begin"));
        Pattern fin = expression(texte(champs.get("end"), ou + " end"));
        String portee = champs.get("name") == null
                ? "meta.block"
                : texte(champs.get("name"), ou);
        String contenu = champs.get("contentName") == null
                ? null
                : texte(champs.get("contentName"), ou);
        List<Regle> motifs = champs.get("patterns") == null
                ? List.of()
                : aplatir(champs.get("patterns"), referentiel, ou + " block");
        return new RegleBloc(debut, fin, portee, contenu,
                captures(champs.get("beginCaptures")), captures(champs.get("endCaptures")),
                motifs);
    }

    /** Les portées de capture par numéro de groupe, triées. */
    private static @Nullable Map<Integer, String> captures(@Nullable Object brut) {
        if (brut == null) {
            return null;
        }
        if (!(brut instanceof Map<?, ?> groupes)) {
            throw new IllegalArgumentException("captures must be an object");
        }
        Map<Integer, String> captures = new TreeMap<>();
        for (Map.Entry<?, ?> entree : groupes.entrySet()) {
            captures.put(Integer.parseInt(String.valueOf(entree.getKey())),
                    String.valueOf(entree.getValue()));
        }
        return captures.isEmpty() ? null : captures;
    }

    /** Compile une expression, les erreurs de syntaxe sont explicites. */
    private static Pattern expression(String motif) {
        try {
            return Pattern.compile(motif);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("bad pattern: " + motif, e);
        }
    }

    /** Exige une chaîne non nulle. */
    private static String texte(@Nullable Object brut, String ou) {
        if (brut == null) {
            throw new IllegalArgumentException(ou + " must be a string");
        }
        return String.valueOf(brut);
    }
}
