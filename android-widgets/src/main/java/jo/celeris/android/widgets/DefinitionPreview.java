package jo.celeris.android.widgets;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.structure.OutlineEntry;

/**
 * L'aperçu rapide d'une définition (D-11) : la déclaration du symbole
 * visé se lit dans une fenêtre flottante, sans quitter le fichier —
 * le plan du fichier (B-16) sert d'index, les lignes sources de
 * l'entrée portent l'extrait, borné pour rester lisible.
 *
 * <p>Le symbole se lit au curseur : la première déclaration du plan
 * qui porte ce nom exact répond ; à défaut, l'entrée qui contient le
 * curseur — la déclaration la plus proche se survole.</p>
 */
public record DefinitionPreview(String nom, String categorie, int line,
        List<String> extrait) {

    /** Le plafond de lignes de l'extrait : assez pour lire, trop peu pour noyer. */
    private static final int LIMITE_EXTRAIT = 12;

    /**
     * Le symbole au curseur : les caractères d'identifiant qui
     * l'entourent — la chaîne vide entre deux mots.
     *
     * @param text  le texte intégral du document
     * @param cursor le décalage du curseur
     * @return le mot du curseur, jamais null
     */
    public static String symboleAu(String text, int cursor) {
        int borne = Math.min(cursor, text.length());
        int start = borne;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        int end = borne;
        while (end < text.length() && Character.isJavaIdentifierPart(
                text.charAt(end))) {
            end++;
        }
        return text.substring(start, end);
    }

    /**
     * L'aperçu du symbole au curseur, depuis le plan du fichier.
     *
     * @param plan    les entrées du plan, dans l'ordre du fichier
     * @param lines  les lignes du fichier
     * @param symbole le symbole cherché, la chaîne vide pour l'entrée
     *                qui contient le curseur
     * @param offset  le décalage du curseur dans le fichier
     * @return l'aperçu, null si aucune déclaration ne répond
     */
    public static @Nullable DefinitionPreview of(List<OutlineEntry> plan,
            List<String> lines, String symbole, int offset) {
        OutlineEntry target = null;
        if (!symbole.isEmpty()) {
            for (OutlineEntry input : plan) {
                if (input.name().equals(symbole)) {
                    target = input;
                    break;
                }
            }
        }
        if (target == null) {
            // L'entrée la plus intime qui contient le curseur : la
            // déclaration précise se survole, pas son enveloppe.
            for (OutlineEntry input : plan) {
                if (offset >= input.startOffset() && offset < input.endOffset()
                        && (target == null || input.endOffset()
                                - input.startOffset() < target.endOffset()
                                - target.startOffset())) {
                    target = input;
                }
            }
        }
        if (target == null) {
            return null;
        }
        int start = Math.min(target.startLine(), lines.size());
        int end = Math.min(target.endLine() + 1, lines.size());
        List<String> extrait = lines.subList(start, Math.min(end, start
                + LIMITE_EXTRAIT));
        return new DefinitionPreview(target.name(),
                target.category().name(), start, List.copyOf(extrait));
    }
}
