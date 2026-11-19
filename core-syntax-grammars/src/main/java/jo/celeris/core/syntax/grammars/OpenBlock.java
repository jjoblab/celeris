package jo.celeris.core.syntax.grammars;

import java.util.Objects;

import org.joni.Matcher;
import org.joni.Regex;
import org.joni.Region;
import org.jspecify.annotations.Nullable;

/**
 * Un bloc ouvert, entrée de la pile transportée par ligne : la règle
 * qui a ouvert, et sa fermeture EFFECTIVE — substituée par les captures
 * de l'ouverture quand la source référence {@code \1} et suivants. La
 * valeur d'état compte : deux ouvertures de la même règle aux mêmes
 * captures se comparent égales, la cascade de re-lexicographie
 * s'arrête exactement à la reconvergence (PIEGE-21) — l'expression
 * compilée ne participe pas à l'égalité, seule sa source y entre.
 */
final class OpenBlock {

    private final BlockRule regle;
    private final Regex end;
    private final @Nullable String sourceFin;

    private OpenBlock(BlockRule regle, Regex end, @Nullable String sourceFin) {
        this.regle = regle;
        this.end = end;
        this.sourceFin = sourceFin;
    }

    /**
     * Ouvre un bloc sans référence de fermeture : la fermeture compilée
     * de la règle sert telle quelle, partagée entre toutes les ouvertures.
     *
     * @param regle la règle du bloc
     * @return le bloc ouvert
     */
    static OpenBlock open(BlockRule regle) {
        return new OpenBlock(regle, regle.end(), null);
    }

    /**
     * Ouvre un bloc dont la fermeture référence les captures de
     * l'ouverture : chaque {@code \N} se substitue par le texte capturé,
     * échappé — un groupe absent se remplace par un motif qui ne matche
     * jamais, comme la référence non satisfaite d'Oniguruma.
     *
     * @param regle  la règle du bloc
     * @param start  la correspondance d'ouverture, groupes évalués
     * @param line  la ligne transcodée, pour lire les captures en UTF-16
     * @return le bloc ouvert, fermeture substituée
     */
    static OpenBlock open(BlockRule regle, Matcher start, Utf8Line line) {
        String source = substituer(regle.sourceFin(), start, line);
        try {
            return new OpenBlock(regle, Expressions.compiler(source), source);
        } catch (IllegalArgumentException e) {
            return new OpenBlock(regle, Expressions.jamais(), source);
        }
    }

    /** La règle qui a ouvert le bloc. */
    BlockRule regle() {
        return regle;
    }

    /** La fermeture effective du bloc ouvert. */
    Regex end() {
        return end;
    }

    /** La source substituée, null si la fermeture est celle de la règle. */
    @Nullable String sourceFin() {
        return sourceFin;
    }

    @Override
    public boolean equals(@Nullable Object autre) {
        if (!(autre instanceof OpenBlock ouvert)) {
            return false;
        }
        return regle.equals(ouvert.regle) && Objects.equals(sourceFin, ouvert.sourceFin);
    }

    @Override
    public int hashCode() {
        return regle.hashCode() * 31 + Objects.hashCode(sourceFin);
    }

    /** Vrai si le caractère à la position ouvre une référence de capture (1 à 9). */
    private static boolean reference(String source, int position) {
        char c = source.charAt(position);
        return c >= '1' && c <= '9';
    }

    /** Le texte substitué d'une référence : la capture échappée, l'impossible sinon. */
    private static String capture(int number, Region groupes, Utf8Line line) {
        if (number >= groupes.getNumRegs()) {
            return "\\" + number;
        }
        if (groupes.getBeg(number) < 0) {
            return "(?=x)y";
        }
        int debutChar = line.versChar(groupes.getBeg(number));
        int finChar = line.versChar(groupes.getEnd(number));
        return Expressions.echapperLitteral(
                line.text().subSequence(debutChar, finChar).toString());
    }

    /** Substitue les références de fermeture par les captures de l'ouverture. */
    private static String substituer(String source, Matcher start, Utf8Line line) {
        Region groupes = start.getEagerRegion();
        StringBuilder output = new StringBuilder(source.length() + 16);
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\' && i + 1 < source.length() && reference(source, i + 1)) {
                output.append(capture(source.charAt(i + 1) - '0', groupes, line));
                i += 2;
            } else {
                output.append(c);
                i++;
            }
        }
        return output.toString();
    }
}
