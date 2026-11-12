package jo.celeris.core.lsp;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.text.Document;

/**
 * La documentation des modificateurs au survol (C-10) : la table des
 * modificateurs d'agencement du kit d'interface déclaratif — {@code
 * Modifier.padding}, {@code Modifier.background}, {@code size}… —
 * servie au point du survol, le nom connu seul décide (ADR-005 : le
 * nom, jamais une résolution en chaîne du récepteur).
 *
 * <p>La reconnaissance est TOLÉRANTE : l'identifiant qui porte le
 * curseur se lit dans le texte du document (n'importe quel langage),
 * la table dit si un modificateur vit sous ce nom — la FORME se
 * vérifie au passage : un modificateur se nomme en position d'appel
 * (un {@code (} suit le nom, blanc intercalé toléré), le récepteur
 * {@code Modifier} se survole en toute position. Un nom inconnu, un
 * blanc ou une référence sans appel rend un survol nul — jamais un
 * texte faux. La lecture est TEXTUELLE : le lexique du langage ne
 * décide pas ici, un nom en position d'appel apparente dans une
 * chaîne se documenterait (limite documentée). Les textes sont
 * français, la signature vivante dans une clôture Kotlin (PIEGE-16 :
 * le titre ne se fond jamais dans le corps).</p>
 */
public final class DocModificateurs {

    /** Le récepteur des modificateurs — il se survole en toute position. */
    private static final String RECEPTEUR = "Modifier";

    /** La table des modificateurs connus : nom vers documentation markdown. */
    private static final Map<String, String> DOCUMENTATION = Map.ofEntries(
            entry(RECEPTEUR,
                    "**Modifier** — le récepteur des modificateurs :\n\n"
                            + "```kotlin\nModifier.padding(16.dp).background(couleur)\n```\n\n"
                            + "L'objet neutre dont chaque modificateur dérive : les "
                            + "modificateurs s'y enchaînent, l'ordre compte — celui "
                            + "de droite s'applique à ce que celui de gauche a dessiné."),
            entry("padding",
                    "**padding** — modificateur d'espacement :\n\n"
                            + "```kotlin\nModifier.padding(tous: Dp)\n"
                            + "Modifier.padding(horizontal: Dp, vertical: Dp)\n```\n\n"
                            + "L'espace autour du contenu : tous les côtés d'un coup ou "
                            + "chacun le sien (haut, bas, gauche, droite), en dp."),
            entry("background",
                    "**background** — modificateur de fond :\n\n"
                            + "```kotlin\nModifier.background(couleur: Color)\n```\n\n"
                            + "La teinte (ou le pinceau) qui se peint derrière le "
                            + "contenu, forme comprise — rectangle par défaut, coupe "
                            + "arrondie par une forme passée."),
            entry("size",
                    "**size** — modificateur de dimension :\n\n"
                            + "```kotlin\nModifier.size(taille: Dp)\n```\n\n"
                            + "La largeur ET la hauteur d'un même coup, en dp — les "
                            + "contraignes de la rangée porteuse peuvent les recadrer."),
            entry("width",
                    "**width** — modificateur de largeur :\n\n"
                            + "```kotlin\nModifier.width(largeur: Dp)\n```\n\n"
                            + "La largeur fixe, en dp — la hauteur reste ce que le "
                            + "contenu demande."),
            entry("height",
                    "**height** — modificateur de hauteur :\n\n"
                            + "```kotlin\nModifier.height(hauteur: Dp)\n```\n\n"
                            + "La hauteur fixe, en dp — la largeur reste ce que le "
                            + "contenu demande."),
            entry("fillMaxWidth",
                    "**fillMaxWidth** — modificateur de remplissage :\n\n"
                            + "```kotlin\nModifier.fillMaxWidth()\n```\n\n"
                            + "La largeur prend TOUT ce que le parent accorde — la "
                            + "fraction passée en prend la part."),
            entry("fillMaxHeight",
                    "**fillMaxHeight** — modificateur de remplissage :\n\n"
                            + "```kotlin\nModifier.fillMaxHeight()\n```\n\n"
                            + "La hauteur prend tout ce que le parent accorde — la "
                            + "fraction passée en prend la part."),
            entry("fillMaxSize",
                    "**fillMaxSize** — modificateur de remplissage :\n\n"
                            + "```kotlin\nModifier.fillMaxSize()\n```\n\n"
                            + "La largeur ET la hauteur prennent tout ce que le "
                            + "parent accorde."),
            entry("weight",
                    "**weight** — modificateur de partage :\n\n"
                            + "```kotlin\nModifier.weight(part: Float)\n```\n\n"
                            + "La part de l'espace RESTANT dans la rangée porteuse "
                            + "(rangée horizontale ou colonne) — le poids est relatif "
                            + "aux autres occupants."),
            entry("align",
                    "**align** — modificateur d'alignement :\n\n"
                            + "```kotlin\nModifier.align(alignment: Alignment)\n```\n\n"
                            + "La place du contenu dans le parent (cadre commun) — "
                            + "l'alignement se dit par ses deux axes."),
            entry("offset",
                    "**offset** — modificateur de décalage :\n\n"
                            + "```kotlin\nModifier.offset(x: Dp, y: Dp)\n```\n\n"
                            + "Le décalage VISUEL du contenu, sans changer ni sa "
                            + "place mesurée, ni celle de ses voisins — un décalage "
                            + "de mise en page se prend par le padding."),
            entry("clickable",
                    "**clickable** — modificateur d'interaction :\n\n"
                            + "```kotlin\nModifier.clickable { ... }\n```\n\n"
                            + "Le contenu devient appuyable : l'ondulation de la "
                            + "réponse, l'état appuyé et l'étiquette d'accessibilité "
                            + "vivent avec."),
            entry("alpha",
                    "**alpha** — modificateur de transparence :\n\n"
                            + "```kotlin\nModifier.alpha(transparence: Float)\n```\n\n"
                            + "La transparence du dessin, de 0 (invisible) à 1 "
                            + "(opaque) — la place mesurée ne change pas."),
            entry("border",
                    "**border** — modificateur de contour :\n\n"
                            + "```kotlin\nModifier.border(largeur: Dp, couleur: Color)\n```\n\n"
                            + "Le trait autour du contenu, épaisseur et teinte "
                            + "données — la coupe arrondie suit une forme passée."),
            entry("clip",
                    "**clip** — modificateur de coupe :\n\n"
                            + "```kotlin\nModifier.clip(forme: Shape)\n```\n\n"
                            + "Le contenu se coupe à la forme — tout ce qui dépasse "
                            + "ne se dessine plus, les descendants héritent la coupe."),
            entry("shadow",
                    "**shadow** — modificateur d'ombre :\n\n"
                            + "```kotlin\nModifier.shadow(elévation: Dp)\n```\n\n"
                            + "L'ombre portée derrière le contenu, élévation et "
                            + "forme données."),
            entry("aspectRatio",
                    "**aspectRatio** — modificateur de proportion :\n\n"
                            + "```kotlin\nModifier.aspectRatio(ratio: Float)\n```\n\n"
                            + "La largeur se déduit de la hauteur (l'inverse selon "
                            + "ce que le parent accorde) au rapport donné."),
            entry("wrapContentSize",
                    "**wrapContentSize** — modificateur d'enveloppement :\n\n"
                            + "```kotlin\nModifier.wrapContentSize(align: Alignment)\n```\n\n"
                            + "Le contenu redevient sa taille propre dans l'espace "
                            + "accordé, aligné dedans — le remplissage précédent "
                            + "ne l'écrase plus."));

    private DocModificateurs() {
    }

    /**
     * Le survol du modificateur à l'offset : l'identifiant qui porte
     * le curseur se lit dans le texte, la table dit sa documentation —
     * un modificateur se nomme en position d'appel (un parenthèse
     * ouvrante suit), le récepteur {@code Modifier} se survole en
     * toute position.
     *
     * @param document le document ouvert, jamais muté
     * @param offset    la position visitée
     * @return le survol markdown, sa plage aux bornes de l'identifiant,
     *         ou null quand rien ne se nomme là
     */
    public static @Nullable SurvolLsp survol(Document document, int offset) {
        Objects.requireNonNull(document, "document");
        if (offset < 0 || offset > document.length()) {
            throw new IllegalArgumentException(
                    "offset " + offset + " out of [0, " + document.length() + "]");
        }
        String texte = document.text();
        int fin = Math.min(offset, texte.length());
        int debut = debutIdentifiant(texte, fin);
        int borne = fin;
        while (borne < texte.length() && estIdentifiant(texte.charAt(borne))) {
            borne++;
        }
        String nom = debut == borne ? null : texte.substring(debut, borne);
        if (nom == null || !connait(nom)) {
            return null;
        }
        if (!nom.equals(RECEPTEUR) && !ouvreAppel(texte, borne)) {
            return null;
        }
        String documentation = Objects.requireNonNull(DOCUMENTATION.get(nom), "documentation");
        return new SurvolLsp(documentation, "markdown",
                PositionsLsp.plage(document, debut, borne));
    }

    /**
     * La documentation d'un modificateur connu.
     *
     * @param nom le nom du modificateur
     * @return la documentation markdown, null si la table ne connaît pas
     */
    public static @Nullable String documentation(String nom) {
        Objects.requireNonNull(nom, "nom");
        return DOCUMENTATION.get(nom);
    }

    /** Vrai quand un modificateur vit sous ce nom dans la table. */
    private static boolean connait(String nom) {
        return DOCUMENTATION.containsKey(nom);
    }

    /** Vrai quand un caractère autre qu'un blanc suit le nom avant la parenthèse. */
    private static boolean ouvreAppel(String texte, int borne) {
        int apres = borne;
        while (apres < texte.length() && Character.isWhitespace(texte.charAt(apres))) {
            apres++;
        }
        return apres < texte.length() && texte.charAt(apres) == '(';
    }

    /** Le début du mot qui finit à la borne : identifiants reculés. */
    private static int debutIdentifiant(String texte, int borne) {
        int debut = borne;
        while (debut > 0 && estIdentifiant(texte.charAt(debut - 1))) {
            debut--;
        }
        return debut;
    }

    /** Vrai quand le caractère vit dans un identifiant (lettres, chiffres, tiret bas). */
    private static boolean estIdentifiant(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** L'entrée de la table — la fabrique de Map.ofEntries, nommée pour la lisibilité. */
    private static Map.Entry<String, String> entry(String nom, String documentation) {
        return Map.entry(nom, documentation);
    }
}
