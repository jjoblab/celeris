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
    public static final int PLAFOND_LIGNES = 256;

    /** Les octets par ligne : seize, le classique du vidage. */
    public static final int OCTETS_PAR_LIGNE = 16;

    private HexadecimalPreview() {
    }

    /**
     * L'aperçu borné : les lignes du vidage et son drapeau de coupe.
     *
     * @param lignes  les lignes rendues, au plus {@link #PLAFOND_LIGNES}
     * @param tronque vrai quand le fichier dépasse le plafond
     */
    public record Dump(List<String> lignes, boolean tronque) {

        /** Constructeur canonique : liste figée. */
        public Dump {
            lignes = List.copyOf(lignes);
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
    public static Dump de(byte[] octets) {
        Objects.requireNonNull(octets, "octets");
        List<String> lignes = new ArrayList<>();
        int total = Math.min(octets.length, PLAFOND_LIGNES * OCTETS_PAR_LIGNE);
        for (int debut = 0; debut < total; debut += OCTETS_PAR_LIGNE) {
            lignes.add(ligne(octets, debut, total));
        }
        return new Dump(lignes, octets.length > total);
    }

    /** La ligne du vidage : décalage, seize octets, gouttière. */
    private static String ligne(byte[] octets, int debut, int total) {
        StringBuilder hexa = new StringBuilder(OCTETS_PAR_LIGNE * 3);
        StringBuilder gouttiere = new StringBuilder(OCTETS_PAR_LIGNE);
        for (int i = 0; i < OCTETS_PAR_LIGNE; i++) {
            int index = debut + i;
            if (index < total) {
                int valeur = octets[index] & 0xFF;
                hexa.append(String.format("%02X ", valeur));
                gouttiere.append(valeur >= 0x20 && valeur <= 0x7E
                        ? (char) valeur : '.');
            } else {
                hexa.append("   ");
            }
        }
        return String.format("%08X  %s |%s|", debut, hexa, gouttiere);
    }
}
