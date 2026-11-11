package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * La recherche dans le projet (A-21) : la requête se pose sur le projet
 * embarqué, hors ligne — la voie de l'index sert l'identifiant entier
 * (jamais les chaînes ni les commentaires), la voie du balayage sert
 * le reste ; la latence se consigne au banc (calibrage mesuré le
 * 2026-11-11 : 60 fichiers de 200 lignes, la voie de l'index 0 ms /
 * budget 200 ms, la voie du balayage 1 ms / budget 400 ms).
 */
class RechercheProjetTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    /** A-21 — la voie de l'index : l'identifiant entier se sert des occurrences comptées. */
    @Test
    void laVoieDeLIndex() {
        Map<String, Document> projet = projet(
                "Achat.java", """
                        class Achat {
                            int total(int quantite) {
                                return quantite;
                            }
                        }
                        """,
                "Etiquette.java", """
                        class Etiquette {
                            String total() {
                                return "total";   // total dans une chaîne et un commentaire
                            }
                        }
                        """);
        IndexSymboles index = index(projet);
        List<RechercheProjet.Trouvaille> trouvailles =
                RechercheProjet.chercher(projet, index, "total");
        assertEquals(2, trouvailles.size(),
                "deux déclarations — jamais la chaîne, jamais le commentaire");
        assertEquals("Achat.java", trouvailles.get(0).uri());
        assertEquals(22, trouvailles.get(0).offset());
        assertEquals("Etiquette.java", trouvailles.get(1).uri());
        assertEquals(29, trouvailles.get(1).offset());
    }

    /** A-21 — la voie du balayage : le texte partiel se cherche partout, chaînes comprises. */
    @Test
    void laVoieDuBalayage() {
        Map<String, Document> projet = projet(
                "Achat.java", "class Achat {\n    int total() {\n        return 0;\n    }\n}\n",
                "Etiquette.java", "class Etiquette {\n    String texte = \"total\";\n}\n");
        List<RechercheProjet.Trouvaille> trouvailles =
                RechercheProjet.chercher(projet, null, "otal");
        assertEquals(2, trouvailles.size(), "le texte partiel se trouve partout");
        assertEquals("Achat.java", trouvailles.get(0).uri(), "les uris du dictionnaire d'abord");
        assertEquals(23, trouvailles.get(0).offset(), "les offsets croissants ensuite");
        assertEquals("Etiquette.java", trouvailles.get(1).uri());
        assertEquals("total", extrait(projet, trouvailles.get(1)),
                "la chaîne vivante compte au balayage");
    }

    /** A-21 — l'équivalence : la voie de l'index égale le balayage par mot entier à casse exacte. */
    @Test
    void lequivalenceDesVoies() {
        Map<String, Document> projet = projet(
                "Achat.java", """
                        class Achat {
                            int total() {
                                int sousTotal = total();
                                return sousTotal;
                            }
                        }
                        """,
                "Facture.java", """
                        class Facture {
                            int total;
                        }
                        """);
        IndexSymboles index = index(projet);
        List<RechercheProjet.Trouvaille> parIndex =
                RechercheProjet.chercher(projet, index, "total");
        List<RechercheProjet.Trouvaille> parBalayage = motEntier(projet, "total");
        assertEquals(parBalayage, parIndex,
                "l'index sert exactement le mot entier à casse exacte");
        assertEquals(3, parIndex.size(), "deux déclarations et l'appel");
    }

    /** A-21 — la latence se consigne : 60 fichiers de 200 lignes, budgets tenus. */
    @Test
    void laLatenceSeConsigne() {
        Map<String, Document> projet = new LinkedHashMap<>();
        StringBuilder corps = new StringBuilder();
        for (int ligne = 0; ligne < 200; ligne++) {
            corps.append("    int valeur").append(ligne).append(" = ").append(ligne).append(";\n");
        }
        for (int fichier = 0; fichier < 60; fichier++) {
            projet.put("Fichier" + fichier + ".java",
                    Document.of("class Fichier" + fichier + " {\n" + corps + "}\n"));
        }
        IndexSymboles index = index(projet);

        long debut = System.nanoTime();
        List<RechercheProjet.Trouvaille> parIndex =
                RechercheProjet.chercher(projet, index, "valeur100");
        long indexMs = (System.nanoTime() - debut) / 1_000_000;

        debut = System.nanoTime();
        RechercheProjet.chercher(projet, null, "valeur100 = 100");
        long balayageMs = (System.nanoTime() - debut) / 1_000_000;

        assertEquals(60, parIndex.size(), "chaque fichier porte sa déclaration");
        System.out.printf("A-21 latence : index %d ms / budget 200 ms, balayage %d ms / budget 400 ms%n",
                indexMs, balayageMs);
        assertTrue(indexMs < 200, "la voie de l'index : " + indexMs + " ms / budget 200 ms");
        assertTrue(balayageMs < 400, "la voie du balayage : " + balayageMs + " ms / budget 400 ms");
    }

    /** A-21 — le motif vide refuse d'exception. */
    @Test
    void leMotifVideRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> RechercheProjet.chercher(Map.of(), null, ""));
    }

    /** Le projet embarqué : les fichiers nommés, dans l'ordre donné. */
    private Map<String, Document> projet(String... paires) {
        Map<String, Document> projet = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            projet.put(paires[i], Document.of(paires[i + 1]));
        }
        return projet;
    }

    /** L'index des fichiers du projet, sur l'analyse Java de chacun. */
    private IndexSymboles index(Map<String, Document> projet) {
        IndexSymboles index = IndexSymboles.vide();
        for (Map.Entry<String, Document> fichier : projet.entrySet()) {
            Analysis analyse = java.analyseFull(fichier.getValue());
            index.remplacer(fichier.getKey(), fichier.getValue(), analyse);
        }
        return index;
    }

    /** Le mot entier à casse exacte, par balayage — l'oracle de l'équivalence. */
    private List<RechercheProjet.Trouvaille> motEntier(Map<String, Document> projet,
            String motif) {
        List<RechercheProjet.Trouvaille> trouvailles = new ArrayList<>();
        for (Map.Entry<String, Document> fichier : new java.util.TreeMap<>(projet).entrySet()) {
            String texte = fichier.getValue().text();
            int offset = texte.indexOf(motif);
            while (offset >= 0) {
                boolean debut = offset == 0
                        || !Character.isJavaIdentifierPart(texte.charAt(offset - 1));
                boolean fin = offset + motif.length() >= texte.length()
                        || !Character.isJavaIdentifierPart(
                                texte.charAt(offset + motif.length()));
                if (debut && fin) {
                    trouvailles.add(new RechercheProjet.Trouvaille(fichier.getKey(), offset));
                }
                offset = texte.indexOf(motif, offset + motif.length());
            }
        }
        return trouvailles;
    }

    /** Le mot de la trouvaille, relu sur le document. */
    private String extrait(Map<String, Document> projet, RechercheProjet.Trouvaille trouvaille) {
        Document document = projet.get(trouvaille.uri());
        assertTrue(document != null, "le fichier trouvé vit dans le projet");
        String texte = document.text();
        return texte.substring(trouvaille.offset() - 1, trouvaille.offset() + 4);
    }
}
