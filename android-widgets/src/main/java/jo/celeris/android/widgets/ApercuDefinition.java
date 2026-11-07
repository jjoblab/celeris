package jo.celeris.android.widgets;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.OutlineEntry;

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
public record ApercuDefinition(String nom, String categorie, int ligne,
        List<String> extrait) {

    /** Le plafond de lignes de l'extrait : assez pour lire, trop peu pour noyer. */
    private static final int LIMITE_EXTRAIT = 12;

    /**
     * Le symbole au curseur : les caractères d'identifiant qui
     * l'entourent — la chaîne vide entre deux mots.
     *
     * @param texte  le texte intégral du document
     * @param curseur le décalage du curseur
     * @return le mot du curseur, jamais null
     */
    public static String symboleAu(String texte, int curseur) {
        int borne = Math.min(curseur, texte.length());
        int debut = borne;
        while (debut > 0 && Character.isJavaIdentifierPart(texte.charAt(debut - 1))) {
            debut--;
        }
        int fin = borne;
        while (fin < texte.length() && Character.isJavaIdentifierPart(
                texte.charAt(fin))) {
            fin++;
        }
        return texte.substring(debut, fin);
    }

    /**
     * L'aperçu du symbole au curseur, depuis le plan du fichier.
     *
     * @param plan    les entrées du plan, dans l'ordre du fichier
     * @param lignes  les lignes du fichier
     * @param symbole le symbole cherché, la chaîne vide pour l'entrée
     *                qui contient le curseur
     * @param offset  le décalage du curseur dans le fichier
     * @return l'aperçu, null si aucune déclaration ne répond
     */
    public static @Nullable ApercuDefinition de(List<OutlineEntry> plan,
            List<String> lignes, String symbole, int offset) {
        OutlineEntry cible = null;
        if (!symbole.isEmpty()) {
            for (OutlineEntry entree : plan) {
                if (entree.name().equals(symbole)) {
                    cible = entree;
                    break;
                }
            }
        }
        if (cible == null) {
            // L'entrée la plus intime qui contient le curseur : la
            // déclaration précise se survole, pas son enveloppe.
            for (OutlineEntry entree : plan) {
                if (offset >= entree.startOffset() && offset < entree.endOffset()
                        && (cible == null || entree.endOffset()
                                - entree.startOffset() < cible.endOffset()
                                - cible.startOffset())) {
                    cible = entree;
                }
            }
        }
        if (cible == null) {
            return null;
        }
        int debut = Math.min(cible.startLine(), lignes.size());
        int fin = Math.min(cible.endLine() + 1, lignes.size());
        List<String> extrait = lignes.subList(debut, Math.min(fin, debut
                + LIMITE_EXTRAIT));
        return new ApercuDefinition(cible.name(),
                cible.category().name(), debut, List.copyOf(extrait));
    }
}
