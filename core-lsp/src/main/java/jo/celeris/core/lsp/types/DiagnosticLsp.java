package jo.celeris.core.lsp.types;

import org.jspecify.annotations.Nullable;

/**
 * Un diagnostic publié par le serveur : plage, gravité (1 erreur à 4
 * suggestion), message, code et source optionnels. La lecture depuis
 * les types de la bibliothèque vit derrière la frontière protocolaire
 * (ADR-015, CorrespondanceLsp).
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
}
