package jo.celeris.android.host.spi;

import java.util.List;
import java.util.Optional;

/**
 * Le catalogue des correctifs mécaniques de lint (C-12) : les règles
 * dont la réparation est un remplacement textuel sans jugement —
 * l'ellipse typographique (« ... » devient « … ») et le tiret
 * cadratin (« -- » devient « — »). Chaque correctif se calcule depuis
 * le diagnostic et les lignes du fichier : la ligne porteuse, la
 * première occurrence du texte fautif.
 *
 * <p>Le catalogue est volontairement fermé et honnête : les règles
 * dont le correctif demanderait le contexte du projet ne s'y
 * inventent pas — le mécanisme d'application est le livrable, les
 * règles mécaniques en sont la preuve.</p>
 */
public final class CorrectifsLint {

    private CorrectifsLint() {
    }

    /**
     * Le correctif du diagnostic sur les lignes données, s'il en
     * existe un de mécanique.
     *
     * @param diagnostic le diagnostic de lint, message porteur de son
     *                   identifiant de règle
     * @param lignes     les lignes du fichier, numérotées à partir de
     *                   zéro
     * @return le correctif, vide si la règle n'est pas mécanique ou si
     *         la ligne ne porte plus le texte fautif
     */
    public static Optional<CorrectifLint> pour(DiagnosticHote diagnostic, List<String> lignes) {
        String regle = AnalyseurLint.regle(diagnostic.message());
        int index = diagnostic.ligne() - 1;
        if (index < 0 || index >= lignes.size()) {
            return Optional.empty();
        }
        String ligne = lignes.get(index);
        return switch (regle) {
            case "TypographyEllipsis" -> occurrence(diagnostic, ligne, "...",
                    "Remplacer les trois points par le caractère d'ellipse");
            case "TypographyDashes" -> occurrence(diagnostic, ligne, "--",
                    "Remplacer le double tiret par le tiret cadratin");
            default -> Optional.empty();
        };
    }

    private static Optional<CorrectifLint> occurrence(DiagnosticHote diagnostic,
            String ligne, String avant, String titre) {
        int position = ligne.indexOf(avant);
        if (position < 0) {
            return Optional.empty();
        }
        return Optional.of(new CorrectifLint(titre, diagnostic.ligne(),
                position + 1, avant, remplacant(avant)));
    }

    private static String remplacant(String avant) {
        return switch (avant) {
            case "..." -> "…";
            case "--" -> "—";
            default -> throw new IllegalArgumentException("unknown: " + avant);
        };
    }
}
