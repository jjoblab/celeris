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
final class BlocOuvert {

    private final RegleBloc regle;
    private final Regex fin;
    private final @Nullable String sourceFin;

    private BlocOuvert(RegleBloc regle, Regex fin, @Nullable String sourceFin) {
        this.regle = regle;
        this.fin = fin;
        this.sourceFin = sourceFin;
    }

    /**
     * Ouvre un bloc sans référence de fermeture : la fermeture compilée
     * de la règle sert telle quelle, partagée entre toutes les ouvertures.
     *
     * @param regle la règle du bloc
     * @return le bloc ouvert
     */
    static BlocOuvert ouvrir(RegleBloc regle) {
        return new BlocOuvert(regle, regle.fin(), null);
    }

    /**
     * Ouvre un bloc dont la fermeture référence les captures de
     * l'ouverture : chaque {@code \N} se substitue par le texte capturé,
     * échappé — un groupe absent se remplace par un motif qui ne matche
     * jamais, comme la référence non satisfaite d'Oniguruma.
     *
     * @param regle  la règle du bloc
     * @param debut  la correspondance d'ouverture, groupes évalués
     * @param ligne  la ligne transcodée, pour lire les captures en UTF-16
     * @return le bloc ouvert, fermeture substituée
     */
    static BlocOuvert ouvrir(RegleBloc regle, Matcher debut, Utf8Line ligne) {
        String source = substituer(regle.sourceFin(), debut, ligne);
        try {
            return new BlocOuvert(regle, Expressions.compiler(source), source);
        } catch (IllegalArgumentException e) {
            return new BlocOuvert(regle, Expressions.jamais(), source);
        }
    }

    /** La règle qui a ouvert le bloc. */
    RegleBloc regle() {
        return regle;
    }

    /** La fermeture effective du bloc ouvert. */
    Regex fin() {
        return fin;
    }

    /** La source substituée, null si la fermeture est celle de la règle. */
    @Nullable String sourceFin() {
        return sourceFin;
    }

    @Override
    public boolean equals(@Nullable Object autre) {
        if (!(autre instanceof BlocOuvert ouvert)) {
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
    private static String capture(int numero, Region groupes, Utf8Line ligne) {
        if (numero >= groupes.getNumRegs()) {
            return "\\" + numero;
        }
        if (groupes.getBeg(numero) < 0) {
            return "(?=x)y";
        }
        int debutChar = ligne.versChar(groupes.getBeg(numero));
        int finChar = ligne.versChar(groupes.getEnd(numero));
        return Expressions.echapperLitteral(
                ligne.texte().subSequence(debutChar, finChar).toString());
    }

    /** Substitue les références de fermeture par les captures de l'ouverture. */
    private static String substituer(String source, Matcher debut, Utf8Line ligne) {
        Region groupes = debut.getEagerRegion();
        StringBuilder sortie = new StringBuilder(source.length() + 16);
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\' && i + 1 < source.length() && reference(source, i + 1)) {
                sortie.append(capture(source.charAt(i + 1) - '0', groupes, ligne));
                i += 2;
            } else {
                sortie.append(c);
                i++;
            }
        }
        return sortie.toString();
    }
}
