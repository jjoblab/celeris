package jo.celeris.core.syntax.grammars;

import java.nio.charset.StandardCharsets;

import org.jcodings.specific.UTF8Encoding;
import org.joni.Option;
import org.joni.Regex;
import org.joni.exception.JOniException;

/**
 * La fabrique d'expressions des grammaires (ADR-014) : tout motif se
 * compile par joni, le portage Java d'Oniguruma — la sémantique que le
 * format TextMate suppose (quantificateurs possessifs, mode étendu,
 * classes POSIX). {@code java.util.regex} n'apparaît plus dans le
 * module : le spike a mesuré qu'il réécrivait 122 motifs du commerce
 * en silence, divergence de coloration invisible, pire qu'un refus.
 *
 * <p>La détection des formes catastrophiques visibles vit dans
 * {@link GardeCatastrophique} ; la substitution des fermetures à
 * références s'appuie sur {@link #echapperLitteral} et
 * {@link #modele}.</p>
 */
final class Expressions {

    /** Le motif qui ne matche jamais — repli d'une fermeture substituée invalide. */
    private static final Regex JAMAIS = compiler("(?=x)y");

    private Expressions() {
    }

    /** Le motif qui ne matche jamais, compilé une fois pour toutes. */
    static Regex jamais() {
        return JAMAIS;
    }

    /**
     * Compile un motif au sens d'Oniguruma.
     *
     * @param motif le texte du motif
     * @return l'expression compilée, prête à matcher sur des octets UTF-8
     * @throws IllegalArgumentException si le motif est invalide — l'appelant
     *                                   décide de rejeter le paquet ou d'écarter la règle
     */
    static Regex compiler(String motif) {
        try {
            byte[] octets = motif.getBytes(StandardCharsets.UTF_8);
            return new Regex(octets, 0, octets.length, Option.NONE, UTF8Encoding.INSTANCE);
        } catch (JOniException | IllegalArgumentException e) {
            throw new IllegalArgumentException("motif invalide : " + motif, e);
        }
    }

    /**
     * Le modèle de validation d'une fermeture à références : chaque
     * {@code \N} y devient {@code \w+} — la fermeture seule ne compile
     * pas (la référence vise les captures de l'OUVERTURE, un autre
     * motif), le modèle valide la syntaxe de ce qui l'entoure.
     *
     * @param source la source de la fermeture, références comprises
     * @return la source du modèle, sans référence
     */
    static String modele(String source) {
        return source.replaceAll("(?<!\\\\)\\\\([1-9])", "\\\\w+");
    }

    /**
     * Échappe un texte pour entrer tel quel dans un motif — les
     * références de la fermeture ({@code \1}) se substituent par le texte
     * capturé à l'ouverture, jamais interprété.
     *
     * @param texte le texte capturé
     * @return le texte échappé, prêt pour le motif
     */
    static String echapperLitteral(String texte) {
        StringBuilder sortie = new StringBuilder(texte.length() + 8);
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if ("\\^.[]|()*+?{}-$# \t\n\r\f\u000B".indexOf(c) >= 0) {
                sortie.append('\\');
            }
            sortie.append(c);
        }
        return sortie.toString();
    }
}
