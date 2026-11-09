package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

/**
 * Les tubes à file du module (ADR-015) : une direction dont l'écriture
 * gare des octets et la lecture les attend — l'ordre tient, l'octet
 * seul s'enchaîne avec les trames, la fermeture coupe l'écriture et
 * laisse la lecture vider ce qui reste (jamais la sémantique de
 * VIVACITÉ des tubes du JDK, constat M8.6 : la lecture ne suit pas la
 * mort du fil écrivain).
 */
class TubesMemoireTest {

    /** L'ordre tient : octet seul, trame, octet seul — tout sort dans l'ordre d'entrée. */
    @Test
    void lordreDesOctetsTientEntreTramesEtOctetsSeuls() throws Exception {
        TubesMemoire.Direction direction = TubesMemoire.creer();
        OutputStream sortie = direction.sortie();
        InputStream entree = direction.entree();
        sortie.write(65);
        sortie.write("BCD".getBytes(StandardCharsets.UTF_8));
        sortie.write(69);
        byte[] lus = new byte[5];
        int total = 0;
        while (total < 5) {
            int n = entree.read(lus, total, 5 - total);
            assertTrue(n > 0, "la lecture bloque jusqu'aux octets, jamais moins");
            total += n;
        }
        assertTrue(java.util.Arrays.equals("ABCDE".getBytes(StandardCharsets.UTF_8), lus),
                "l'ordre d'entrée est l'ordre de sortie");
    }

    /** La fermeture : l'écriture suivante échoue, la lecture vide le reste puis rend la fin. */
    @Test
    void laFermetureCoupleLEcritureEtAcheveLaLecture() throws Exception {
        TubesMemoire.Direction direction = TubesMemoire.creer();
        OutputStream sortie = direction.sortie();
        InputStream entree = direction.entree();
        sortie.write("reste".getBytes(StandardCharsets.UTF_8));
        sortie.close();
        assertThrows(IOException.class, () -> sortie.write(88),
                "l'écriture après fermeture échoue");
        byte[] tampon = new byte[10];
        int total = 0;
        int lus;
        while ((lus = entree.read(tampon, total, tampon.length - total)) >= 0) {
            total += lus;
        }
        assertTrue(total >= 5, "la lecture vide ce qui restait avant la fin");
        assertTrue(new String(tampon, 0, total, StandardCharsets.UTF_8).startsWith("reste"),
                "le reste arrive avant la fin de flux");
    }

    /** La lecture d'un octet seul : le premier octet attendu, pas un de plus. */
    @Test
    void laLectureOctetParOctetAttendChaqueOctet() throws Exception {
        TubesMemoire.Direction direction = TubesMemoire.creer();
        direction.sortie().write("xy".getBytes(StandardCharsets.UTF_8));
        InputStream entree = direction.entree();
        assertTrue(entree.read() == 'x', "le premier octet sort le premier");
        assertTrue(entree.read() == 'y', "le deuxième suit");
    }
}
