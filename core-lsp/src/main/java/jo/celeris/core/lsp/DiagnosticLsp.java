package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * Un diagnostic publié par le serveur : plage, gravité (1 erreur à 4
 * suggestion), message, code et source optionnels.
 *
 * @param plage    la plage du diagnostic, en positions LSP
 * @param gravite  la gravité (1 erreur, 2 avertissement, 3 information,
 *                 4 suggestion), zéro si absente
 * @param message  le message humain
 * @param code     le code du diagnostic, ou null
 * @param source   la source (le nom de l'analyseur), ou null
 */
public record DiagnosticLsp(PlageLsp plage, int gravite, String message,
                            @Nullable String code, @Nullable String source) {

    /** La gravité « erreur ». */
    public static final int ERREUR = 1;

    /** La gravité « avertissement ». */
    public static final int AVERTISSEMENT = 2;

    /**
     * Analyse le diagnostic depuis sa forme JSON.
     *
     * @param json l'objet diagnostic
     * @return le diagnostic lu — plage vide et message vide si la forme
     *         est incomplète, la gravité absente vaut zéro
     */
    public static DiagnosticLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new DiagnosticLsp(new PlageLsp(new PositionLsp(0, 0),
                    new PositionLsp(0, 0)), 0, "", null, null);
        }
        PlageLsp plage = PlageLsp.de(objet.membreOu("range", JsonObjet.vide()));
        Long gravite = objet.membreEntier("severity");
        String message = objet.membreTexte("message");
        return new DiagnosticLsp(plage,
                gravite == null ? 0 : gravite.intValue(),
                message == null ? "" : message,
                objet.membreTexte("code"),
                objet.membreTexte("source"));
    }
}
