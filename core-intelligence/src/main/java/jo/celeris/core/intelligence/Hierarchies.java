package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;

/**
 * Les hiérarchies de types et d'appels (B-12) : les ancêtres et les
 * descendants d'un type par les clauses {@code extends} et
 * {@code implements} lues à l'en-tête (la lecture vit dans
 * {@link ClausesHeritage}), les appelants d'une méthode par les sites
 * d'appel — l'arbre se parcourt en liste, chaque sommet porte sa
 * profondeur (le patron du plan : c'est l'arbre qui porte la
 * hiérarchie, la liste la parcourt).
 *
 * <p>Le projet se donne en dictionnaire uri → document (ADR-002), la
 * syntaxe donnée décide de l'analyse de chaque fichier — les fichiers
 * qu'elle ne lit pas en types ne comptent pas. La lecture est
 * TOLÉRANTE et l'index est nominal (ADR-005) : le nom seul décide,
 * jamais une résolution en chaîne — les surcharges homonymes fusionnent
 * (limite documentée) ; les parents hors du projet embarqué se
 * signalent introuvables, la frontière s'arrête là ; un appel sur un
 * récepteur étranger ({@code x.nom(}) ou une référence de méthode
 * ({@code nom::}) ne crée pas d'arête — le doute se signale, jamais ne
 * se tranche ; les constructions ({@code new nom(}) ne comptent pas —
 * la hiérarchie d'appels sert les méthodes ; l'appel hors méthode
 * (initialiseur de champ) ne se résout pas — signalé.</p>
 */
public final class Hierarchies {

    private Hierarchies() {
    }

    /** Un sommet de l'arbre : le nom, le fichier qui le déclare, la profondeur depuis la racine. */
    public record Sommet(String nom, String uri, int profondeur) {
    }

    /**
     * La hiérarchie de types d'un nom : les ancêtres par générations
     * (la racine d'abord, chaque génération ensuite), les descendants
     * en parcours en profondeur (la racine d'abord), et les parents
     * nommés hors du projet embarqué — introuvables, la frontière
     * s'arrête là.
     */
    public record HierarchieTypes(List<Sommet> ancetres, List<Sommet> descendants,
            List<String> introuvables) {

        /** Constructeur canonique : listes figées. */
        public HierarchieTypes {
            ancetres = List.copyOf(ancetres);
            descendants = List.copyOf(descendants);
            introuvables = List.copyOf(introuvables);
        }
    }

    /** La hiérarchie d'appels d'un nom : les appelants en arbre, le doute signalé. */
    public record HierarchieAppels(List<Sommet> appelants, boolean doute) {

        /** Constructeur canonique : liste figée. */
        public HierarchieAppels {
            appelants = List.copyOf(appelants);
        }
    }

    /**
     * La hiérarchie de types d'un nom : ancêtres et descendants, sur
     * le projet embarqué.
     *
     * @param nom     le nom du type cherché
     * @param projet  les fichiers du projet, uri vers document
     * @param syntaxe la syntaxe qui analyse chaque fichier
     * @return la hiérarchie : la racine aux deux listes, les parents
     *         hors projet en introuvables
     * @throws IllegalArgumentException si le nom est blanc
     */
    public static HierarchieTypes types(String nom, Map<String, Document> projet,
            SyntaxService syntaxe) {
        requireNom(nom);
        Objects.requireNonNull(projet, "projet");
        Objects.requireNonNull(syntaxe, "syntaxe");
        List<FichierProjet> fichiers = analyser(projet, syntaxe);
        Map<String, List<String>> parents = new LinkedHashMap<>();
        Map<String, String> uris = new LinkedHashMap<>();
        for (FichierProjet fichier : fichiers) {
            for (OutlineEntry type : fichier.types()) {
                uris.putIfAbsent(type.name(), fichier.uri());
                ajouterClauses(parents, type.name(), parentsDe(fichier, type));
            }
        }
        return new HierarchieTypes(ancetres(nom, parents, uris),
                descendants(nom, parents, uris), introuvables(nom, parents, uris));
    }

    /**
     * La hiérarchie d'appels d'un nom de méthode : les appelants en
     * arbre, chaque site d'appel nu ou {@code this.nom(} compte, le
     * doute (récepteur étranger, référence de méthode, appel hors
     * méthode) se signale sans arête.
     *
     * @param nom     le nom de la méthode cherchée
     * @param projet  les fichiers du projet, uri vers document
     * @param syntaxe la syntaxe qui analyse chaque fichier
     * @return la hiérarchie : la racine d'abord, les appelants en
     *         profondeur ; le doute quand un site ne se résout pas
     * @throws IllegalArgumentException si le nom est blanc
     */
    public static HierarchieAppels appelants(String nom, Map<String, Document> projet,
            SyntaxService syntaxe) {
        requireNom(nom);
        Objects.requireNonNull(projet, "projet");
        Objects.requireNonNull(syntaxe, "syntaxe");
        List<FichierProjet> fichiers = analyser(projet, syntaxe);
        Map<String, List<Sommet>> aretes = new LinkedHashMap<>();
        Map<String, String> uris = urisDesMethodes(fichiers);
        List<Sommet> sortie = new ArrayList<>();
        sortie.add(new Sommet(nom, uris.getOrDefault(nom, ""), 0));
        Set<String> visites = new HashSet<>();
        visites.add(cle(nom, uris.getOrDefault(nom, "")));
        Doute doute = new Doute();
        appelantsDescendants(nom, 1, fichiers, aretes, visites, sortie, doute);
        return new HierarchieAppels(sortie, doute.marque());
    }

    /** Les ancêtres par générations : la racine, puis chaque génération en largeur. */
    private static List<Sommet> ancetres(String nom, Map<String, List<String>> parents,
            Map<String, String> uris) {
        List<Sommet> sortie = new ArrayList<>();
        sortie.add(new Sommet(nom, uris.getOrDefault(nom, ""), 0));
        Set<String> visites = new HashSet<>();
        visites.add(nom);
        List<String> generation = new ArrayList<>();
        ajouterGeneration(parents.getOrDefault(nom, List.of()), visites, generation);
        for (String parent : generation) {
            sortie.add(new Sommet(parent, uris.getOrDefault(parent, ""), 1));
        }
        int profondeur = 2;
        while (!generation.isEmpty()) {
            List<String> suivante = new ArrayList<>();
            for (String membre : generation) {
                ajouterGeneration(parents.getOrDefault(membre, List.of()), visites, suivante);
            }
            for (String parent : suivante) {
                sortie.add(new Sommet(parent, uris.getOrDefault(parent, ""), profondeur));
            }
            generation = suivante;
            profondeur++;
        }
        return sortie;
    }

    /** La génération suivante : les parents non visités, jamais dédoublés. */
    private static void ajouterGeneration(List<String> candidats, Set<String> visites,
            List<String> generation) {
        for (String parent : candidats) {
            if (visites.add(parent)) {
                generation.add(parent);
            }
        }
    }

    /** Les descendants en profondeur : la racine, puis chaque sous-type. */
    private static List<Sommet> descendants(String nom, Map<String, List<String>> parents,
            Map<String, String> uris) {
        List<Sommet> sortie = new ArrayList<>();
        sortie.add(new Sommet(nom, uris.getOrDefault(nom, ""), 0));
        Set<String> visites = new HashSet<>();
        visites.add(nom);
        descendantsDfs(nom, 1, enfantsDe(parents), uris, visites, sortie);
        return sortie;
    }

    /** Le parcours en profondeur des sous-types, garde de cycle comprise. */
    private static void descendantsDfs(String nom, int profondeur,
            Map<String, List<String>> enfants, Map<String, String> uris,
            Set<String> visites, List<Sommet> sortie) {
        for (String enfant : enfants.getOrDefault(nom, List.of())) {
            if (visites.contains(enfant)) {
                continue;
            }
            visites.add(enfant);
            sortie.add(new Sommet(enfant, uris.getOrDefault(enfant, ""), profondeur));
            descendantsDfs(enfant, profondeur + 1, enfants, uris, visites, sortie);
        }
    }

    /** Les enfants de chaque parent : la carte des clauses retournée. */
    private static Map<String, List<String>> enfantsDe(Map<String, List<String>> parents) {
        Map<String, List<String>> enfants = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> clause : parents.entrySet()) {
            for (String parent : clause.getValue()) {
                enfants.computeIfAbsent(parent, cle -> new ArrayList<>())
                        .add(clause.getKey());
            }
        }
        return enfants;
    }

    /** Les parents nommés hors du projet embarqué, une fois chacun. */
    private static List<String> introuvables(String nom, Map<String, List<String>> parents,
            Map<String, String> uris) {
        Set<String> vus = new HashSet<>();
        List<String> sortie = new ArrayList<>();
        List<String> frontiere = new ArrayList<>(parents.getOrDefault(nom, List.of()));
        for (int i = 0; i < frontiere.size(); i++) {
            String membre = frontiere.get(i);
            if (!vus.add(membre)) {
                continue;
            }
            if (uris.containsKey(membre)) {
                frontiere.addAll(parents.getOrDefault(membre, List.of()));
            } else {
                sortie.add(membre);
            }
        }
        return sortie;
    }

    /**
     * Le parcours des appelants : les arêtes se calculent par nom au
     * fil du besoin, la garde de cycle tient à la clé nom+uri — la
     * récursion directe se montre une fois, profondeur un.
     */
    private static void appelantsDescendants(String nom, int profondeur,
            List<FichierProjet> fichiers, Map<String, List<Sommet>> aretes,
            Set<String> visites, List<Sommet> sortie, Doute doute) {
        List<Sommet> arete = aretes.computeIfAbsent(nom, cle -> appelantsDe(cle, fichiers, doute));
        for (Sommet appelant : arete) {
            String cle = cle(appelant.nom(), appelant.uri());
            if (visites.contains(cle) && !(profondeur == 1 && estRacine(appelant, sortie))) {
                continue;
            }
            visites.add(cle);
            sortie.add(new Sommet(appelant.nom(), appelant.uri(), profondeur));
            appelantsDescendants(appelant.nom(), profondeur + 1,
                    fichiers, aretes, visites, sortie, doute);
        }
    }

    /** Vrai quand le sommet est la racine de la hiérarchie en cours. */
    private static boolean estRacine(Sommet sommet, List<Sommet> sortie) {
        Sommet racine = sortie.get(0);
        return racine.nom().equals(sommet.nom()) && racine.uri().equals(sommet.uri());
    }

    /** Les appelants d'un nom sur le projet : chaque site d'appel résolu porte son arête. */
    private static List<Sommet> appelantsDe(String nom, List<FichierProjet> fichiers,
            Doute doute) {
        List<Sommet> sortie = new ArrayList<>();
        for (FichierProjet fichier : fichiers) {
            List<FlowAnalysis.AbsoluteToken> jetons = fichier.jetons();
            List<int[]> zones = zonesAEpargner(nom, fichier);
            for (int i = 0; i < jetons.size(); i++) {
                Sommet appelant = siteAppelant(jetons, i, nom, zones, fichier, doute);
                if (appelant != null) {
                    sortie.add(appelant);
                }
            }
        }
        return sortie;
    }

    /**
     * Le sommet appelant d'une occurrence du nom, ou null : l'appel nu
     * et celui de {@code this} portent l'arête, la construction ne
     * compte pas (la hiérarchie sert les méthodes), le récepteur
     * étranger, la référence de méthode et l'appel hors méthode
     * marquent le doute sans arête, la signature épargnée ne compte
     * pas.
     */
    private static @Nullable Sommet siteAppelant(List<FlowAnalysis.AbsoluteToken> jetons,
            int i, String nom, List<int[]> zones, FichierProjet fichier, Doute doute) {
        FlowAnalysis.AbsoluteToken jeton = jetons.get(i);
        if (!FlowAnalysis.isName(jeton.kind()) || !jeton.text().equals(nom)) {
            return null;
        }
        String avant = voisin(jetons, i - 1);
        boolean formeAppel = i + 1 < jetons.size() && jetons.get(i + 1).text().equals("(");
        if (!formeAppel) {
            doute.marquerSi(avant.equals("::"));
            return null;
        }
        if (dansZones(jeton.offset(), zones) || avant.equals("new")) {
            return null;
        }
        if (avant.equals(".") && !voisin(jetons, i - 2).equals("this")) {
            doute.marquer();
            return null;
        }
        Sommet porteuse = porteuse(fichier, jeton.offset());
        if (porteuse == null) {
            doute.marquer();
        }
        return porteuse;
    }

    /** La méthode porteuse d'un site, ou null hors méthode — l'appel ne se résout pas. */
    private static @Nullable Sommet porteuse(FichierProjet fichier, int offset) {
        OutlineEntry porteuse = Extraction.enclosing(fichier.doc(), fichier.analyse(),
                offset, offset + 1, NodeCategory.FUNCTION);
        return porteuse == null ? null : new Sommet(porteuse.name(), fichier.uri(), 0);
    }

    /**
     * Les zones de signatures à épargner : chaque méthode homonyme
     * porteuse donne sa zone de déclaration (jusqu'à l'accolade du
     * corps) ; la méthode brisée s'épargne entière — ses appels ne se
     * comptent pas.
     */
    private static List<int[]> zonesAEpargner(String nom, FichierProjet fichier) {
        List<int[]> zones = new ArrayList<>();
        for (OutlineEntry methode : fichier.methodes()) {
            if (!methode.name().equals(nom)) {
                continue;
            }
            int corps = MethodStructure.bodyOffset(FlowAnalysis.tokens(
                    fichier.doc(), fichier.analyse().lex(),
                    methode.startOffset(), methode.endOffset()));
            zones.add(corps < 0
                    ? new int[]{methode.startOffset(), methode.endOffset()}
                    : new int[]{methode.startOffset(), corps + 1});
        }
        return zones;
    }

    /** Vrai quand l'offset vit dans une des zones données. */
    private static boolean dansZones(int offset, List<int[]> zones) {
        for (int[] zone : zones) {
            if (offset >= zone[0] && offset < zone[1]) {
                return true;
            }
        }
        return false;
    }

    /** Les parents déclarés d'un type : les clauses lues à l'en-tête de sa déclaration. */
    private static List<String> parentsDe(FichierProjet fichier, OutlineEntry type) {
        List<FlowAnalysis.AbsoluteToken> jetons = FlowAnalysis.tokens(fichier.doc(),
                fichier.analyse().lex(), type.startOffset(), type.endOffset());
        return ClausesHeritage.de(jetons, indexDuCorps(jetons));
    }

    /** L'index de l'accolade ouvrante du corps dans les jetons du type, ou -1. */
    private static int indexDuCorps(List<FlowAnalysis.AbsoluteToken> jetons) {
        for (int i = 0; i < jetons.size(); i++) {
            if (jetons.get(i).text().equals("{")) {
                return i;
            }
        }
        return -1;
    }

    /** Les clauses d'un type s'ajoutent aux siennes, doublon jamais, ordre du source. */
    private static void ajouterClauses(Map<String, List<String>> parents, String nom,
            List<String> clauses) {
        List<String> connus = parents.computeIfAbsent(nom, cle -> new ArrayList<>());
        for (String clause : clauses) {
            if (!connus.contains(clause)) {
                connus.add(clause);
            }
        }
    }

    /** Le voisin d'un jeton par son index, chaîne vide aux bornes. */
    private static String voisin(List<FlowAnalysis.AbsoluteToken> jetons, int i) {
        return i >= 0 && i < jetons.size() ? jetons.get(i).text() : "";
    }

    /** Les fichiers analysés du projet, uris croissantes — l'ordre déterministe. */
    private static List<FichierProjet> analyser(Map<String, Document> projet,
            SyntaxService syntaxe) {
        List<FichierProjet> fichiers = new ArrayList<>();
        for (Map.Entry<String, Document> entree : new TreeMap<>(projet).entrySet()) {
            fichiers.add(new FichierProjet(entree.getKey(), entree.getValue(),
                    syntaxe.analyseFull(entree.getValue())));
        }
        return fichiers;
    }

    /** Les uris des premières déclarations de méthodes, nom vers uri. */
    private static Map<String, String> urisDesMethodes(List<FichierProjet> fichiers) {
        Map<String, String> uris = new LinkedHashMap<>();
        for (FichierProjet fichier : fichiers) {
            for (OutlineEntry methode : fichier.methodes()) {
                uris.putIfAbsent(methode.name(), fichier.uri());
            }
        }
        return uris;
    }

    /** La clé de visite d'un sommet : nom et uri. */
    private static String cle(String nom, String uri) {
        return nom + "@" + uri;
    }

    private static void requireNom(String nom) {
        Objects.requireNonNull(nom, "nom");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("nom must not be blank");
        }
    }

    /** Le doute de la hiérarchie d'appels : marqué une fois, lu à la fin. */
    private static final class Doute {
        private boolean marque;

        void marquer() {
            marque = true;
        }

        void marquerSi(boolean condition) {
            if (condition) {
                marque = true;
            }
        }

        boolean marque() {
            return marque;
        }
    }
}
