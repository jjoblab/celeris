package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.KotlinSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.XmlSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Les critères du jalon testés sur projet Android réel — le corpus
 * embarqué (sortie M6) : une activité Java, un utilitaire Kotlin, un
 * manifeste, l'éditeurconfig du dépôt. L'index, les usages, la
 * recherche floue, la complétion, l'import, l'optimisation, le
 * renommage, la génération, le formatage et l'editorconfig y passent —
 * les mêmes critères que la matrice, sur des fichiers de la forme d'un
 * vrai projet. Contenu original, aucune pièce tierce (neutralité,
 * ADR-011).
 */
class CorpusAndroidTest {

    private static final String ACTIVITE = """
            package com.exemple.app;

            import android.os.Bundle;
            import java.util.List;

            public class MainActivity {
                private final String titre;
                private int compteur;
                private boolean actif;

                public MainActivity(String titre) {
                    this.titre = titre;
                    this.compteur = 0;
                    this.actif = false;
                }

                public void demarrer(Bundle etat) {
                    compteur = compteur + 1;
                    actif = true;
                    String rendu = Utilitaire.formater(compteur);
                }
            }
            """;

    private static final String UTILITAIRE = """
            package com.exemple.app

            class Utilitaire {
                fun formater(compteur: Int): String {
                    return "compteur : $compteur"
                }
            }
            """;

    private static final String MANIFESTE = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="Corpus">
                    <activity android:name=".MainActivity" />
                </application>
            </manifest>
            """;

    private static final String EDITORCONFIG = """
            root = true
            end_of_line = lf

            [*.kt]
            indent_style = space
            indent_size = 2

            [*.xml]
            indent_size = 2

            [*.java]
            indent_size = 4

            [*.kts]
            indent_style = tab
            """;

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);
    private final SyntaxService kotlin = SyntaxService.of(KotlinSyntax.INSTANCE);

    /** L'index et les usages : symboles des deux fichiers, occurrences à travers les uris. */
    @Test
    void lIndexEtLesUsagesSurLeCorpus() {
        IndexSymboles index = IndexSymboles.vide();
        Document activite = Document.of(ACTIVITE);
        Document utilitaire = Document.of(UTILITAIRE);
        index.remplacer("src/main/java/com/exemple/app/MainActivity.java",
                activite, java.analyseFull(activite));
        index.remplacer("src/main/java/com/exemple/app/Utilitaire.kt",
                utilitaire, kotlin.analyseFull(utilitaire));
        assertTrue(index.symboles().stream().anyMatch(s -> s.nom().equals("MainActivity")));
        assertTrue(index.symboles().stream().anyMatch(s -> s.nom().equals("Utilitaire")));
        // les usages de Utilitaire vivent dans DEUX fichiers : la déclaration
        // Kotlin, l'appel Java
        List<IndexSymboles.Usage> usages = index.usages("Utilitaire");
        assertTrue(usages.size() >= 2);
        assertTrue(usages.stream().anyMatch(u -> u.uri().endsWith(".kt")));
        assertTrue(usages.stream().anyMatch(u -> u.uri().endsWith(".java")));
        assertFalse(index.sature("src/main/java/com/exemple/app/MainActivity.java"));
    }

    /** La recherche floue : le préfixe tape, le symbole répond, ordonné par score. */
    @Test
    void laRechercheFloueSurLeCorpus() {
        IndexSymboles index = indexDuCorpus();
        List<Symbole> trouves = index.rechercher("mainact");
        assertFalse(trouves.isEmpty(), "mainact attrape MainActivity");
        assertEquals("MainActivity", trouves.get(0).nom());
        // la requête muette montre tout, l'ordre de la matrice tient
        List<Symbole> tout = index.rechercher("");
        assertTrue(tout.size() >= index.symboles().size());
    }

    /** Le renommage : le plan par occurrence, fichier par fichier, la collision signalée. */
    @Test
    void leRennomageSurLeCorpus() {
        IndexSymboles index = indexDuCorpus();
        Renommage.PlanRenommage plan = Renommage.plan(index, "Utilitaire", "Aide");
        assertTrue(plan.conflits().isEmpty());
        assertTrue(plan.editions().size() >= 2, "les editions couvrent les deux fichiers");
        // les éditions d'un fichier : ordre descendant, les offsets tiennent
        List<TextEdit> editions = plan.editions().get(
                "src/main/java/com/exemple/app/Utilitaire.kt");
        assertNotNull(editions);
        assertFalse(editions.isEmpty());
        // la collision : renommer vers un nom déjà déclaré — signalé, jamais tranché
        Renommage.PlanRenommage collision = Renommage.plan(index, "Utilitaire", "MainActivity");
        assertTrue(collision.conflits().stream()
                .anyMatch(c -> c.raison() == Renommage.RaisonConflit.COLLISION));
    }

    /** La complétion de base : les mots du code répondent, jamais ceux des chaînes. */
    @Test
    void laCompletionSurLeCorpus() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        int caret = doc.text().indexOf("compteur") + 4;
        List<String> candidats = CompletionMots.completer(doc, analyse, caret, List.of());
        assertTrue(candidats.contains("compteur"));
        // le préfixe commande : les mots d'un autre préfixe ne répondent pas
        assertFalse(candidats.contains("titre"));
        assertFalse(candidats.contains("actif"));
    }

    /** L'optimisation des imports : l'import mort signalé, jamais supprimé en silence. */
    @Test
    void loptimisationDesImportsSurLeCorpus() {
        Document doc = Document.of(ACTIVITE);
        List<OptimiseurImports.RapportImport> rapports =
                OptimiseurImports.analyser(doc, java.analyseFull(doc));
        assertTrue(rapports.stream().anyMatch(r -> r.importComplet()
                .equals("import java.util.List;")
                && !r.utilise()), "l'import mort est rapporte");
        assertTrue(rapports.stream().anyMatch(r -> r.importComplet()
                .equals("import android.os.Bundle;")
                && r.utilise()), "l'import vivant est rapporte");
    }

    /** La génération : champs typés du corpus, conventions tenues — le booléen en is. */
    @Test
    void laGenerationSurLeCorpus() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        List<Generateurs.Champ> champs = Generateurs.champs(doc, analyse, "MainActivity");
        assertEquals(3, champs.size());
        assertEquals("String", champs.get(0).type());
        assertEquals("int", champs.get(1).type());
        assertEquals("boolean", champs.get(2).type());
        String accesseurs = Generateurs.accesseurs(champs);
        assertTrue(accesseurs.contains("public boolean isActif()"));
        assertFalse(accesseurs.contains("getActif"));
        String constructeur = Generateurs.constructeur("MainActivity", champs);
        assertTrue(constructeur.contains(
                "public MainActivity(String titre, int compteur, boolean actif)"));
    }

    /** Le formatage : idempotent sur les trois langages du corpus, le style d'editorconfig. */
    @Test
    void leFormatageIdempotentSurLeCorpus() {
        EditorConfig config = EditorConfig.analyser(EDITORCONFIG);
        // java : quatre espaces, l'indentation faussée réparée puis stable
        String javaFaux = ACTIVITE.replace("    private", "  private")
                .replace("        this", "    this");
        Document docJava = Document.of(javaFaux);
        String javaUne = Formateur.formater(docJava, java.analyseFull(docJava),
                config.pour("src/main/java/com/exemple/app/MainActivity.java").indentStyle());
        Document docJavaDeux = Document.of(javaUne);
        String javaDeux = Formateur.formater(docJavaDeux, java.analyseFull(docJavaDeux),
                config.pour("src/main/java/com/exemple/app/MainActivity.java").indentStyle());
        assertEquals(javaUne, javaDeux, "reformater le formaté ne change rien (java)");
        // kotlin : deux espaces
        String ktFaux = UTILITAIRE.replace("    fun", "  fun");
        Document docKt = Document.of(ktFaux);
        IndentStyle styleKt = config.pour("src/main/java/com/exemple/app/Utilitaire.kt")
                .indentStyle();
        String ktUne = Formateur.formater(docKt, kotlin.analyseFull(docKt), styleKt);
        Document docKtDeux = Document.of(ktUne);
        assertEquals(ktUne, Formateur.formater(docKtDeux, kotlin.analyseFull(docKtDeux),
                styleKt), "idem (kotlin)");
        // xml : deux espaces
        String xmlFaux = MANIFESTE.replace("    <application", "  <application");
        SyntaxService xml = SyntaxService.of(XmlSyntax.INSTANCE);
        Document docXml = Document.of(xmlFaux);
        IndentStyle styleXml = config.pour("src/main/AndroidManifest.xml").indentStyle();
        String xmlUne = Formateur.formater(docXml, xml.analyseFull(docXml), styleXml);
        Document docXmlDeux = Document.of(xmlUne);
        assertEquals(xmlUne, Formateur.formater(docXmlDeux, xml.analyseFull(docXmlDeux),
                styleXml), "idem (xml)");
    }

    /** L'editorconfig : les règles du corpus résolues par extension, les fins de ligne tenues. */
    @Test
    void lEditorconfigSurLeCorpus() {
        EditorConfig config = EditorConfig.analyser(EDITORCONFIG);
        IndentStyle java = config.pour("src/main/java/com/exemple/app/MainActivity.java")
                .indentStyle();
        assertEquals(4, java.width());
        assertFalse(java.tabs());
        IndentStyle kotlin = config.pour("src/main/java/com/exemple/app/Utilitaire.kt")
                .indentStyle();
        assertEquals(2, kotlin.width());
        IndentStyle kts = config.pour("build.gradle.kts").indentStyle();
        assertTrue(kts.tabs(), "le kts prend les tabulations");
        assertEquals("lf", config.pour("src/main/AndroidManifest.xml").finDeLigne());
        assertEquals("\na\nb\n", EditorConfig.appliquerFins("\r\na\nb\n",
                config.pour("src/main/AndroidManifest.xml")));
    }

    private IndexSymboles indexDuCorpus() {
        IndexSymboles index = IndexSymboles.vide();
        Document activite = Document.of(ACTIVITE);
        Document utilitaire = Document.of(UTILITAIRE);
        index.remplacer("src/main/java/com/exemple/app/MainActivity.java",
                activite, java.analyseFull(activite));
        index.remplacer("src/main/java/com/exemple/app/Utilitaire.kt",
                utilitaire, kotlin.analyseFull(utilitaire));
        return index;
    }
}
