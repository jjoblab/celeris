package jo.celeris.core.syntax.grammars;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * La moisson des mots réservés d'une grammaire TextMate : les règles
 * de portée {@code keyword.control} (et {@code keyword} nu) portent
 * des alternatives de littéraux — {@code \b(if|else|for)\b} — que la
 * complétion de base sert dès le premier caractère, sans serveur de
 * langage ni table de l'hôte. Les motifs qui ne sont PAS des
 * alternatives de littéraux purs (opérateurs, classes de caractères,
 * quantificateurs) se sautent : mieux vaut peu de mots justes que
 * des fragments d'expression régulière proposés au développeur.
 *
 * <p>Pure et sans état : le lecteur de grammaires y verse au
 * chargement, {@link GrammaireSyntax#reservedWords()} sert la
 * récolte.</p>
 */
final class MotsClesGrammaire {

    /** Les portées qui portent les mots réservés — les opérateurs
     *  ({@code keyword.operator}) et les autres espèces ne s'y
     *  moissonnent pas : leurs motifs ne sont pas des mots. */
    private static final List<String> PORTEES_MOISSON =
            List.of("keyword.control", "keyword");

    /** Le plafond de mots par grammaire — une grammaire déréglée ne
     *  gonfle pas la complétion de milliers de fragments. */
    private static final int PLAFOND_MOTS = 400;

    private MotsClesGrammaire() {
    }

    /**
     * La portée porte-t-elle des mots réservés ?
     *
     * @param portee la portée de la règle, null possible
     * @return vrai quand la règle se moissonne
     */
    static boolean porteDesMots(@Nullable String portee) {
        if (portee == null) {
            return false;
        }
        for (String candidate : PORTEES_MOISSON) {
            if (portee.equals(candidate) || portee.startsWith(candidate + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moissonne les littéraux d'un motif : les alternatives au niveau
     * racine rendent leurs mots purs ; le motif dont AUCUNE
     * alternative n'est pure mais qui PORTE un groupe d'alternatives
     * (les grammaires du commerce entourent leurs mots d'assertions
     * et de classes) rend les mots du PREMIER groupe capturant qui
     * moissonne. Le motif sans mot pur ni groupe moissonnable se
     * saute ENTIER : un fragment d'expression ne se propose jamais.
     *
     * @param motif   le texte du motif de la règle
     * @param portee  la portée de la règle
     * @param verses  la récolte en cours, y versée
     */
    static void moissonner(String motif, @Nullable String portee,
            Set<String> verses) {
        if (!porteDesMots(portee) || verses.size() >= PLAFOND_MOTS) {
            return;
        }
        if (verser(alternatives(motif), verses)) {
            return;
        }
        // Les groupes capturants, du premier au dernier : le premier
        // qui rend un mot pur gagne (catch|finally|throw au milieu
        // des assertions).
        for (String groupe : groupes(motif)) {
            if (verser(alternatives(groupe), verses)) {
                return;
            }
        }
    }

    /** Verse les mots purs des alternatives — vrai si AU MOINS UN
     *  mot y passe (le versement ne se fait alors que de mots purs). */
    private static boolean verser(List<String> candidates, Set<String> verses) {
        boolean auMoinsUn = false;
        for (String alternative : candidates) {
            String mot = motPur(alternative);
            if (mot != null) {
                if (verses.size() < PLAFOND_MOTS) {
                    verses.add(mot);
                }
                auMoinsUn = true;
            }
        }
        return auMoinsUn;
    }

    /**
     * Les groupes CAPTURANTS du motif, dans l'ordre : leurs contenus
     * entre parenthèses ouvrantes non échappées et leur fermante
     * équilibrée — les groupes non capturants ({@code (?:…)}, les
     * assertions) ne s'y ouvrent pas.
     */
    private static List<String> groupes(String motif) {
        List<String> groupes = new java.util.ArrayList<>(4);
        int profondeur = 0;
        int ouverture = -1;
        for (int i = 0; i < motif.length(); i++) {
            char c = motif.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '(') {
                if (profondeur == 0 && !estNonCapturant(motif, i)) {
                    ouverture = i;
                }
                profondeur++;
            } else if (c == ')') {
                profondeur--;
                if (profondeur == 0 && ouverture >= 0) {
                    groupes.add(motif.substring(ouverture + 1, i));
                    ouverture = -1;
                }
            }
        }
        return groupes;
    }

    /** Le groupe ouvert à l'index est-il non capturant ({@code (?:},
     *  {@code (?=}, {@code (?<}) ? */
    private static boolean estNonCapturant(String motif, int index) {
        return index + 1 < motif.length() && motif.charAt(index + 1) == '?';
    }

    /**
     * Les alternatives du motif : les gardes de mot ({@code \b},
     * {@code \B}) et les assertions de regarder autour (lookbehind,
     * lookahead) se déshabillent, le groupe capturant ou non qui
     * enveloppe tout s'ouvre, la barre verticale sépare — un motif
     * SANS barre verticale rend sa forme unique. Les grammaires du
     * commerce ({@code (?<![_$[:alnum:]])(?:(?<!\.))(mot|mot)(?!…)})
     * y rendent leurs alternatives.
     */
    private static List<String> alternatives(String motif) {
        String corps = motif.strip();
        corps = deGarder(corps);
        // le groupe capturant ou non qui enveloppe tout : (mot|mot)
        boolean enveloppe = corps.startsWith("(") && corps.endsWith(")");
        while (enveloppe) {
            String interieur = corps.startsWith("(?:")
                    ? corps.substring(3, corps.length() - 1)
                    : corps.substring(1, corps.length() - 1);
            if (!equilibre(interieur)) {
                break;
            }
            corps = deGarder(interieur);
            enveloppe = corps.startsWith("(") && corps.endsWith(")");
        }
        return List.of(corps.split("\\|", -1));
    }

    /** Déshabille les gardes et assertions du bord : {@code \b},
     *  {@code \B}, les lookbehinds et lookaheads — une assertion
     *  interne (pas au bord) casse l'équilibre et reste. */
    private static String deGarder(String texte) {
        boolean change = true;
        while (change) {
            change = false;
            if (texte.startsWith("\\b") || texte.startsWith("\\B")) {
                texte = texte.substring(2).strip();
                change = true;
            }
            if (texte.endsWith("\\b") || texte.endsWith("\\B")) {
                texte = texte.substring(0, texte.length() - 2).strip();
                change = true;
            }
            if (texte.startsWith("(?<!") || texte.startsWith("(?<")
                    || texte.startsWith("(?=") || texte.startsWith("(?!")) {
                int fin = assertionFin(texte);
                if (fin > 0) {
                    texte = texte.substring(fin).strip();
                    change = true;
                }
            }
            if (texte.endsWith(")") && commenceAssertionFinale(texte)) {
                int debut = assertionDebut(texte);
                if (debut >= 0) {
                    texte = texte.substring(0, debut).strip();
                    change = true;
                }
            }
        }
        return texte;
    }

    /** La fin d'une assertion d'ouverture : l'index APRÈS la
     *  parenthèse fermante équilibrée, -1 si introuvable. */
    private static int assertionFin(String texte) {
        int profondeur = 0;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '(') {
                profondeur++;
            } else if (c == ')') {
                profondeur--;
                if (profondeur == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    /** L'assertion finale existe-t-elle : le texte se termine par une
     *  parenthèse qui ouvre une assertion (?=, (?!… — son ouverture. */
    private static boolean commenceAssertionFinale(String texte) {
        int debut = assertionDebut(texte);
        return debut >= 0 && (texte.startsWith("(?=", debut)
                || texte.startsWith("(?!", debut));
    }

    /** L'ouverture de la dernière parenthèse équilibrée du bord : -1
     *  si le bord droit n'est pas un groupe autonome. */
    private static int assertionDebut(String texte) {
        int profondeur = 0;
        for (int i = texte.length() - 1; i >= 0; i--) {
            char c = texte.charAt(i);
            if (c == '\\') {
                continue;
            }
            if (c == ')') {
                profondeur++;
            } else if (c == '(') {
                profondeur--;
                if (profondeur == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Les parenthèses de l'intérieur s'équilibrent-elles (le groupe
     *  externe fermait une parenthèse intérieure) ? */
    private static boolean equilibre(String texte) {
        int profondeur = 0;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '(') {
                profondeur++;
            } else if (c == ')') {
                profondeur--;
                if (profondeur < 0) {
                    return false;
                }
            }
        }
        return profondeur == 0;
    }

    /**
     * Le mot pur d'une alternative : lettres, chiffres et tiret bas
     * exclusivement, non vide — tout autre caractère (classe,
     * quantificateur, référence) rend l'alternative IMPURE.
     *
     * @param alternative une alternative du motif
     * @return le mot, null si l'alternative n'est pas un mot pur
     */
    private static @Nullable String motPur(String alternative) {
        String mot = alternative.strip();
        if (mot.isEmpty()) {
            return null;
        }
        for (int i = 0; i < mot.length(); i++) {
            char c = mot.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return null;
            }
        }
        return mot;
    }

    /**
     * La récolte figée, triée sans casse — la liste de service de la
     * complétion.
     *
     * @param verses les mots moissonnés
     * @return les mots distincts triés, jamais null
     */
    static List<String> figer(Set<String> verses) {
        List<String> mots = new java.util.ArrayList<>(new LinkedHashSet<>(verses));
        mots.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(mots);
    }
}
