package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Map;

/**
 * Le schéma embarqué des ressources Android (C-02) : les balises
 * connues et leurs attributs, les valeurs de taille et les préfixes de
 * référence — la matière première de la complétion guidée, sans
 * aucune dépendance : les tables sont déclarées ici, le reste du dépôt
 * n'y touche jamais directement.
 *
 * <p>Les attributs communs — identifiant, tailles, marges, bourrelets —
 * s'ajoutent à ceux propres à chaque balise ; une balise inconnue
 * retombe sur les communs, jamais sur rien.</p>
 */
public final class SchemaAndroid {

    /** Les attributs communs à toute balise de mise en page. */
    public static final List<String> COMMUNS = List.of(
            "android:id", "android:layout_width", "android:layout_height",
            "android:layout_margin", "android:layout_marginTop",
            "android:layout_marginBottom", "android:layout_marginStart",
            "android:layout_marginEnd", "android:padding", "android:paddingTop",
            "android:paddingBottom", "android:paddingStart", "android:paddingEnd");

    private static final Map<String, List<String>> BALISES = Map.ofEntries(
            Map.entry("TextView", List.of("android:text", "android:textSize",
                    "android:textColor", "android:textStyle", "android:maxLines",
                    "android:ellipsize", "android:gravity")),
            Map.entry("Button", List.of("android:text", "android:onClick",
                    "android:enabled", "android:textAllCaps")),
            Map.entry("EditText", List.of("android:hint", "android:inputType",
                    "android:maxLength", "android:maxLines", "android:imeOptions")),
            Map.entry("ImageView", List.of("android:src", "android:contentDescription",
                    "android:scaleType", "android:adjustViewBounds")),
            Map.entry("LinearLayout", List.of("android:orientation",
                    "android:gravity", "android:baselineAligned",
                    "android:divider", "android:showDividers")),
            Map.entry("RelativeLayout", List.of("android:gravity",
                    "android:layout_centerInParent")),
            Map.entry("ScrollView", List.of("android:fillViewport",
                    "android:scrollbars", "android:overScrollMode")),
            Map.entry("CheckBox", List.of("android:checked", "android:text",
                    "android:onClick")),
            Map.entry("RadioButton", List.of("android:checked", "android:text")),
            Map.entry("Switch", List.of("android:checked", "android:text",
                    "android:textOn", "android:textOff")),
            Map.entry("Spinner", List.of("android:entries", "android:prompt")),
            Map.entry("SeekBar", List.of("android:max", "android:progress")),
            Map.entry("ProgressBar", List.of("android:max", "android:progress",
                    "android:indeterminate")),
            Map.entry("RecyclerView", List.of("android:scrollbars",
                    "android:overScrollMode")),
            Map.entry("View", List.of("android:background", "android:visibility",
                    "android:contentDescription")));

    /** Les balises connues du schéma, pour la complétion de nom. */
    public static final List<String> BALISES_CONNUES = List.copyOf(
            BALISES.keySet().stream().sorted().toList());

    private static final List<String> TAILLES = List.of(
            "match_parent", "wrap_content");

    private static final List<String> REFERENCES = List.of(
            "@string/", "@drawable/", "@color/", "@dimen/", "@layout/",
            "@mipmap/", "@array/", "?attr/");

    private SchemaAndroid() {
    }

    /**
     * Les attributs suggérés pour une balise : les siens puis les
     * communs, sans doublon, dans l'ordre du schéma.
     *
     * @param balise le nom de la balise, sans chevrons
     * @return les attributs suggérés (jamais vide : les communs répondent)
     */
    public static List<String> attributs(String balise) {
        List<String> propres = BALISES.getOrDefault(balise, List.of());
        List<String> tous = new java.util.ArrayList<>(propres);
        for (String commun : COMMUNS) {
            if (!tous.contains(commun)) {
                tous.add(commun);
            }
        }
        return List.copyOf(tous);
    }

    /**
     * Les valeurs de taille suggérées pour un attribut de dimension :
     * les constantes de mise en page pour les tailles de vue, les
     * unités suffixées pour les autres.
     *
     * @param attribut le nom complet de l'attribut
     * @param valeur   la valeur en cours de frappe (peut être vide)
     * @return les valeurs suggérées
     */
    public static List<String> valeurs(String attribut, String valeur) {
        if (attribut.equals("android:layout_width")
                || attribut.equals("android:layout_height")) {
            return TAILLES;
        }
        if (attribut.equals("android:textSize") && !valeur.isEmpty()) {
            return List.of(valeur + "sp");
        }
        if (!valeur.isEmpty() && (attribut.endsWith("Margin")
                || attribut.startsWith("android:padding"))) {
            return List.of(valeur + "dp");
        }
        return List.of();
    }

    /**
     * Les préfixes de référence suggérés dans une valeur d'attribut.
     *
     * @return les préfixes ({@code @string/}, {@code @drawable/}…)
     */
    public static List<String> references() {
        return REFERENCES;
    }
}
