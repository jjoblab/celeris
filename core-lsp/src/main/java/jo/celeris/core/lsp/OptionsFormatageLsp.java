package jo.celeris.core.lsp;

/**
 * Les options de formatage (H-12) : la taille de tabulation et le
 * choix espaces contre tabulations — les seules que le protocole
 * exige, la carte des serveurs reste la leur.
 *
 * @param largeurTab la taille de tabulation
 * @param espaces    vrai pour des espaces, faux pour des tabulations
 */
public record OptionsFormatageLsp(int largeurTab, boolean espaces) {

    /** Le défaut raisonnable : tabulation de quatre espaces. */
    public static final OptionsFormatageLsp DEFAUT =
            new OptionsFormatageLsp(4, true);
}
