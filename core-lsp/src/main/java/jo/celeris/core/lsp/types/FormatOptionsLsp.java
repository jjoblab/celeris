package jo.celeris.core.lsp.types;

/**
 * Les options de formatage (H-12) : la taille de tabulation et le
 * choix espaces contre tabulations — les seules que le protocole
 * exige, la carte des serveurs reste la leur.
 *
 * @param tabWidth la taille de tabulation
 * @param espaces    vrai pour des espaces, faux pour des tabulations
 */
public record FormatOptionsLsp(int tabWidth, boolean espaces) {

    /** Le défaut raisonnable : tabulation de quatre espaces. */
    public static final FormatOptionsLsp DEFAUT =
            new FormatOptionsLsp(4, true);
}
