package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * L'aperçu hexadécimal d'un fichier binaire (A-26) : la lecture SÛRE,
 * bornée — chaque ligne porte son décalage (huit chiffres), seize
 * octets en hexadécimal, et le gouttière des caractères imprimables
 * (le point ailleurs). Le plafond tient : 256 lignes, 4 Kio — le
 * fichier plus vaste se coupe, le drapeau le dit — jamais la mémoire
 * de l'hôte ne porte le binaire entier pour le montrer.
 *
 * <p>L'aperçu n'engage à rien : il ne se réécrit pas, il ne nourrit
 * pas l'édition — la session qui le montre se pose en lecture seule
 * (le refus d'édition destructrice, A-26 : le binaire ne repart
 * JAMAIS par la voie texte).</p>
 */
public final class HexadecimalPreview {

    /** Le plafond de lignes : 256 lignes, 4 Kio d'octets. */
    public static final int LINES_LIMIT = 256;

    /** Les octets par ligne : seize, le classique du vidage. */
    public static final int BYTES_PER_LINE = 16;

    private HexadecimalPreview() {
    }

    /**
     * L'aperçu borné : les lignes du vidage et son drapeau de coupe.
     *
     * @param lines  les lignes rendues, au plus {@link #LINES_LIMIT}
     * @param tronque vrai quand le fichier dépasse le plafond
     */
    public record Dump(List<String> lines, boolean tronque) {

        /** Constructeur canonique : liste figée. */
        public Dump {
            lines = List.copyOf(lines);
        }
    }

    /**
     * Le vidage hexadécimal des octets, borné au plafond : chaque
     * ligne rend son décalage, ses seize octets et le gouttière des
     * imprimables — la ligne finale, courte, se borne d'espaces pour
     * l'alignement du gouttière.
     *
     * @param octets les octets bruts du fichier
     * @return l'aperçu : ses lignes et son drapeau de coupe
     */
    public static Dump of(byte[] octets) {
        Objects.requireNonNull(octets, "octets");
        List<String> lines = new ArrayList<>();
        int total = Math.min(octets.length, LINES_LIMIT * BYTES_PER_LINE);
        for (int start = 0; start < total; start += BYTES_PER_LINE) {
            lines.add(line(octets, start, total));
        }
        return new Dump(lines, octets.length > total);
    }

    /** La ligne du vidage : décalage, seize octets, gouttière. */
    private static String line(byte[] octets, int start, int total) {
        StringBuilder hexa = new StringBuilder(BYTES_PER_LINE * 3);
        StringBuilder gutter = new StringBuilder(BYTES_PER_LINE);
        for (int i = 0; i < BYTES_PER_LINE; i++) {
            int index = start + i;
            if (index < total) {
                int value = octets[index] & 0xFF;
                hexa.append(String.format("%02X ", value));
                gutter.append(value >= 0x20 && value <= 0x7E
                        ? (char) value : '.');
            } else {
                hexa.append("   ");
            }
        }
        return String.format("%08X  %s |%s|", start, hexa, gutter);
    }
}
