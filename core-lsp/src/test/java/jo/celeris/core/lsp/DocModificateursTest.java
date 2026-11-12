package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.text.Document;

/**
 * La documentation des modificateurs au survol (C-10) : le nom connu
 * seul décide — l'identifiant qui porte le curseur se lit, la table
 * sert sa documentation en markdown, sa plage aux bornes exactes ;
 * la forme se vérifie au passage (le modificateur se nomme en position
 * d'appel, le récepteur se survole en toute position) ; les refus se
 * signalent par un survol nul, jamais par un texte faux.
 */
class DocModificateursTest {

    private static final String ECHEC = """
            @Composable
            fun TitreCarte(nom: String) {
                Text(text = nom, modifier = Modifier.padding(16.dp))
            }
            """;

    private int auCurseurDe(String expression) {
        int index = ECHEC.indexOf(expression);
        assertTrue(index >= 0, "expression absente du corpus : " + expression);
        return index;
    }

    /** C-10 — le modificateur survolé sert sa documentation, sa plage aux bornes exactes. */
    @Test
    void leModificateurSurvole() {
        SurvolLsp survol = DocModificateurs.survol(Document.of(ECHEC),
                auCurseurDe("padding") + 2);
        assertNotNull(survol, "le modificateur connu survolé doit servir sa documentation");
        assertEquals("markdown", survol.genre());
        assertTrue(survol.texte().startsWith("**padding** — modificateur d'espacement"),
                "la documentation s'ouvre sur le nom et son genre : " + survol.texte());
        assertTrue(survol.texte().contains("```kotlin"),
                "la signature vivante se clôture en Kotlin (PIEGE-16)");
        assertNotNull(survol.plage(), "la plage du survol dit les bornes");
        PlageLsp plage = survol.plage();
        assertEquals(auCurseurDe("padding"), PositionsLsp.bornes(
                Document.of(ECHEC), plage)[0],
                "la plage commence au mot survolé");
        assertEquals(auCurseurDe("padding") + "padding".length(), PositionsLsp.bornes(
                Document.of(ECHEC), plage)[1],
                "la plage finit au mot survolé");
    }

    /** C-10 — le récepteur des modificateurs se survole en toute position. */
    @Test
    void leRecepteurSeSurvoleEnToutePosition() {
        SurvolLsp survol = DocModificateurs.survol(Document.of(ECHEC),
                auCurseurDe("Modifier") + 1);
        assertNotNull(survol);
        assertTrue(survol.texte().startsWith("**Modifier** — le récepteur"));
        // le récepteur seul, sans appel derrière, se survole aussi
        String reference = "val base = Modifier\n";
        assertNotNull(DocModificateurs.survol(Document.of(reference),
                reference.indexOf("Modifier") + 1));
    }

    /** C-10 — le nom en référence (sans appel) ne se documente pas : la forme se vérifie. */
    @Test
    void leNomSansAppelRefuse() {
        String source = "val marge = padding\n";
        assertNull(DocModificateurs.survol(Document.of(source),
                source.indexOf("padding") + 1),
                "un nom connu sans position d'appel ne se documente pas");
    }

    /** C-10 — le nom inconnu, le blanc et la ponctuation se refusent. */
    @Test
    void leNomInconnuRefuse() {
        Document document = Document.of(ECHEC);
        assertNull(DocModificateurs.survol(document, auCurseurDe("Text") + 1),
                "le nom hors de la table ne se documente pas");
        assertNull(DocModificateurs.survol(document, auCurseurDe(" ") + 1),
                "le blanc ne porte pas de nom");
        assertNull(DocModificateurs.survol(document, ECHEC.indexOf('(')),
                "la ponctuation ne porte pas de nom");
    }

    /** C-10 — l'appel écarté d'un blanc se documente : la forme tolère l'espace. */
    @Test
    void lAppelEcarteDUnBlancSeDocumente() {
        String source = "Modifier.padding (16.dp)\n";
        SurvolLsp survol = DocModificateurs.survol(Document.of(source),
                source.indexOf("padding") + 1);
        assertNotNull(survol, "l'appel écarté d'un blanc reste une position d'appel");
    }

    /** C-10 — la documentation d'un nom connu se sert, l'inconnu rend null. */
    @Test
    void laDocumentationSeSertParNom() {
        String documentation = DocModificateurs.documentation("padding");
        assertNotNull(documentation);
        assertTrue(documentation.contains("L'espace autour du contenu"));
        assertNull(DocModificateurs.documentation("inconnu"));
    }

    /** C-10 — le curseur hors bornes se refuse d'entrée. */
    @Test
    void leCurseurHorsBornesRefuseDEntree() {
        Document document = Document.of(ECHEC);
        assertThrows(IllegalArgumentException.class,
                () -> DocModificateurs.survol(document, -1));
        assertThrows(IllegalArgumentException.class,
                () -> DocModificateurs.survol(document, document.length() + 1));
    }

    /** C-10 — la lecture est textuelle : le lexique ne décide pas ici (limite documentée). */
    @Test
    void laLectureEstTextuelle() {
        // limite documentée : un nom connu, en position d'appel APPARENTE dans une
        // chaîne, se documenterait — la lecture ne connaît pas de clôture de littéral
        String source = "val cle = \"padding(16)\";";
        assertNotNull(DocModificateurs.survol(Document.of(source),
                source.indexOf("padding") + 1),
                "limite documentée : la lecture est textuelle, jamais lexicale");
        // le nom SURVOLÉ seul, sans position d'appel, se refuse
        String reference = "val cle = \"padding\";";
        assertNull(DocModificateurs.survol(Document.of(reference),
                reference.indexOf("padding") + 1),
                "sans position d'appel, le nom se refuse");
    }
}
