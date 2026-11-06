package jo.celeris.core.lsp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Le décodeur de trames du transport à flux : avale les octets par
 * morceaux de toute taille, met de côté chaque charge complète, rend
 * les messages prêts à la demande — jamais un message partiel. Les
 * en-têtes se suivent jusqu'à la ligne vide, dans n'importe quel ordre
 * (Content-Type toléré) ; l'en-tête Content-Length est exigé.
 */
public final class DecodeurTrames {

    private enum Etat {
        ENTETE, CORPS
    }

    private final List<String> pretes = new ArrayList<>();
    private final StringBuilder entete = new StringBuilder();
    private byte[] corps = new byte[64];
    private int tailleCorps;
    private Etat etat = Etat.ENTETE;
    private long longueurRestante = -1;

    /**
     * Avale des octets.
     *
     * @param tampon   le tampon reçu
     * @param debut    le premier octet utile
     * @param longueur le nombre d'octets utiles
     */
    public void ajouter(byte[] tampon, int debut, int longueur) {
        for (int i = 0; i < longueur; i++) {
            avaler(tampon[debut + i]);
        }
    }

    /** Avale tout le tampon. */
    public void ajouter(byte[] octets) {
        ajouter(octets, 0, octets.length);
    }

    private void avaler(byte octet) {
        if (etat == Etat.CORPS) {
            if (tailleCorps == corps.length) {
                byte[] plusGrand = new byte[corps.length * 2];
                System.arraycopy(corps, 0, plusGrand, 0, corps.length);
                corps = plusGrand;
            }
            corps[tailleCorps++] = octet;
            longueurRestante--;
            if (longueurRestante == 0) {
                terminerTrame();
            }
            return;
        }
        avalerEntete(octet);
    }

    private void terminerTrame() {
        pretes.add(new String(corps, 0, tailleCorps, StandardCharsets.UTF_8));
        tailleCorps = 0;
        longueurRestante = -1;
        etat = Etat.ENTETE;
    }

    private void avalerEntete(byte octet) {
        entete.append((char) (octet & 0xff));
        int taille = entete.length();
        if (taille >= 2 && entete.charAt(taille - 2) == '\r'
                && entete.charAt(taille - 1) == '\n') {
            traiterLigneEntete();
        }
    }

    private void traiterLigneEntete() {
        String ligne = entete.substring(0, entete.length() - 2);
        entete.setLength(0);
        if (ligne.isEmpty()) {
            if (longueurRestante < 0) {
                throw new IllegalStateException("frame without Content-Length header");
            }
            etat = Etat.CORPS;
            return;
        }
        int deuxPoints = ligne.indexOf(':');
        if (deuxPoints < 0) {
            throw new IllegalStateException("malformed header line");
        }
        String nom = ligne.substring(0, deuxPoints).trim();
        if ("Content-Length".equalsIgnoreCase(nom)) {
            longueurRestante = Long.parseLong(ligne.substring(deuxPoints + 1).trim());
        }
    }

    /**
     * Les charges complètes reçues depuis le dernier appel — la liste se
     * vide à chaque lecture.
     *
     * @return les messages JSON décodés, dans l'ordre d'arrivée
     */
    public List<String> messages() {
        if (pretes.isEmpty()) {
            return List.of();
        }
        List<String> rendues = new ArrayList<>(pretes);
        pretes.clear();
        return rendues;
    }
}
