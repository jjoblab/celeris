package jo.celeris.core.lsp;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Le cadrage des trames du transport à flux (base protocol LSP) :
 * en-tête « Content-Length » comptant les OCTETS UTF-8 de la charge,
 * ligne vide, puis la charge. L'encodeur n'écrit que cela — le décodage
 * tolère, lui, les en-têtes additionnels.
 */
public final class CadreurTrames {

    private static final byte[] EN_TETE = "Content-Length: ".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SAUT = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private CadreurTrames() {
    }

    /**
     * La trame complète en octets.
     *
     * @param json la charge JSON
     * @return l'en-tête, le saut et la charge, prêts à écrire
     */
    public static byte[] tramer(String json) {
        byte[] charge = json.getBytes(StandardCharsets.UTF_8);
        byte[] longueur = Integer.toString(charge.length).getBytes(StandardCharsets.US_ASCII);
        byte[] trame = new byte[EN_TETE.length + longueur.length
                + SAUT.length + charge.length];
        int position = 0;
        System.arraycopy(EN_TETE, 0, trame, position, EN_TETE.length);
        position += EN_TETE.length;
        System.arraycopy(longueur, 0, trame, position, longueur.length);
        position += longueur.length;
        System.arraycopy(SAUT, 0, trame, position, SAUT.length);
        position += SAUT.length;
        System.arraycopy(charge, 0, trame, position, charge.length);
        return trame;
    }

    /**
     * Écrit la trame sur le flux.
     *
     * @param sortie le flux de sortie du transport
     * @param json   la charge JSON
     * @throws IOException si le flux échoue
     */
    public static void ecrire(OutputStream sortie, String json) throws IOException {
        sortie.write(tramer(json));
        sortie.flush();
    }
}
