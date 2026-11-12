package jo.celeris.android.host.spi;

import java.util.Objects;

/**
 * La détection des fichiers binaires (A-26) : l'octet nul et les
 * caractères de contrôle hors texte décident — dans le préfixe lu
 * (8 Kio), jamais après lui. Jamais une extension de nom (elle
 * ment), jamais un décodage (il détruirait ce qu'il lit : l'octet
 * nul devenu remplacement, la perte se confondrait) — la décision se
 * prend sur les octets BRUTS, avant toute interprétation.
 *
 * <p>Les blancs du texte se permettent : tabulation, saut de ligne,
 * saut de page, retour de chariot ; tout autre contrôle du bloc
 * refuse — un fichier texte n'en porte pas. Les octets hauts ne
 * décident jamais : le texte encodé en Unicode en porte. Le préfixe
 * borne la lecture : le signal au-delà de 8 Kio ne se voit pas —
 * limite documentée, le binaire digne de ce nom parle tôt.</p>
 */
public final class DetectionBinaire {

    /** Le préfixe lu : 8 Kio, la mesure bornée. */
    public static final int PREFIXE = 8192;

    private DetectionBinaire() {
    }

    /**
     * Vrai quand les octets portent un fichier binaire : l'octet nul
     * ou un contrôle hors texte dans le préfixe décide.
     *
     * @param octets les octets bruts du fichier, jamais décodés
     * @return vrai si le fichier se refuse au texte
     */
    public static boolean binaire(byte[] octets) {
        Objects.requireNonNull(octets, "octets");
        int borne = Math.min(octets.length, PREFIXE);
        for (int i = 0; i < borne; i++) {
            if (estControle(octets[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai quand l'octet est un contrôle que le texte ne porte pas :
     * l'octet nul compris, les blancs du texte exceptés (tabulation,
     * saut de ligne, saut de page, retour de chariot).
     */
    private static boolean estControle(byte octet) {
        int valeur = octet & 0xFF;
        return valeur < 0x09 || valeur == 0x0B
                || (valeur >= 0x0E && valeur <= 0x1F);
    }
}
