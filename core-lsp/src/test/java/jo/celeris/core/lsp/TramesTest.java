package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le cadrage des trames du transport stdio (ADR-005) : en-tête
 * « Content-Length », charge UTF-8, réassemblage par morceaux — le flux
 * peut arriver en n'importe quels morceaux, la trame sort entière ou pas
 * du tout.
 */
class TramesTest {

    /** L'encodage : en-tête, saut, charge — la longueur compte les octets UTF-8. */
    @Test
    void lencodage() throws IOException {
        byte[] trame = CadreurTrames.tramer("{\"a\":1}");
        assertEquals("Content-Length: 7\r\n\r\n{\"a\":1}",
                new String(trame, StandardCharsets.UTF_8));
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        CadreurTrames.ecrire(sortie, "{}");
        assertEquals("Content-Length: 2\r\n\r\n{}", sortie.toString(StandardCharsets.UTF_8));
        // la longueur est en OCTETS : « é » compte pour deux, pas pour un
        byte[] accent = CadreurTrames.tramer("\"é\"");
        assertEquals(4, longueurEnTete(accent));
    }

    /** Le décodage : une trame entière d'un coup. */
    @Test
    void leDecodageDUnBloc() {
        DecodeurTrames decodeur = new DecodeurTrames();
        decodeur.ajouter(CadreurTrames.tramer("{\"a\":1}"));
        assertEquals(List.of("{\"a\":1}"), decodeur.messages());
        // épuisée : pas de message en attente
        assertTrue(decodeur.messages().isEmpty());
    }

    /** Le découpage arbitraire : octet par octet, la trame ne sort qu'entière. */
    @Test
    void leDecoupageOctetParOctet() {
        byte[] deux = concatener(CadreurTrames.tramer("[1]"),
                CadreurTrames.tramer("[2]"));
        DecodeurTrames decodeur = new DecodeurTrames();
        for (int i = 0; i < deux.length - 1; i++) {
            decodeur.ajouter(new byte[] {deux[i]});
            assertTrue(decodeur.messages().isEmpty(),
                    "rien avant la fin de la première trame (octet " + i + ")");
        }
        decodeur.ajouter(new byte[] {deux[deux.length - 1]});
        assertEquals(List.of("[1]", "[2]"), decodeur.messages());
    }

    /** Les morceaux de tailles inégales et les en-têtes additionnels tolérés. */
    @Test
    void lesMorceauxInegaux() {
        byte[] trame = ("Content-Length: 6\r\n"
                + "Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n"
                + "\r\n\"a\":12").getBytes(StandardCharsets.UTF_8);
        DecodeurTrames decodeur = new DecodeurTrames();
        decodeur.ajouter(trame, 0, 25);
        assertTrue(decodeur.messages().isEmpty());
        decodeur.ajouter(trame, 25, trame.length - 25);
        assertEquals(List.of("\"a\":12"), decodeur.messages());
    }

    /** La moitié d'une trame, puis l'autre — jamais de message partiel. */
    @Test
    void laMoitiePuisLeReste() {
        byte[] trame = CadreurTrames.tramer("{\"vrai\":true}");
        DecodeurTrames decodeur = new DecodeurTrames();
        decodeur.ajouter(trame, 0, trame.length / 2);
        assertTrue(decodeur.messages().isEmpty());
        decodeur.ajouter(trame, trame.length / 2, trame.length - trame.length / 2);
        assertEquals(List.of("{\"vrai\":true}"), decodeur.messages());
    }

    /** Les accents : la charge multi-octets sort décodée en UTF-8 intacte. */
    @Test
    void lesAccentsUtf8() {
        DecodeurTrames decodeur = new DecodeurTrames();
        decodeur.ajouter(CadreurTrames.tramer("\"éléganté\""));
        assertEquals(List.of("\"éléganté\""), decodeur.messages());
    }

    /** Aide : la valeur de l'en-tête Content-Length d'une trame encodée. */
    private static int longueurEnTete(byte[] trame) {
        String texte = new String(trame, StandardCharsets.UTF_8);
        int fin = texte.indexOf("\r\n");
        return Integer.parseInt(texte.substring("Content-Length: ".length(), fin));
    }

    /** Aide : concatène deux tableaux d'octets. */
    private static byte[] concatener(byte[] premier, byte[] second) {
        byte[] tout = new byte[premier.length + second.length];
        System.arraycopy(premier, 0, tout, 0, premier.length);
        System.arraycopy(second, 0, tout, premier.length, second.length);
        return tout;
    }
}
