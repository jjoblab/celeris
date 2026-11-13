package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Les fichiers binaires (A-26) : la détection se lit sur les octets
 * bruts — l'octet nul et les contrôles hors texte décident, dans le
 * préfixe borné ; l'aperçu hexadécimal sert la lecture sûre, borné à
 * son plafond ; l'écriture par la voie texte SE REFUSE — jamais
 * l'aller-retour destructeur du décodage.
 */
class FichiersBinairesTest {

    private static final String TEXTE = "Bonjour, le texte accentue — tab\t, saut\n"
            + "de ligne, retour\r et saut de page\f.\n";

    /** A-26 — l'octet nul décide : le binaire se détecte d'entrée. */
    @Test
    void lOctetNulDecide() {
        assertTrue(DetectionBinaire.binaire(new byte[] {'a', 0x00, 'b'}),
                "l'octet nul refuse le texte");
        assertTrue(DetectionBinaire.binaire(new byte[] {0x00}),
                "le fichier d'un seul octet nul se détecte");
    }

    /** A-26 — les contrôles hors texte décident, les blancs du texte ne comptent pas. */
    @Test
    void lesControlesDecidentLesBlancsNon() {
        assertTrue(DetectionBinaire.binaire(new byte[] {'a', 0x01, 'b'}),
                "le contrôle 0x01 refuse le texte");
        assertTrue(DetectionBinaire.binaire(new byte[] {'a', 0x0B, 'b'}),
                "la tabulation verticale refuse le texte");
        assertTrue(DetectionBinaire.binaire(new byte[] {'a', 0x1F, 'b'}),
                "le contrôle du bloc haut refuse le texte");
        assertFalse(DetectionBinaire.binaire(TEXTE.getBytes(StandardCharsets.UTF_8)),
                "le texte avec ses blancs et ses accents ne se détecte pas");
        assertFalse(DetectionBinaire.binaire(new byte[0]),
                "le fichier vide ne se détecte pas");
    }

    /** A-26 — le préfixe borne la lecture : le signal tardif ne se voit pas (limite documentée). */
    @Test
    void lePrefixeBorneLaLecture() {
        byte[] octets = new byte[DetectionBinaire.PREFIXE + 16];
        java.util.Arrays.fill(octets, (byte) 'a');
        octets[DetectionBinaire.PREFIXE + 8] = 0x00;
        assertFalse(DetectionBinaire.binaire(octets),
                "l'octet nul au-delà du préfixe ne se voit pas — limite documentée");
        octets[DetectionBinaire.PREFIXE - 1] = 0x00;
        assertTrue(DetectionBinaire.binaire(octets),
                "l'octet nul dans le préfixe se voit, lui");
    }

    /** A-26 — l'aperçu hexadécimal : décalage, seize octets, gouttière des imprimables. */
    @Test
    void lApercuRendSesLignes() {
        byte[] octets = new byte[] {'A', 'c', 'c', 'u', 'e', 'i', 'l', 0x00, 'b', 'o', 'r', 'd'};
        HexadecimalPreview.Dump apercu = HexadecimalPreview.de(octets);
        assertFalse(apercu.tronque(), "douze octets tiennent sous le plafond");
        assertEquals(1, apercu.lignes().size());
        assertEquals("00000000  41 63 63 75 65 69 6C 00 62 6F 72 64 "
                        + " ".repeat(12) + " |Accueil.bord|",
                apercu.lignes().get(0),
                "le décalage, les octets en hexadécimal, la gouttière au point pour le nul");
    }

    /** A-26 — le plafond tient : 256 lignes, 4 Kio, le drapeau de coupe le dit. */
    @Test
    void lePlafondTient() {
        HexadecimalPreview.Dump plein = HexadecimalPreview.de(new byte[4096]);
        assertEquals(HexadecimalPreview.PLAFOND_LIGNES, plein.lignes().size(),
                "4 Kio exact : 256 lignes, pas de coupe");
        assertFalse(plein.tronque());
        HexadecimalPreview.Dump vaste = HexadecimalPreview.de(new byte[4097]);
        assertEquals(HexadecimalPreview.PLAFOND_LIGNES, vaste.lignes().size(),
                "l'octet de trop se coupe : le plafond tient");
        assertTrue(vaste.tronque(), "le drapeau de coupe se porte");
    }

    /** A-26 — le binaire se détecte au système virtuel, l'aperçu se sert, l'écriture se refuse. */
    @Test
    void leBinaireSeDetecteLApercuSeSertLEcritureSeRefuse() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.fichier("racine", "source.java", "class Source {}");
        arbre.fichierBinaire("racine", "image.png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x00, 0x0D, 0x0A, 0x1A, 0x0A});
        AdaptateurStockageDocuments systeme = new AdaptateurStockageDocuments(arbre, "racine");
        assertTrue(systeme.estBinaire(CheminDoc.de("/image.png")),
                "le binaire se détecte au système virtuel");
        assertFalse(systeme.estBinaire(CheminDoc.de("/source.java")),
                "le texte ne se détecte pas");
        assertFalse(systeme.estBinaire(CheminDoc.de("/absent.png")),
                "l'absent ne se détecte pas — il n'existe pas");
        String apercu = systeme.apercuBinaire(CheminDoc.de("/image.png"));
        assertNotNull(apercu, "l'aperçu du binaire se sert");
        assertTrue(apercu.startsWith("00000000  89 50 4E 47 00 0D 0A 1A 0A "),
                "l'aperçu rend les octets bruts : " + apercu);
        assertNull(systeme.apercuBinaire(CheminDoc.de("/source.java")),
                "le texte ne se prévoit pas");
        assertNull(systeme.apercuBinaire(CheminDoc.de("/absent.png")),
                "l'absent ne se prévoit pas");
        // le refus d'édition destructrice : jamais l'aller-retour
        assertThrows(FichierBinaireException.class, () -> systeme.ecrire(
                CheminDoc.de("/image.png"), "réécrit en texte"));
        // le texte s'écrit, lui — le garde ne bloque que le binaire
        systeme.ecrire(CheminDoc.de("/source.java"), "class Source { /* neuf */ }");
        assertEquals("class Source { /* neuf */ }",
                arbre.lire(arbre.enfants("racine").stream()
                        .filter(e -> e.nom().equals("source.java"))
                        .findFirst().orElseThrow().idDocument()));
    }

    /** A-26 — la mémoire de référence ne porte que du texte : ses défauts disent non. */
    @Test
    void laMemoireDeReferenceNePorteQueDuTexte() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        systeme.ecrire(CheminDoc.de("/note.txt"), TEXTE);
        assertFalse(systeme.estBinaire(CheminDoc.de("/note.txt")),
                "le texte en mémoire ne se détecte pas");
        assertNull(systeme.apercuBinaire(CheminDoc.de("/note.txt")),
                "la mémoire de texte ne prévoit rien");
    }

    /** L'arbre en mémoire : les identifiants sont des compteurs, les octets bruts. */
    private static final class ArbreFaux implements ArbreDocuments {
        private final java.util.Map<String, java.util.List<EntreeDocument>> dossiers =
                new java.util.LinkedHashMap<>();
        private final java.util.Map<String, byte[]> fichiers = new java.util.LinkedHashMap<>();
        private int compteur;

        ArbreFaux() {
            dossiers.put("racine", new java.util.ArrayList<>());
        }

        void fichier(String idParent, String nom, String contenu) {
            fichiers.put("doc-" + compteur,
                    contenu.getBytes(StandardCharsets.UTF_8));
            dossiers.get(idParent).add(new EntreeDocument(nom, "doc-" + compteur, false));
            compteur++;
        }

        void fichierBinaire(String idParent, String nom, byte[] octets) {
            fichiers.put("doc-" + compteur, octets.clone());
            dossiers.get(idParent).add(new EntreeDocument(nom, "doc-" + compteur, false));
            compteur++;
        }

        @Override
        public java.util.List<EntreeDocument> enfants(String idDossier) {
            return java.util.List.copyOf(dossiers.getOrDefault(idDossier, java.util.List.of()));
        }

        @Override
        public String lire(String idFichier) {
            return new String(octetsDe(idFichier), StandardCharsets.UTF_8);
        }

        @Override
        public byte[] lireOctets(String idFichier) {
            return octetsDe(idFichier).clone();
        }

        private byte[] octetsDe(String idFichier) {
            byte[] octets = fichiers.get(idFichier);
            if (octets == null) {
                throw new IllegalStateException("not a file: " + idFichier);
            }
            return octets;
        }

        @Override
        public void remplacer(String idFichier, String contenu) {
            fichiers.put(idFichier, contenu.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String creer(String idParent, String nom, boolean dossier) {
            String id = "doc-" + compteur++;
            if (dossier) {
                dossiers.put(id, new java.util.ArrayList<>());
            } else {
                fichiers.put(id, new byte[0]);
            }
            dossiers.get(idParent).add(new EntreeDocument(nom, id, dossier));
            return id;
        }

        @Override
        public boolean estFichier(String idDocument) {
            return fichiers.containsKey(idDocument);
        }
    }
}
