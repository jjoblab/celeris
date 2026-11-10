package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.language.JsonReader;
import jo.celeris.core.syntax.ScopeSelector;

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
        String porteeRacine = texte(champs.get("scopeName"), "scopeName");
        // Le nom racine est FACULTATIF au format TextMate — la
        // grammaire PHP du commerce (microsoft/vscode) s'en passe ; la
        // portée racine sert de repli affichable.
        Object brutNom = champs.get("name");
        String nom = brutNom == null ? porteeRacine : texte(brutNom, "name");
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
        List<Injection> injections = injections(champs.get("injections"), lecteur);
        return new Grammaire(nom, porteeRacine, racine.regles(), injections,
                rapport.tisser());
    }

    /**
     * Les injections déclarées (H-05) : la clé porte le sélecteur et
     * sa priorité ({@code L:} avant le contexte, {@code R:} après,
     * sans préfixe : après — l'hôte reste maître chez lui), la valeur
     * porte les règles injectées.
     */
    private static List<Injection> injections(@Nullable Object brut,
            LecteurGrammaire lecteur) {
        List<Injection> injections = new ArrayList<>();
        if (brut == null) {
            return injections;
        }
        if (!(brut instanceof Map<?, ?> cles)) {
            throw new IllegalArgumentException(
                    "injections : un objet est attendu");
        }
        for (Map.Entry<?, ?> entree : cles.entrySet()) {
            String cle = String.valueOf(entree.getKey());
            boolean gauche = cle.startsWith("L:");
            String selecteur = cle.startsWith("L:") || cle.startsWith("R:")
                    ? cle.substring(2) : cle;
            injections.add(new Injection(ScopeSelector.parse(selecteur),
                    lecteur.reglesDe(entree.getValue(), "injections." + cle),
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
