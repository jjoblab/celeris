package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SymboleLsp;









import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Les tables du terrain de la couverture (H-12) — les réponses du
 * serveur de référence aux méthodes nouvelles du jalon : symboles,
 * plis, formatage, surlignages, renommage, correctifs, indices, jetons
 * et références, lus sur le document suivi que le serveur procure.
 * Servies par le protocole, jamais par l'appel direct.
 */
final class TablesCouverture {

    private final Function<String, @Nullable Document> documentsSuivis;

    TablesCouverture(Function<String, @Nullable Document> documentsSuivis) {
        this.documentsSuivis = documentsSuivis;
    }

    // ── La couverture du jalon (H-12) : les tables du terrain,
    //    servies par le protocole — jamais par l'appel direct. ──────

    /**
     * Les symboles (H-12) : les déclarations du document — chaque
     * « mot-clé nom( » ouvre un symbole de fonction, sa plage étendue
     * à la ligne du nom.
     */
    List<SymboleLsp> symboles(String uri) {
        Document document = documentsSuivis.apply(uri);
        List<SymboleLsp> symboles = new ArrayList<>();
        if (document == null) {
            return symboles;
        }
        String texte = document.text();
        int ligne = 0;
        int debutLigne = 0;
        for (int i = 0; i <= texte.length(); i++) {
            if (i == texte.length() || texte.charAt(i) == '\n') {
                String ligneTexte = texte.substring(debutLigne,
                        i < texte.length() ? i : texte.length());
                String[] mots = ligneTexte.strip().split("[\\s(]+");
                if (mots.length >= 2 && !mots[0].isEmpty()
                        && Character.isJavaIdentifierStart(mots[0].charAt(0))) {
                    int colonne = ligneTexte.indexOf(mots[1]);
                    PlageLsp plage = new PlageLsp(new PositionLsp(ligne, 0),
                            new PositionLsp(ligne, ligneTexte.strip().length()));
                    symboles.add(new SymboleLsp(mots[1], 12, plage,
                            new PlageLsp(new PositionLsp(ligne, colonne),
                                    new PositionLsp(ligne,
                                            colonne + mots[1].length())),
                            List.of()));
                }
                ligne++;
                debutLigne = i + 1;
            }
        }
        return symboles;
    }

    /**
     * Les plis (H-12) : les accolades ouvrantes à leur fermante — le
     * repli du terrain, fin EXCLUSIVE au domaine.
     */
    List<PliLsp> plis(String uri) {
        Document document = documentsSuivis.apply(uri);
        List<PliLsp> plis = new ArrayList<>();
        if (document == null) {
            return plis;
        }
        String texte = document.text();
        java.util.Deque<Integer> ouvertes = new java.util.ArrayDeque<>();
        int ligne = 0;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '\n') {
                ligne++;
            } else if (c == '{') {
                ouvertes.push(ligne);
            } else if (c == '}' && !ouvertes.isEmpty()) {
                int debut = ouvertes.pop();
                if (ligne > debut) {
                    plis.add(new PliLsp(debut, ligne + 1));
                }
            }
        }
        return plis;
    }

    /**
     * Le formatage (H-12) : l'alignement à l'amorce de la ligne —
     * quatre espaces par niveau d'accolade, la table simple du terrain.
     */
    List<EditionTexteLsp> formatage(String uri, OptionsFormatageLsp options) {
        Document document = documentsSuivis.apply(uri);
        List<EditionTexteLsp> editions = new ArrayList<>();
        if (document == null) {
            return editions;
        }
        String[] lignes = document.text().split("\n", -1);
        int niveau = 0;
        for (int numero = 0; numero < lignes.length; numero++) {
            String ligneTexte = lignes[numero];
            String nue = ligneTexte.strip();
            if (nue.startsWith("}")) {
                niveau = Math.max(0, niveau - 1);
            }
            String amorce = " ".repeat(options.espaces()
                    ? options.largeurTab() * niveau : 0);
            if (!ligneTexte.equals(amorce + nue) && !nue.isEmpty()) {
                editions.add(new EditionTexteLsp(
                        new PlageLsp(new PositionLsp(numero, 0),
                                new PositionLsp(numero, ligneTexte.length())),
                        amorce + nue));
            }
            int ouvrantes = nue.length() - nue.replace("{", "").length();
            int fermantes = nue.length() - nue.replace("}", "").length();
            niveau = Math.max(0, niveau + ouvrantes - fermantes);
        }
        return editions;
    }

    /**
     * Les surlignages (H-12) : les occurrences du symbole sous la
     * position — le mot entier, chaque fois qu'il paraît.
     */
    List<SurlignageLsp> surlignages(String uri, PositionLsp position) {
        Document document = documentsSuivis.apply(uri);
        List<SurlignageLsp> surlignages = new ArrayList<>();
        if (document == null) {
            return surlignages;
        }
        String symbole = PopupCoach.symboleA(document,
                PositionsLsp.offset(document, position));
        if (symbole == null || symbole.isEmpty()) {
            return surlignages;
        }
        String texte = document.text();
        int ligne = 0;
        int debutLigne = 0;
        for (int i = 0; i <= texte.length(); i++) {
            if (i == texte.length() || texte.charAt(i) == '\n') {
                chercherOccurrences(texte.substring(debutLigne,
                        i < texte.length() ? i : texte.length()), symbole,
                        ligne, surlignages);
                ligne++;
                debutLigne = i + 1;
            }
        }
        return surlignages;
    }

    /** Les occurrences d'un symbole dans une ligne — le mot entier, pas la sous-chaîne. */
    private static void chercherOccurrences(String ligneTexte, String symbole,
            int numero, List<SurlignageLsp> surlignages) {
        java.util.regex.Matcher mots = MOT.matcher(ligneTexte);
        while (mots.find()) {
            if (mots.group().equals(symbole)) {
                surlignages.add(new SurlignageLsp(new PlageLsp(
                        new PositionLsp(numero, mots.start()),
                        new PositionLsp(numero, mots.end())), 1));
            }
        }
    }

    /** Le mot d'identifiant — la convention du terrain. */
    private static final java.util.regex.Pattern MOT =
            java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * La préparation du renommage (H-12) : la plage du mot sous la
     * position — null quand la position ne porte pas de nom.
     */
    @Nullable PreparationRennomageLsp preparerRennomage(String uri,
            PositionLsp position) {
        Document document = documentsSuivis.apply(uri);
        if (document == null) {
            return null;
        }
        String symbole = PopupCoach.symboleA(document,
                PositionsLsp.offset(document, position));
        if (symbole == null || symbole.isEmpty()) {
            return null;
        }
        PlageLsp plage = plageDuMot(document, position);
        return new PreparationRennomageLsp(plage, symbole);
    }

    /**
     * Le renommage (H-12) : chaque occurrence remplacée — l'édition
     * d'espace du terrain, une édition par occurrence.
     */
    EditionEspaceLsp rennomage(String uri, PositionLsp position,
            String nouveauNom) {
        Document document = documentsSuivis.apply(uri);
        if (document == null) {
            return EditionEspaceLsp.VIDE;
        }
        String symbole = PopupCoach.symboleA(document,
                PositionsLsp.offset(document, position));
        if (symbole == null || symbole.isEmpty()) {
            return EditionEspaceLsp.VIDE;
        }
        List<EditionTexteLsp> editions = new ArrayList<>();
        surlignages(uri, position).forEach(surlignage -> editions.add(
                new EditionTexteLsp(surlignage.plage(), nouveauNom)));
        return new EditionEspaceLsp(
                List.of(new jo.celeris.core.lsp.types.EditionDocumentLsp(uri, -1,
                        editions)),
                List.of());
    }

    /**
     * Les correctifs (H-12) : envelopper la sélection — le terrain
     * n'en sert qu'un, l'édition vide n'engage à rien.
     */
    List<CorrectifLsp> correctifs(String uri, PlageLsp plage) {
        return List.of(new CorrectifLsp("Entourer d'accolades",
                EditionEspaceLsp.VIDE));
    }

    /**
     * Les indices (H-12) : le type inféré d'une déclaration
     * « nom = valeur » — la valeur sert d'amorce au type.
     */
    List<IndiceLsp> indices(String uri, PlageLsp plage) {
        Document document = documentsSuivis.apply(uri);
        List<IndiceLsp> indices = new ArrayList<>();
        if (document == null) {
            return indices;
        }
        String texte = document.text();
        int ligne = 0;
        int debutLigne = 0;
        for (int i = 0; i <= texte.length(); i++) {
            if (i == texte.length() || texte.charAt(i) == '\n') {
                String ligneTexte = texte.substring(debutLigne,
                        i < texte.length() ? i : texte.length());
                int egal = ligneTexte.indexOf('=');
                if (egal > 0 && plage.debut().ligne() <= ligne
                        && ligne <= plage.fin().ligne()) {
                    String nom = ligneTexte.substring(0, egal).strip();
                    String valeur = ligneTexte.substring(egal + 1).strip();
                    if (!nom.isEmpty() && !valeur.isEmpty()
                            && estNombre(valeur)) {
                        indices.add(new IndiceLsp(
                                new PositionLsp(ligne,
                                        nom.length() + 1),
                                List.of(new IndiceLsp.Partie(
                                        ": " + typeDe(valeur), null)),
                                1, false, false));
                    }
                }
                ligne++;
                debutLigne = i + 1;
            }
        }
        return indices;
    }

    /** Vrai quand la valeur s'écrit en chiffres — le type s'y lit. */
    private static boolean estNombre(String valeur) {
        return valeur.matches("-?\\d+(\\.\\d+)?");
    }

    /** Le type d'une valeur numérique — entier ou décimal. */
    private static String typeDe(String valeur) {
        return valeur.contains(".") ? "Double" : "Int";
    }

    /**
     * Les jetons (H-12) : chaque identifiant de déclaration porte le
     * type « variable » — la légende convenue à l'initialisation du
     * terrain : [variable].
     */
    SemanticTokensLsp jetons(String uri) {
        Document document = documentsSuivis.apply(uri);
        if (document == null) {
            return SemanticTokensLsp.de(null, List.of());
        }
        String texte = document.text();
        List<Integer> donnees = new ArrayList<>();
        int ligne = 0;
        int debutLigne = 0;
        for (int i = 0; i <= texte.length(); i++) {
            if (i == texte.length() || texte.charAt(i) == '\n') {
                String ligneTexte = texte.substring(debutLigne,
                        i < texte.length() ? i : texte.length());
                java.util.regex.Matcher mots = MOT.matcher(ligneTexte);
                while (mots.find()) {
                    if (Character.isLowerCase(mots.group().charAt(0))) {
                        donnees.add(ligne);
                        donnees.add(mots.start());
                        donnees.add(mots.end() - mots.start());
                        donnees.add(0);
                        donnees.add(0);
                    }
                }
                ligne++;
                debutLigne = i + 1;
            }
        }
        return SemanticTokensLsp.de("terrain-" + uri.hashCode(), donnees);
    }

    /**
     * Les références (H-12) : chaque occurrence du symbole devient un
     * lieu du document — la table du terrain, l'uri du suivi.
     */
    List<LieuLsp> references(String uri, PositionLsp position) {
        List<LieuLsp> lieux = new ArrayList<>();
        for (SurlignageLsp surlignage : surlignages(uri, position)) {
            lieux.add(new LieuLsp(uri, surlignage.plage()));
        }
        return lieux;
    }

    /** La plage du mot sous la position — ses bornes d'identifiant réelles. */
    private static @Nullable PlageLsp plageDuMot(Document document,
            PositionLsp position) {
        int offset = PositionsLsp.offset(document, position);
        String texte = document.text();
        int debut = Math.min(offset, Math.max(0, texte.length() - 1));
        int fin = debut;
        while (debut > 0 && Character.isJavaIdentifierPart(texte.charAt(debut - 1))) {
            debut--;
        }
        while (fin < texte.length()
                && Character.isJavaIdentifierPart(texte.charAt(fin))) {
            fin++;
        }
        if (debut >= fin) {
            return null;
        }
        return new PlageLsp(PositionsLsp.position(document, debut),
                PositionsLsp.position(document, fin));
    }
}
