package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jo.celeris.core.syntax.scope.ScopeSelector;
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
 * sautent et se comptent, comme la référence les tolère. Les injections
 * se lisent (H-05) : leur clé porte le sélecteur et la priorité
 * ({@code L:} ou {@code R:}), leurs règles s'appliquent partout où le
 * sélecteur matche la pile courante. Une forme catastrophique visible
 * (quantificateurs non bornés imbriqués) écarte SA règle — comptée au
 * rapport, comme un motif invalide : jamais le paquet entier (H-05,
 * les grammaires du commerce chargent telles que publiées), jamais un
 * gel de l'interface (le budget d'exécution y veille).
 *
 * <p>M7.6 : les grammaires embarquées vivent dans les paquets de
 * langage ({@code langages/<id>/grammar.json} de core-language) et
 * arrivent par la couture {@link PackGrammars} ; ce chargeur ne lit
 * que du texte JSON.</p>
 */
public final class Grammars {

    private Grammars() {
    }

    /**
     * Lit une grammaire depuis son texte JSON déclaratif, sans registre
     * externe — les inclusions de portée externe se sautent, comptées.
     *
     * @param json le texte de la grammaire
     * @return la grammaire compilée, référentiel résolu en références
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    public static Grammar read(String json) {
        return read(json, Map.of());
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
    public static Grammar read(String json, Map<String, Grammar> externes) {
        Object racineBrute = JsonReader.read(json);
        if (!(racineBrute instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException("la racine de la grammaire est un objet");
        }
        String porteeRacine = text(champs.get("scopeName"), "scopeName");
        // Le nom racine est FACULTATIF au format TextMate — la
        // grammaire PHP du commerce (microsoft/vscode) s'en passe ; la
        // portée racine sert de repli affichable.
        Object brutNom = champs.get("name");
        String nom = brutNom == null ? porteeRacine : text(brutNom, "name");
        GrammarReport.Tisseur report = new GrammarReport.Tisseur();
        Map<String, Object> bruts = bruts(champs.get("repository"));
        Map<String, ListRule> referentiel = new LinkedHashMap<>();
        for (String cle : bruts.keySet()) {
            referentiel.put(cle, new ListRule());
        }
        ListRule racine = new ListRule();
        GrammarReader lecteur =
                new GrammarReader(referentiel, racine, externes, report);
        for (Map.Entry<String, Object> input : bruts.entrySet()) {
            Objects.requireNonNull(referentiel.get(input.getKey()), "liste manquante")
                    .fixer(lecteur.reglesDe(input.getValue(), "#" + input.getKey()));
        }
        racine.fixer(lecteur.aplatir(champs.get("patterns"), "patterns"));
        List<Injection> injections = injections(champs.get("injections"), lecteur);
        Grammar grammaire = new Grammar(nom, porteeRacine, racine.regles(),
                injections, report.tisser(), lecteur.keywords());
        // Le jeu d'ancres de document (chantier C2) : le lexeur y
        // traduit les expressions à \A selon la ligne.
        grammaire.ancres().putAll(lecteur.ancres());
        return grammaire;
    }

    /**
     * Les injections déclarées (H-05) : la clé porte le sélecteur et
     * sa priorité ({@code L:} avant le contexte, {@code R:} après,
     * sans préfixe : après — l'hôte reste maître chez lui), la valeur
     * porte les règles injectées.
     */
    private static List<Injection> injections(@Nullable Object brut,
            GrammarReader lecteur) {
        List<Injection> injections = new ArrayList<>();
        if (brut == null) {
            return injections;
        }
        if (!(brut instanceof Map<?, ?> cles)) {
            throw new IllegalArgumentException(
                    "injections : un objet est attendu");
        }
        for (Map.Entry<?, ?> input : cles.entrySet()) {
            String cle = String.valueOf(input.getKey());
            boolean gauche = cle.startsWith("L:");
            String selecteur = cle.startsWith("L:") || cle.startsWith("R:")
                    ? cle.substring(2) : cle;
            injections.add(new Injection(ScopeSelector.parse(selecteur),
                    lecteur.reglesDe(input.getValue(), "injections." + cle),
                    gauche));
        }
        return injections;
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
        for (Map.Entry<?, ?> input : cles.entrySet()) {
            if (!(input.getKey() instanceof String cle)) {
                throw new IllegalArgumentException("les clés du référentiel sont des chaînes");
            }
            bruts.put(cle, input.getValue());
        }
        return bruts;
    }

    /** Exige une chaîne non nulle. */
    private static String text(@Nullable Object brut, String ou) {
        if (brut == null) {
            throw new IllegalArgumentException(ou + " : une chaîne est attendue");
        }
        return String.valueOf(brut);
    }
}
