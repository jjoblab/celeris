package jo.celeris.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.SyntaxPalette;
import jo.celeris.core.language.TokenColorsTheme;
import jo.celeris.core.syntax.ScopeStacks;
import jo.celeris.core.syntax.ScopeStyle;
import jo.celeris.core.syntax.ScopeTheme;


import org.jspecify.annotations.Nullable;

/**
 * Les thèmes importés de la démonstration (M7.6, H-07) : le fichier
 * {@code tokenColors} au format de VS Code vit aux ressources du
 * module — le FORMAT est celui des thèmes du commerce, le contenu est
 * écrit ici (licence propre, pas de reprise). L'importeur du
 * {@code core-language} le lit : les règles deviennent le thème par
 * portées, les couleurs d'éditeur fondent le chrome, la palette par
 * sorte se DÉRIVE des résolutions du thème — une seule table de
 * couleurs, jamais deux.
 */
final class ImportedThemes {

    /** Le fichier du thème importé, aux ressources du module. */
    private static final String FICHIER = "/themes/atelier.json";

    /** L'identifiant du thème importé — unique parmi les thèmes de la vue. */
    private static final int IDENTIFIANT = 4;

    private static volatile @Nullable EditorTheme theme;

    private ImportedThemes() {
    }

    /**
     * Le thème importé, lu une fois : le chrome (fond, encre de base,
     * sélection, curseur, gouttière) se fonde sur les couleurs
     * d'éditeur du fichier ; la palette par sorte se dérive des
     * résolutions du thème par portées.
     *
     * @return le thème complet, prêt pour la vue
     */
    static EditorTheme atelier() {
        EditorTheme present = theme;
        if (present != null) {
            return present;
        }
        synchronized (ImportedThemes.class) {
            present = theme;
            if (present == null) {
                present = batis(lire());
                theme = present;
            }
            return present;
        }
    }

    /** Le fichier du thème importé, lu aux ressources du module. */
    static TokenColorsTheme lire() {
        try (InputStream flux = ImportedThemes.class
                .getResourceAsStream(FICHIER)) {
            if (flux == null) {
                throw new IllegalStateException(
                        "imported theme not found: " + FICHIER);
            }
            StringBuilder texte = new StringBuilder();
            byte[] tampon = new byte[8192];
            int lus;
            while ((lus = flux.read(tampon)) > 0) {
                texte.append(new String(tampon, 0, lus,
                        StandardCharsets.UTF_8));
            }
            return TokenColorsTheme.parse(texte.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "imported theme unreadable: " + FICHIER, e);
        }
    }

    /** Le thème complet de son import : chrome du fichier, palette dérivée. */
    private static EditorTheme batis(TokenColorsTheme importe) {
        int fond = importe.background() != 0 ? importe.background()
                : 0xFFFBF6EE;
        int base = importe.foreground() != 0 ? importe.foreground()
                : 0xFF3A3630;
        return new EditorTheme(IDENTIFIANT, fond, base, 0xFFEBDFC8,
                0xFFB45309, 0xFF6E675D, 0x143A3630,
                paletteDerivee(importe.scopes(), base), importe.scopes());
    }

    /**
     * La palette par sorte DÉRIVÉE du thème par portées : chaque sorte
     * sonde sa portée de référence, la résolution rend l'encre — la
     * base fait foi quand le thème ne la fixe pas. Une seule table de
     * couleurs vit dans le fichier, la palette n'en est qu'une lecture.
     */
    private static SyntaxPalette paletteDerivee(ScopeTheme portees, int base) {
        return new SyntaxPalette(
                sonde(portees, base, "keyword.other.probe"),
                sonde(portees, base, "entity.name.type.probe"),
                sonde(portees, base, "string.quoted.probe"),
                sonde(portees, base, "constant.numeric.probe"),
                sonde(portees, base, "comment.line.probe"),
                sonde(portees, base, "comment.block.documentation.probe"),
                sonde(portees, base, "meta.annotation.probe"),
                sonde(portees, base, "keyword.operator.probe"),
                sonde(portees, base, "punctuation.separator.probe"),
                sonde(portees, base, "meta.probe"),
                sonde(portees, base, "support.constant.probe"),
                sonde(portees, base, "punctuation.section.probe.begin"),
                sonde(portees, base, "punctuation.section.probe.end"),
                sonde(portees, base, "invalid.illegal.probe"));
    }

    /** L'encre d'une sorte par sa sonde : la résolution, la base en repli. */
    private static int sonde(ScopeTheme portees, int base, String portee) {
        int pile = ScopeStacks.push(ScopeStacks.root("source.demo"), portee);
        ScopeStyle style = portees.styleFor(pile);
        return style != null && style.hasInk() ? style.foreground() : base;
    }
}
