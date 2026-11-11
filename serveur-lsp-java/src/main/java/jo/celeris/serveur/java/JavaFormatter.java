package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

/**
 * Le formatage du mini serveur : la réindentation par profondeur
 * d'accolades — chaque niveau s'indente d'une unité (espaces ou
 * tabulations, au choix de la demande), la fermante remonte d'un cran,
 * le contenu des lignes se conserve par ailleurs tel quel.
 *
 * <p>Le formatage est idempotent : formater le texte formaté rend le
 * même texte. Les chaînes et commentaires ne comptent pas dans la
 * profondeur — le code dépouillé décide seul.</p>
 */
public final class JavaFormatter {

    private JavaFormatter() {
    }

    /**
     * Réindentation du document entier.
     *
     * @param source       le document suivi
     * @param tabSize      la largeur d'une unité d'indentation
     * @param insertSpaces vrai pour des espaces, faux pour des
     *                     tabulations
     * @return le texte réindenté
     */
    public static String format(JavaSource source, int tabSize,
            boolean insertSpaces) {
        String unite = insertSpaces ? " ".repeat(Math.max(1, tabSize)) : "\t";
        String texte = source.stripped().text();
        List<String> rendues = new ArrayList<>();
        int profondeur = 0;
        for (int ligne = 0; ligne < source.lineCount(); ligne++) {
            String contenue = source.line(ligne).strip();
            if (contenue.isEmpty()) {
                rendues.add("");
                continue;
            }
            if (contenue.startsWith("}")) {
                profondeur = Math.max(0, profondeur - 1);
            }
            rendues.add(unite.repeat(profondeur) + contenue);
            profondeur += netBraces(texte, source, ligne);
        }
        // la dernière ligne vide d'un texte fini par un saut ne se
        // rend pas : le saut final la tient déjà
        boolean finitParSaut = source.text().endsWith("\n");
        if (finitParSaut && !rendues.isEmpty()
                && rendues.get(rendues.size() - 1).isEmpty()) {
            rendues.remove(rendues.size() - 1);
        }
        return String.join("\n", rendues) + (finitParSaut ? "\n" : "");
    }

    /** Le solde d'accolades d'une ligne (ouvrantes moins fermantes). */
    private static int netBraces(String texte, JavaSource source, int ligne) {
        int debut = source.lineStart(ligne);
        int fin = debut + source.line(ligne).length();
        if (debut >= texte.length()) {
            return 0;
        }
        int solde = 0;
        for (int i = debut; i < Math.min(fin, texte.length()); i++) {
            if (texte.charAt(i) == '{') {
                solde++;
            } else if (texte.charAt(i) == '}') {
                solde--;
            }
        }
        return solde;
    }
}
