package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Propriétés jqwik de la rope (ADR-003, ADR-010) : généralisation du fuzz à
 * oracle {@code StringBuilder} — chaque essai tire une graine, pilote une
 * séquence d'éditions et confronte la rope à l'oracle naïf. L'échec d'un
 * essai rapporte la graine ; la rejouer par {@code new Random(graine)}
 * reproduit la faute exactement.
 *
 * <p>Total d'environ 10 300 séquences aléatoires par exécution (objectif
 * ADR-003 : 10 000). La randomisation déterministe, rejouable à l'identique,
 * vit dans {@code RopeFuzzTest} : les deux couches se complètent.</p>
 */
class RopeProprietesTest {

    private static final int OPS_PAR_SEQUENCE = 400;
    private static final int MAX_DOC = 10_000;

    /**
     * La séquence complète d'insertions/suppressions/remplacements est
     * indistinguable de l'oracle : longueur à chaque pas, contenu intégral
     * au terme de la séquence, fenêtre glissante tous les cinquante pas.
     */
    @Property(tries = 7_000)
    void lesSequencesDEditionsEgalentLOracleStringBuilder(@ForAll long graine) {
        Random rng = new Random(graine);
        Rope rope = Rope.EMPTY;
        StringBuilder oracle = new StringBuilder();
        for (int op = 0; op < OPS_PAR_SEQUENCE; op++) {
            int choix = rng.nextInt(10);
            if (choix < 6 || oracle.length() == 0) {
                int pos = oracle.length() == 0 ? 0 : rng.nextInt(oracle.length() + 1);
                String texte = texteAleatoire(rng, rng.nextInt(20) + 1);
                oracle.insert(pos, texte);
                rope = rope.replace(pos, pos, texte);
            } else if (choix < 9) {
                int debut = rng.nextInt(oracle.length());
                int fin = debut + rng.nextInt(Math.min(oracle.length() - debut, 30) + 1);
                oracle.delete(debut, fin);
                rope = rope.replace(debut, fin, "");
            } else {
                int debut = rng.nextInt(oracle.length());
                int fin = debut + rng.nextInt(Math.min(oracle.length() - debut, 30) + 1);
                String texte = texteAleatoire(rng, rng.nextInt(20) + 1);
                oracle.replace(debut, fin, texte);
                rope = rope.replace(debut, fin, texte);
            }
            if (oracle.length() > MAX_DOC) {
                oracle.delete(MAX_DOC / 2, oracle.length());
                rope = rope.replace(MAX_DOC / 2, rope.length(), "");
            }
            assertEquals(oracle.length(), rope.length(),
                "longueur divergente a l'operation " + op + " (graine " + graine + ")");
            if (op % 50 == 0) {
                int fenetre = Math.min(80, rope.length());
                int debut = rope.length() == 0 ? 0 : rng.nextInt(rope.length() - fenetre + 1);
                assertEquals(oracle.substring(debut, debut + fenetre),
                    rope.sub(debut, debut + fenetre).toString(),
                    "fenetre divergente a l'operation " + op + " (graine " + graine + ")");
            }
        }
        assertEquals(oracle.toString(), rope.toString(), "contenu final divergent (graine " + graine + ")");
    }

    /** L'accès par caractère reste exact au fil des insertions. */
    @Property(tries = 1_000)
    void charAtEgalLOracleAuFilDesInsertions(@ForAll long graine) {
        Random rng = new Random(graine);
        Rope rope = Rope.fromString("hello world");
        StringBuilder oracle = new StringBuilder("hello world");
        for (int op = 0; op < 200; op++) {
            if (rng.nextBoolean() && oracle.length() > 0) {
                int idx = rng.nextInt(oracle.length());
                assertEquals(oracle.charAt(idx), rope.charAt(idx),
                    "charAt divergent a " + idx + " (graine " + graine + ")");
            } else {
                int pos = rng.nextInt(oracle.length() + 1);
                String texte = texteAleatoire(rng, 5);
                oracle.insert(pos, texte);
                rope = rope.replace(pos, pos, texte);
            }
        }
    }

    /** Toute sous-plage de la rope est la sous-chaîne correspondante. */
    @Property(tries = 1_500)
    void touteSousPlageEgaleLaSousChaine(@ForAll long graine) {
        Random rng = new Random(graine);
        StringBuilder oracle = new StringBuilder();
        Rope rope = Rope.EMPTY;
        for (int i = 0; i < 100; i++) {
            String texte = texteAleatoire(rng, 10);
            oracle.append(texte);
            rope = rope.replace(rope.length(), rope.length(), texte);
        }
        for (int i = 0; i < 100; i++) {
            int debut = rng.nextInt(oracle.length() + 1);
            int fin = debut + rng.nextInt(oracle.length() - debut + 1);
            assertEquals(oracle.substring(debut, fin), rope.subSequence(debut, fin).toString(),
                "sous-plage divergente [" + debut + ", " + fin + ") (graine " + graine + ")");
        }
    }

    /** L'édition ne mute jamais l'original : la rope reste immuable. */
    @Property(tries = 500)
    void leditionNeMuteJamaisLOriginal(
            @ForAll String document, @ForAll String insertion, @ForAll int positionBrute) {
        Rope original = Rope.fromString(document);
        int position = document.isEmpty()
            ? 0
            : Math.floorMod(positionBrute, document.length() + 1);
        Rope modifie = original.replace(position, position, insertion);
        assertEquals(document, original.toString(), "l'original a change");
        assertTrue(modifie.length() == document.length() + insertion.length(),
            "la longueur editee est la somme");
        assertEquals(document.substring(0, position) + insertion + document.substring(position),
            modifie.toString());
    }

    /** Les paires de substituts survivent intacts aux coupes aléatoires. */
    @Property(tries = 300)
    void lesSubstitutsSurviventAuxCoupes(@ForAll long graine) {
        Random rng = new Random(graine);
        Rope rope = Rope.fromString("Hello 🌍 World 🚀 et 中");
        StringBuilder oracle = new StringBuilder("Hello 🌍 World 🚀 et 中");
        for (int op = 0; op < 60; op++) {
            if (rng.nextBoolean()) {
                int pos = rng.nextInt(oracle.length() + 1);
                String unite = rng.nextBoolean() ? "😀" : "e";
                oracle.insert(pos, unite);
                rope = rope.replace(pos, pos, unite);
            } else if (oracle.length() > 0) {
                int debut = rng.nextInt(oracle.length());
                int fin = debut + rng.nextInt(Math.min(oracle.length() - debut, 4) + 1);
                oracle.delete(debut, fin);
                rope = rope.replace(debut, fin, "");
            }
        }
        assertEquals(oracle.toString(), rope.toString(), "substituts alters (graine " + graine + ")");
    }

    private static String texteAleatoire(Random rng, int longueurMax) {
        int longueur = rng.nextInt(longueurMax) + 1;
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            // Mélange de lettres minuscules, chiffres, sauts de ligne et
            // emojis occasionnels (paires de substituts).
            int choix = rng.nextInt(20);
            if (choix < 15) sb.append((char) ('a' + rng.nextInt(26)));
            else if (choix < 18) sb.append((char) ('0' + rng.nextInt(10)));
            else if (choix < 19) sb.append('\n');
            else sb.append("🌍");
        }
        return sb.toString();
    }
}
