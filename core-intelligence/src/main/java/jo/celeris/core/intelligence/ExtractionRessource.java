package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * L'extraction d'une chaîne vers une ressource Android (C-13) : le
 * littéral de chaîne du document devient la référence
 * {@code getString(R.string.nom)} et la ressource naît dans le fichier
 * de valeurs par défaut, avant la fermeture de {@code <resources>}. Le
 * plan se construit en pur — le lexique dit les bornes du littéral, le
 * dictionnaire de fichiers dit le fichier de valeurs — et s'applique
 * par la voie de transaction, annulable : une édition par fichier, un
 * geste d'annulation par fichier (le patron multi-fichiers du
 * renommage, B-19).
 *
 * <p>Limites documentées : la référence suit la forme d'appel de
 * l'activité ({@code getString(R.string.nom)}) — l'hôte qui connaît
 * son contexte peut récrire l'édition ; les échappements lus sont les
 * cinq de base ({@code \n}, {@code \t}, {@code \\}, {@code \"},
 * {@code \'}) et les entités du XML se protègent (« et » commercial,
 * chevrons ouvrant et fermant), les autres échappements restent
 * littéraux ; le nom doit être un nom de ressource — minuscule
 * d'abord, puis lettres minuscules, chiffres et tiret bas : un nom
 * camelCase se refuse, la casse reste la charge de l'appelant. Une
 * chaîne vide, un nom déjà défini dans les fichiers du projet, un
 * fichier de valeurs absent ou sans fermeture de ressources refusent
 * le plan — jamais un plan faux.</p>
 */
public final class ExtractionRessource {

    /** Le nom de ressource : minuscule d'abord, puis [a-z0-9_]. */
    private static final Pattern NOM_RESSOURCE = Pattern.compile("[a-z][a-z0-9_]*");

    private ExtractionRessource() {
    }

    /**
     * Le plan d'extraction : les éditions par chemin de fichier — le
     * document édité et le fichier de valeurs.
     *
     * @param editions les éditions : le littéral remplacé par sa
     *                 référence, la ressource insérée avant la
     *                 fermeture ; en ordre descendant d'application
     */
    public record PlanRessource(Map<String, List<TextEdit>> editions) {

        /** Constructeur canonique : carte figée, jamais vide. */
        public PlanRessource {
            Objects.requireNonNull(editions, "editions");
            if (editions.isEmpty()) {
                throw new IllegalArgumentException("editions must not be empty");
            }
            editions = Map.copyOf(editions);
        }
    }

    /**
     * Extrait le littéral de chaîne qui porte le curseur vers une
     * ressource de chaînes : le littéral se remplace par
     * {@code getString(R.string.nom)}, la ressource naît dans le
     * fichier de valeurs par défaut du dictionnaire. Le curseur peut
     * vivre dans le littéral ou le toucher.
     *
     * @param uri      le chemin du document édité, clé de la carte des
     *                 éditions
     * @param doc      le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param offset   le curseur, dans le littéral ou le touchant
     * @param nom      le nom de la ressource, minuscule d'abord
     * @param fichiers les fichiers du projet, chemin vers contenu —
     *                 le fichier de valeurs s'y cherche
     * @return le plan, ou null hors littéral de chaîne, sur une chaîne
     *         vide, un nom invalide ou déjà défini, un fichier de
     *         valeurs absent ou brisé
     * @throws IllegalArgumentException si le nom est blanc ou si le
     *         curseur sort des bornes du document
     */
    public static @Nullable PlanRessource plan(String uri, Document doc, Analysis analyse,
            int offset, String nom, Map<String, String> fichiers) {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(analyse, "analyse");
        Objects.requireNonNull(fichiers, "fichiers");
        requireNom(nom);
        if (offset < 0 || offset > doc.length()) {
            throw new IllegalArgumentException(
                    "offset " + offset + " out of [0, " + doc.length() + "]");
        }
        Litteral jeton = jetonStringAt(doc, analyse, offset);
        if (jeton == null || jeton.text().length() < 3) {
            return null;
        }
        String literal = jeton.text();
        if (!NOM_RESSOURCE.matcher(nom).matches()) {
            return null;
        }
        String cheminValeurs = fichierDeValeurs(fichiers);
        if (cheminValeurs == null) {
            return null;
        }
        String valeurs = Objects.requireNonNull(fichiers.get(cheminValeurs), "contenu");
        int fermeture = valeurs.lastIndexOf("</resources>");
        if (fermeture < 0) {
            return null;
        }
        if (ReferencesRessources.definition(fichiers, "string", nom).isPresent()) {
            return null;
        }
        String interior = literal.substring(1, literal.length() - 1);
        int ligneFermeture = debutDeLigne(valeurs, fermeture);
        String entree = retraitExistant(valeurs, fermeture, ligneFermeture)
                + "<string name=\"" + nom + "\">" + valeurXml(interior) + "</string>\n";
        // Ordre descendant d'application : les offsets originaux
        // restent valides au fil de la transaction séquentielle.
        Map<String, List<TextEdit>> editions = new LinkedHashMap<>();
        editions.computeIfAbsent(uri, cle -> new ArrayList<>())
                .add(new TextEdit(jeton.offset(), literal,
                        "getString(R.string." + nom + ")"));
        editions.computeIfAbsent(cheminValeurs, cle -> new ArrayList<>())
                .add(new TextEdit(ligneFermeture, "", entree));
        for (List<TextEdit> liste : editions.values()) {
            liste.sort(Comparator.comparingInt(TextEdit::start).reversed());
        }
        return new PlanRessource(editions);
    }

    /** Le littéral trouvé : son offset d'ouverture et son texte jeton. */
    private record Litteral(int offset, String text) {
    }

    /**
     * Le littéral de chaîne qui porte le curseur : le jeton STRING
     * double-guillemeté dont les bornes le contiennent ou le
     * touchent. Le lexique ne dit pas les littéraux simples
     * apostrophés (chars) ni les chaînes non fermées — ils ne se
     * ressourcent pas.
     */
    private static @Nullable Litteral jetonStringAt(Document doc,
            Analysis analyse, int offset) {
        for (FlowAnalysis.AbsoluteToken jeton
                : FlowAnalysis.tokens(doc, analyse.lex(), 0, doc.length())) {
            if (jeton.kind() != TokenKind.STRING || jeton.text().charAt(0) != '"') {
                continue;
            }
            int fin = jeton.offset() + jeton.text().length();
            if (jeton.offset() <= offset && offset <= fin) {
                return new Litteral(jeton.offset(), jeton.text());
            }
        }
        return null;
    }

    /**
     * Le fichier de valeurs par défaut du dictionnaire : le chemin
     * finissant par {@code values/strings.xml}, ou le fichier de
     * valeurs nu — déterministe, le plus petit chemin l'emporte.
     */
    private static @Nullable String fichierDeValeurs(Map<String, String> fichiers) {
        List<String> candidats = new ArrayList<>();
        for (String chemin : fichiers.keySet()) {
            if (chemin.endsWith("/values/strings.xml")
                    || chemin.equals("values/strings.xml")
                    || chemin.equals("strings.xml")) {
                candidats.add(chemin);
            }
        }
        candidats.sort(String::compareTo);
        return candidats.isEmpty() ? null : candidats.get(0);
    }

    /**
     * La valeur XML du littéral : les cinq échappements de base se
     * décodent, les entités se protègent — le reste reste littéral.
     */
    private static String valeurXml(String interior) {
        StringBuilder out = new StringBuilder(interior.length() + 8);
        for (int i = 0; i < interior.length(); i++) {
            char c = interior.charAt(i);
            if (c == '\\' && i + 1 < interior.length()) {
                char suivant = interior.charAt(++i);
                switch (suivant) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case '\\' -> out.append('\\');
                    case '"' -> out.append('"');
                    case '\'' -> out.append('\'');
                    default -> out.append('\\').append(suivant);
                }
                continue;
            }
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /** Le début de la ligne qui porte l'offset donné. */
    private static int debutDeLigne(String texte, int offset) {
        int debut = offset;
        while (debut > 0 && texte.charAt(debut - 1) != '\n') {
            debut--;
        }
        return debut;
    }

    /**
     * Le retrait de la nouvelle entrée : celui de la dernière entrée
     * existante, sinon un cran sous la fermeture de ressources.
     */
    private static String retraitExistant(String valeurs, int fermeture, int ligneFermeture) {
        int derniereEntree = valeurs.lastIndexOf("<string", fermeture);
        if (derniereEntree >= 0) {
            return indentationDe(valeurs, debutDeLigne(valeurs, derniereEntree));
        }
        return indentationDe(valeurs, ligneFermeture) + "    ";
    }

    /** Le retrait d'une ligne : espaces et tabulations de tête. */
    private static String indentationDe(String texte, int debutLigne) {
        int fin = debutLigne;
        while (fin < texte.length()
                && (texte.charAt(fin) == ' ' || texte.charAt(fin) == '\t')) {
            fin++;
        }
        return texte.substring(debutLigne, fin);
    }

    private static void requireNom(String nom) {
        Objects.requireNonNull(nom, "nom");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("nom must not be blank");
        }
    }
}
