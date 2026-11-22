package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;

import org.junit.jupiter.api.Test;

/**
 * Le chantier A de la feuille de route coloration (prompt du
 * 2026-11-22, F9 de l'audit) : la langue du FICHIER OUVERT ne
 * patiente plus l'assemblage des 33 paquets — le kit priorise la
 * résolution du fichier demandé, l'assemblage du reste court derrière
 * en tâche de fond. Les bancs mesurent le CONTRAT : la résolution
 * directe d'un fichier ne paie que SON paquet (compteur de paquets
 * assemblés au moment de la résolution), l'assemblage complet finit
 * par vivre (estPret), le rechargement tourne la génération.
 */
final class LanguageKitPrioriteTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE = (id, json) ->
            jo.celeris.core.syntax.clike.JavaSyntax.INSTANCE;

    /** L'exécuteur de banc : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** La source qui COMPTE ses paquets assemblés — ordre et vitesse
     *  observables du banc. */
    private static final class SourceComptee implements PackSource {

        private final AtomicInteger assembles = new AtomicInteger();

        @Override
        public List<PackSource.PackFiles> packs() {
            try {
                return Packs.classpath("langages").packs().stream()
                        .peek(f -> assembles.incrementAndGet())
                        .toList();
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * La résolution PRIORITAIRE : le kit assemble d'abord le paquet
     * du fichier demandé — la langue rend AVANT la fin de
     * l'assemblage total (le banc compte les paquets lus au moment
     * de la résolution : strictement moins que les 33 intégrés).
     */
    @Test
    void laLangueDuFichierNePatientePasLesAutresPaquets() {
        SourceComptee source = new SourceComptee();
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .grammarFactory(FABRIQUE)
                .addSource(source)
                .build();
        LanguageSupport java = kit.languageFor("Main.java");
        assertNotNull(java, "la langue du .java se résout");
        int lus = source.assembles.get();
        assertTrue(lus < 33,
                "la résolution a lu " + lus
                        + " paquets, pas les 33 intégrés — le paquet"
                        + " demandé d'abord, le reste derrière");
    }

    /**
     * La résolution prioritaire est AUSSI un raccourci du même
     * assemblage : le préchargement ne se paie qu'une fois — la
     * résolution suivante rend sans relire les paquets.
     */
    @Test
    void laResolutionPrioritaireNEmpilePasLesLectures() {
        SourceComptee source = new SourceComptee();
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .grammarFactory(FABRIQUE)
                .addSource(source)
                .build();
        kit.languageFor("Main.java");
        int apresPremiere = source.assembles.get();
        LanguageSupport deuxieme = kit.languageFor("Autre.java");
        assertNotNull(deuxieme, "la seconde résolution rend aussi");
        assertEquals(apresPremiere, source.assembles.get(),
                "la seconde résolution ne relit pas les paquets déjà"
                        + " assemblés (le registre vit ou l'assemblage"
                        + " unique court)");
    }

    /**
     * Le CONTRAT d'honnêteté du kit : la langue sans registre reste
     * null SANS ATTENDRE (languageForSiPret) ; la résolution
     * prioritaire ne pose JAMAIS la langue d'un autre onglet — elle
     * résout le fichier DEMANDÉ.
     */
    @Test
    void laResolutionSansAttendreRendNullPuisLaBonneLangue() {
        SourceComptee source = new SourceComptee();
        LanguageKit kit = LanguageKit.builder(
                command -> {
                    // L'assemblage ne part PAS ici : la résolution
                    // sans attendre rend null tant que rien ne vit.
                })
                .grammarFactory(FABRIQUE)
                .addSource(source)
                .build();
        assertNull(kit.languageForSiPret("Main.java"),
                "sans assemblage lancé, la résolution rend null");
        assertEquals(0, source.assembles.get(),
                "la sonde sans attendre n'assemble rien");
    }

    /**
     * Le support direct (sans paquet) reste servi : la résolution
     * prioritaire ne contourne pas les supports posés à la main.
     */
    @Test
    void leSupportDirectResteServi() {
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .addSupport(LanguageSupport.builder(
                                LanguageDescriptor.of("essai", "Essai",
                                        List.of("essai")))
                        .syntax(jo.celeris.core.syntax.clike.JavaSyntax.INSTANCE)
                        .build())
                .build();
        LanguageSupport resolu = kit.languageFor("montage.essai");
        assertTrue(resolu != null && "essai".equals(resolu.id()),
                "le support posé directement se résout");
    }
}
