package jo.celeris.core.syntax.grammars;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.language.JsonReader;

/**
 * Chargeur de grammaires au format TextMate réel (ADR-004 révisé,
 * ADR-014) : lit le JSON déclaratif tel que les extensions de VS Code le
 * publient — nom, portée racine, motifs, référentiel, captures en
 * objets — compile les expressions par joni et résout les inclusions du
 * référentiel en références : la liste de chaque clé existe avant sa
 * propre résolution, les inclusions cycliques se parcourent sans fin
 * d'aplatissement. {@code $self} et {@code $base} pointent la racine de
 * la grammaire chargée ; les inclusions de portée externe se résolvent
 * par le registre passé au chargement — absentes du registre, elles se
 * sautent et se comptent, comme la référence les tolère. Une forme
 * catastrophique visible (quantificateurs non bornés imbriqués) rejette
 * le paquet entier ; un motif invalide écarte sa règle.
 *
 * <p>M7.6 : les grammaires embarquées vivent dans les paquets de
 * langage ({@code langages/<id>/grammar.json} de core-language) et
 * arrivent par la couture {@link PackGrammars} ; ce chargeur ne lit
 * que du texte JSON.</p>
 */
public final class Grammaires {

    private Grammaires() {
    }

    /**
     * Lit une grammaire depuis son texte JSON déclaratif, sans registre
     * externe — les inclusions de portée externe se sautent, comptées.
     *
     * @param json le texte de la grammaire
     * @return la grammaire compilée, référentiel résolu en références
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    public static Grammaire lire(String json) {
        return lire(json, Map.of());
    }

    /**
     * Lit une grammaire depuis son texte JSON déclaratif, avec son
     * registre de grammaires externes : les inclusions de portée racine
     * ({@code source.python}…) s'y résolvent.
     *
     * @param json     le texte de la grammaire
     * @param externes les grammaires résolubles par portée racine
     * @return la grammaire compilée, référentiel résolu en références
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    public static Grammaire lire(String json, Map<String, Grammaire> externes) {
        Object racineBrute = JsonReader.read(json);
        if (!(racineBrute instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException("la racine de la grammaire est un objet");
        }
        String nom = texte(champs.get("name"), "name");
        String porteeRacine = texte(champs.get("scopeName"), "scopeName");
        RapportGrammaire.Tisseur rapport = new RapportGrammaire.Tisseur();
        Map<String, Object> bruts = bruts(champs.get("repository"));
        Map<String, RegleListe> referentiel = new LinkedHashMap<>();
        for (String cle : bruts.keySet()) {
            referentiel.put(cle, new RegleListe());
        }
        RegleListe racine = new RegleListe();
        LecteurGrammaire lecteur =
                new LecteurGrammaire(referentiel, racine, externes, rapport);
        for (Map.Entry<String, Object> entree : bruts.entrySet()) {
            Objects.requireNonNull(referentiel.get(entree.getKey()), "liste manquante")
                    .fixer(lecteur.reglesDe(entree.getValue(), "#" + entree.getKey()));
        }
        racine.fixer(lecteur.aplatir(champs.get("patterns"), "patterns"));
        if (champs.containsKey("injections")) {
            rapport.injectionIgnoree();
        }
        return new Grammaire(nom, porteeRacine, racine.regles(), rapport.tisser());
    }

    /** Le référentiel brut : chaque clé vers son contenu non résolu. */
    private static Map<String, Object> bruts(@Nullable Object brut) {
        Map<String, Object> bruts = new LinkedHashMap<>();
        if (brut == null) {
            return bruts;
        }
        if (!(brut instanceof Map<?, ?> cles)) {
            throw new IllegalArgumentException("repository : un objet est attendu");
        }
        for (Map.Entry<?, ?> entree : cles.entrySet()) {
            if (!(entree.getKey() instanceof String cle)) {
                throw new IllegalArgumentException("les clés du référentiel sont des chaînes");
            }
            bruts.put(cle, entree.getValue());
        }
        return bruts;
    }

    /** Exige une chaîne non nulle. */
    private static String texte(@Nullable Object brut, String ou) {
        if (brut == null) {
            throw new IllegalArgumentException(ou + " : une chaîne est attendue");
        }
        return String.valueOf(brut);
    }
}
