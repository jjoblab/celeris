package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.lsp4j.SignatureHelp;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

/**
 * Le banc de l'aide aux paramètres : l'appel ouvert sous le curseur
 * reçoit la signature de SA déclaration — le libellé porte les
 * paramètres tels que déclarés, le paramètre courant se compte aux
 * virgules de premier niveau. Hors appel, après l'appel, dans
 * l'en-tête d'une déclaration : null — la popup se ferme au bon
 * moment, jamais elle ne s'ouvre à côté.
 */
class JavaSignatureTest {

    private static final String EXEMPLE = """
            class Compte {
                private int solde;
                int ajouter(int a, int b) {
                    return a + b;
                }
                void tenir() {
                    int total = ajouter(1, 2);
                }
                int mesurer(String unite, float valeur, boolean arrondir) {
                    return 0;
                }
                void appeler() {
                    int large = mesurer("cm", 3, true);
                }
            }
            """;

    private static @Nullable SignatureHelp a(String texte, int curseur) {
        JavaSource document = JavaSource.of(texte);
        return JavaSignature.at(document, JavaSymbols.of(document),
                curseur);
    }

    @Test
    void lAppelOuvertRecoitLaSignatureDeSaDeclaration() {
        int curseur = EXEMPLE.indexOf("ajouter(1,") + "ajouter(1,".length();
        SignatureHelp aide = a(EXEMPLE, curseur);
        assertNotNull(aide);
        assertEquals(1, aide.getSignatures().size());
        assertEquals("ajouter(int a, int b)",
                aide.getSignatures().get(0).getLabel());
        assertEquals(2, aide.getSignatures().get(0).getParameters().size());
        assertEquals("int a",
                aide.getSignatures().get(0).getParameters().get(0).getLabel().getLeft());
        assertEquals("int b",
                aide.getSignatures().get(0).getParameters().get(1).getLabel().getLeft());
        assertEquals(0, aide.getActiveSignature());
        assertEquals(1, aide.getActiveParameter());
    }

    @Test
    void leParametreCourantSeCompteAuxVirgules() {
        int curseur = EXEMPLE.indexOf("mesurer(\"cm\",") + "mesurer(\"cm\",".length();
        SignatureHelp aide = a(EXEMPLE, curseur);
        assertNotNull(aide);
        assertEquals("mesurer(String unite, float valeur, boolean arrondir)",
                aide.getSignatures().get(0).getLabel());
        // « "cm" » : la chaîne est dépouillée, sa virgule interne — s'il
        // y en avait une — ne compterait pas ; la première vraie compte
        assertEquals(1, aide.getActiveParameter());
    }

    @Test
    void leCurseurSurLOuverteRendLeParametreZero() {
        int curseur = EXEMPLE.indexOf("ajouter(1") + "ajouter(".length();
        SignatureHelp aide = a(EXEMPLE, curseur);
        assertNotNull(aide);
        assertEquals(0, aide.getActiveParameter());
    }

    @Test
    void lAppelImbriqueTrouveSonPropreNom() {
        String texte = """
                class A {
                    int ajouter(int a, int b) {
                        return a + b;
                    }
                    void corps() {
                        int x = ajouter(ajouter(1, 2), 3);
                    }
                }
                """;
        int curseur = texte.indexOf("ajouter(ajouter(1, 2)") + "ajouter(".length();
        SignatureHelp aide = a(texte, curseur);
        assertNotNull(aide);
        assertEquals(0, aide.getActiveParameter());
        curseur = texte.indexOf("ajouter(1, 2), 3)") + "ajouter(1,".length();
        aide = a(texte, curseur);
        assertNotNull(aide);
        assertEquals(1, aide.getActiveParameter());
    }

    @Test
    void horsAppelRienNeSOuvre() {
        // le curseur après l'appel complet — la frontière de
        // l'instruction est atteinte, jamais la déclaration d'en-tête
        int curseur = EXEMPLE.indexOf("ajouter(1, 2);") + "ajouter(1, 2);".length();
        assertNull(a(EXEMPLE, curseur));
        // le curseur avant l'appel, sur le nom
        assertNull(a(EXEMPLE, EXEMPLE.indexOf("ajouter(1")));
        // le curseur sur une ligne vide d'appel
        assertNull(a(EXEMPLE, EXEMPLE.indexOf("int solde;")));
    }

    @Test
    void lEnTeteDeDeclarationNeServPasLAide() {
        // le curseur DANS les paramètres de la déclaration
        int curseur = EXEMPLE.indexOf("int ajouter(int a") + "int ajouter(int ".length();
        assertNull(a(EXEMPLE, curseur));
    }

    @Test
    void leNomInconnuNeServRien() {
        String texte = "class A {\n    void corps() {\n        inconnu(1);\n    }\n}\n";
        int curseur = texte.indexOf("inconnu(1") + "inconnu(1".length();
        assertNull(a(texte, curseur));
    }

    @Test
    void leConstructeurAppelParNewRecoitSaDeclaration() {
        String texte = """
                class Point {
                    private int x;
                    Point(int x) {
                        this.x = x;
                    }
                    void origine() {
                        Point p = new Point(0);
                    }
                }
                """;
        int curseur = texte.indexOf("new Point(0)") + "new Point(".length();
        SignatureHelp aide = a(texte, curseur);
        assertNotNull(aide);
        assertEquals("Point(int x)", aide.getSignatures().get(0).getLabel());
        assertEquals(0, aide.getActiveParameter());
    }
}
