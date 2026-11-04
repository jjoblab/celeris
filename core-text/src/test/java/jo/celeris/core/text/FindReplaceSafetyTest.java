package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * D-05 — recherche regex SÛRE : une expression à retour sur soi-même
 * catastrophique ne fige plus le thread appelant (budget temps + plafond de
 * résultats), l'état est exposé pour signal à l'UI. Portage des tests du
 * dépôt de départ (oracle) — l'horloge injectée (ADR-007) y ajoute les
 * variantes déterministes : budget expiré sur horloge scriptée, annulation
 * par interruption, texte trop court pour échantillonner le contrôle.
 */
class FindReplaceSafetyTest {

    private static final FindOptions REGEX = new FindOptions().withRegex(true);
    private static final FindOptions REGEX_CI = new FindOptions()
            .withRegex(true).withCaseSensitive(false);

    /**
     * Motif à retour sur soi-même catastrophique sur texte adverse : la
     * recherche doit rendre la main en moins d'une seconde avec l'état
     * « trop coûteuse ». Horloge réelle de mesure (outillage, ADR-007).
     */
    @Test
    @Timeout(value = 5)
    void catastrophicPattern_returnsUnderASecond_tooCostlyState() {
        // 60 000 « a » sans le « b » final : backtracking exponentiel
        // de (a+)+b.
        String adverse = "a".repeat(60_000);
        long t0 = System.nanoTime();
        FindReplace moteurReel = FindReplace.of(System::currentTimeMillis);
        FindReplace.SearchResult resultat =
                moteurReel.find(adverse, "(a+)+b", REGEX);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(resultat.tooCostly(), "l'état trop coûteuse est exposé");
        assertTrue(ms < 1_000, "retour borné < 1 s (mesuré " + ms + " ms)");
    }

    /** Variante insensible à la casse : même garde-fou. */
    @Test
    @Timeout(value = 5)
    void catastrophicPattern_caseInsensitive_guardActive() {
        String adverse = "aAaA".repeat(15_000);
        FindReplace moteurReel = FindReplace.of(System::currentTimeMillis);
        FindReplace.SearchResult resultat =
                moteurReel.find(adverse, "(a+)+b", REGEX_CI);
        assertTrue(resultat.tooCostly(), "le garde-fou couvre le mode insensible à la casse");
    }

    /** Le plafond borne la collecte et porte l'état « tronqué ». */
    @Test
    void matchCeiling_carriesTruncatedState() {
        FindReplace moteur = FindReplace.of(() -> 0L);
        // 200 000 correspondances potentielles → collecte arrêtée à 10 000.
        String texte = "a".repeat(200_000);
        FindReplace.SearchResult resultat = moteur.find(texte, "a", REGEX);
        assertEquals(FindReplace.MAX_MATCHES, resultat.matches().size(),
                "la collecte est bornée au plafond");
        assertTrue(resultat.truncated(), "l'état tronqué est exposé");
        assertFalse(resultat.tooCostly(), "le plafond n'est pas un budget dépassé");
        // Mode plain : même plafond.
        FindReplace.SearchResult resultatPlain =
                moteur.find(texte, "a", new FindOptions());
        assertEquals(FindReplace.MAX_MATCHES, resultatPlain.matches().size());
        assertTrue(resultatPlain.truncated(), "le plafond s'applique aussi au mode plain");
    }

    /** Le plafond s'applique aussi au mode mot entier. */
    @Test
    void matchCeiling_wholeWord() {
        FindReplace moteur = FindReplace.of(() -> 0L);
        String texte = "a ".repeat(FindReplace.MAX_MATCHES + 2);
        FindReplace.SearchResult resultat = moteur.find(texte, "a",
                new FindOptions().withWholeWord(true));
        assertEquals(FindReplace.MAX_MATCHES, resultat.matches().size());
        assertTrue(resultat.truncated(), "le plafond s'applique au mode mot entier");
    }

    /** Recherche saine : comportement inchangé, aucun signal. */
    @Test
    void normalSearch_unchanged() {
        FindReplace moteur = FindReplace.of(() -> 0L);
        String texte = "int foo = 1;\nint bar = foo + foo;\n";
        FindReplace.SearchResult resultat = moteur.find(texte, "foo", new FindOptions());
        assertEquals(3, resultat.matches().size());
        assertFalse(resultat.truncated());
        assertFalse(resultat.tooCostly());
        assertEquals(4, resultat.matches().get(0).start());
        assertEquals(7, resultat.matches().get(0).end());

        // Regex saine : positions exactes.
        FindReplace.SearchResult regex = moteur.find(texte, "int \\w+", REGEX);
        assertEquals(2, regex.matches().size());
        assertFalse(regex.tooCostly());
    }

    /** Motif invalide : aucune correspondance, aucun signal (historique). */
    @Test
    void invalidPattern_noMatch() {
        FindReplace moteur = FindReplace.of(() -> 0L);
        FindReplace.SearchResult resultat = moteur.find("abc", "(a", REGEX);
        assertTrue(resultat.matches().isEmpty());
        assertFalse(resultat.tooCostly());
    }

    /** Pattern centralisé : mêmes flags que l'ancienne construction. */
    @Test
    void centralizedPattern_historicalFlags() {
        FindOptions sensible = new FindOptions().withRegex(true).withCaseSensitive(true);
        assertEquals(Pattern.MULTILINE,
                FindReplace.compilePattern("a", sensible).flags());
        assertEquals(Pattern.MULTILINE | Pattern.CASE_INSENSITIVE,
                FindReplace.compilePattern("a", new FindOptions()).flags(),
                "insensible à la casse par défaut (options historiques)");
    }

    /**
     * Budget expiré sur horloge scriptée : le contrôle échantillonné
     * interrompt la correspondance au premier relevé (déterministe, sans
     * bruit du runner — leçon PIEGE-13).
     */
    @Test
    void expiredScriptedClock_tooCostly() {
        long[] appels = {0L};
        FindReplace moteur = FindReplace.of(() -> appels[0]++ < 1 ? 0L : 10_000L);
        // 2 000 caractères : le moteur regex dépasse 1 024 accès, le
        // premier contrôle d'échéance lit une horloge au-delà du budget.
        FindReplace.SearchResult resultat =
                moteur.find("a".repeat(2_000), "a+", REGEX);
        assertTrue(resultat.tooCostly(), "budget dépassé : état trop coûteuse");
    }

    /**
     * L'interruption du thread annule la recherche proprement — même état
     * « trop coûteuse », et le statut d'interruption est consommé.
     */
    @Test
    void threadInterruption_abortsAsTooCostly() {
        FindReplace moteur = FindReplace.of(() -> 0L);
        Thread.currentThread().interrupt();
        try {
            FindReplace.SearchResult resultat =
                    moteur.find("a".repeat(2_000), "a+", REGEX);
            assertTrue(resultat.tooCostly(), "recherche interrompue : état trop coûteuse");
            assertFalse(Thread.interrupted(), "le statut d'interruption est consommé");
        } finally {
            // Assainir le fil quoi qu'il arrive (le test peut échouer avant
            // la consommation ci-dessus).
            Thread.interrupted();
        }
    }

    /**
     * Texte court : moins de {@value GuardedCharSequence#CHECK_EVERY} accès
     * au texte — aucun contrôle échantillonné ne se déclenche, une horloge
     * déjà « expirée » n'affecte pas la recherche.
     */
    @Test
    void shortText_clockNeverSampled() {
        long[] appels = {0L};
        FindReplace moteur = FindReplace.of(() -> appels[0]++ < 1 ? 0L : 10_000L);
        FindReplace.SearchResult resultat = moteur.find("abc", "b", REGEX);
        assertEquals(1, resultat.matches().size());
        assertFalse(resultat.tooCostly());
    }

    /** La vue garde-fou délègue intégralement : longueur, caractère, tranche, texte. */
    @Test
    void guardedCharSequence_delegatesAllAccesses() {
        GuardedCharSequence garde =
                new GuardedCharSequence("abcdef", FindReplace.REGEX_BUDGET_MS, () -> 0L);
        assertEquals(6, garde.length());
        assertEquals('c', garde.charAt(2));
        assertEquals("cde", garde.subSequence(2, 5).toString());
        assertEquals("c", garde.subSequence(2, 3).toString());
        assertEquals("abcdef", garde.toString());
    }

    /** L'exception du garde-fou porte son message (anglais, ASCII). */
    @Test
    void budgetException_carriesMessage() {
        FindReplace.RegexBudgetExceededException exception =
                new FindReplace.RegexBudgetExceededException("regex too costly");
        assertEquals("regex too costly", exception.getMessage());
    }

    /**
     * L'échantillonnage du contrôle tire aussi dans {@code length()} : au
     * bout de {@value GuardedCharSequence#CHECK_EVERY} accès, l'échéance
     * expirée interrompt.
     */
    @Test
    void guardSampling_firesInLength() {
        long[] appels = {0L};
        GuardedCharSequence garde = new GuardedCharSequence("abcdef",
                FindReplace.REGEX_BUDGET_MS, () -> appels[0]++ < 1 ? 0L : 10_000L);
        assertThrows(FindReplace.RegexBudgetExceededException.class, () -> {
            for (int i = 0; i < GuardedCharSequence.CHECK_EVERY + 10; i++) {
                garde.length();
            }
        });
    }

    /** L'échantillonnage du contrôle tire aussi dans {@code subSequence()}. */
    @Test
    void guardSampling_firesInSubSequence() {
        long[] appels = {0L};
        GuardedCharSequence garde = new GuardedCharSequence("abcdef",
                FindReplace.REGEX_BUDGET_MS, () -> appels[0]++ < 1 ? 0L : 10_000L);
        assertThrows(FindReplace.RegexBudgetExceededException.class, () -> {
            for (int i = 0; i < GuardedCharSequence.CHECK_EVERY + 10; i++) {
                garde.subSequence(0, 2);
            }
        });
    }

    /** Une horloge exactement À l'échéance ne la dépasse pas : pas d'interruption. */
    @Test
    void deadlineExactlyAtBudget_isNotExceeded() {
        long[] appels = {0L};
        GuardedCharSequence garde = new GuardedCharSequence("abcdef",
                FindReplace.REGEX_BUDGET_MS, () -> appels[0]++ < 1 ? 0L : 250L);
        for (int i = 0; i < GuardedCharSequence.CHECK_EVERY + 10; i++) {
            assertEquals(6, garde.length());
        }
        assertEquals('a', garde.charAt(0));
    }
}
