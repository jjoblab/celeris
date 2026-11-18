package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;
import jo.celeris.core.syntax.clike.JavaSyntax;

import org.junit.jupiter.api.Test;

/**
 * Le chargeur de langues du kit (ADR-0022, phase 1 — T5) : le registre
 * s'assemble UNE fois, hors fil d'appel (l'exécuteur s'injecte, le banc
 * n'attend aucun fil réel) ; la résolution par nom de fichier répond
 * dès le premier appel (la vue reste éditable sans coloration
 * pendant le chargement, la langue arrive dès que le registre vit) ;
 * le même kit sert plusieurs éditeurs (partagé, l'assemblage ne se
 * paie qu'une fois) ; le repli rend null sans lever ; le rechargement
 * à chaud relit les sources sans toucher aux langues déjà servies.
 */
final class LanguageKitTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE = (id, json) -> JavaSyntax.INSTANCE;

    /** L'exécuteur de banc : tout tourne sur le fil appelant. */
    private static final java.util.concurrent.Executor DIRECT =
            Runnable::run;

    /** Le kit assemblé des paquets intégrés — la voie du banc. */
    private static LanguageKit kitIntegre() {
        return LanguageKit.builder(DIRECT)
                .grammarFactory(FABRIQUE)
                .builtinPacks()
                .build();
    }

    /** L'assemblage part sur l'exécuteur, la résolution attend la fin :
     *  la langue du fichier s'y résout, le chemin sans langage rend
     *  null sans lever (.txt est « Plain Text », livré ; l'extension
     *  inconnue reste sans langage). */
    @Test
    void laLangueSeResoutApresAssemblage() {
        LanguageKit kit = kitIntegre();
        LanguageSupport java = kit.languageFor("Main.java");
        assertTrue(java != null && "java".equals(java.id()),
                "le .java se résout en java");
        LanguageSupport texte = kit.languageFor("notes.txt");
        assertTrue(texte != null && "text".equals(texte.id()),
                "le .txt se résout en Plain Text, langage livré");
        assertNull(kit.languageFor("montage.inconnu"),
                "une extension inconnue rend null, pas d'exception");
    }

    /** Le registre s'assemble UNE fois : deux résolutions ne paient
     *  qu'un assemblage — la source ne se lit qu'au premier appel. */
    @Test
    void lassemblageNeSePaieQuuneFois() {
        AtomicInteger lectures = new AtomicInteger();
        PackSource sourceComptee = () -> {
            lectures.incrementAndGet();
            return Packs.classpath("langages").packs().stream()
                    .filter(f -> f.name().endsWith("java"))
                    .toList();
        };
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .grammarFactory(FABRIQUE)
                .addSource(sourceComptee)
                .build();
        kit.languageFor("A.java");
        kit.languageFor("B.java");
        assertEquals(1, lectures.get(),
                "la source ne se lit qu'au premier assemblage");
    }

    /** Le kit est PARTAGÉ : plusieurs éditeurs y résolvent leurs
     *  langues, l'assemblage ne se paie toujours qu'une fois. */
    @Test
    void leKitPartageSertPlusieursEditeurs() {
        LanguageKit kit = kitIntegre();
        LanguageSupport java = kit.languageFor("Main.java");
        LanguageSupport python = kit.languageFor("script.py");
        assertSame(kit.languageFor("Autre.java"), java,
                "le même support revient au second éditeur");
        assertTrue(python != null && !"java".equals(python.id()),
                "le python ne se confond pas avec le java");
    }

    /** Le rechargement à chaud : les sources se relisent, les langues
     *  déjà servies restent celles de leur époque (le support déjà
     *  remis aux sessions ouvertes ne bouge pas — USAGE § 6). */
    @Test
    void leRechargementRelitLesSources() {
        AtomicInteger lectures = new AtomicInteger();
        PackSource sourceComptee = () -> {
            lectures.incrementAndGet();
            return List.of();
        };
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .addSource(sourceComptee)
                .build();
        kit.languageFor("A.java");
        assertEquals(1, lectures.get());
        kit.reload();
        kit.languageFor("B.java");
        assertEquals(2, lectures.get(),
                "le rechargement relit les sources au prochain assemblage");
    }

    /** La voie sans grammaire : le kit se construit aussi sans
     *  fabrique — le support écrit posé directement s'y résout (les
     *  paquets à grammaire exigent la fabrique, le support direct
     *  n'en a pas besoin). */
    @Test
    void laVoieSansGrammaireVit() {
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .addSupport(LanguageSupport.builder(
                                LanguageDescriptor.of("essai", "Essai",
                                        List.of("essai")))
                        .syntax(JavaSyntax.INSTANCE)
                        .build())
                .build();
        LanguageSupport resolu = kit.languageFor("montage.essai");
        assertTrue(resolu != null && "essai".equals(resolu.id()),
                "le support écrit posé directement se résout");
    }
}
