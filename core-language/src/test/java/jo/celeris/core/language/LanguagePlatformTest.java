package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.JsonSyntax;
import jo.celeris.core.syntax.XmlSyntax;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Les tests de la plateforme de langages (M7.6) : l'assemblage des
 * paquets intégrés (les seize langages, les analyseurs écrits attachés
 * par préférence, la famille kts→kotlin liée), les contributeurs de
 * capacités, les fournisseurs génériques — et les refus précis : parent
 * inconnu, préférence sans source.
 */
final class LanguagePlatformTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE = (id, json) -> JavaSyntax.INSTANCE;

    /** Un fournisseur de complétion de banc. */
    private record Mots(String etiquette) implements Completion {
        @Override
        public List<String> candidates(Document document,
                @Nullable Analysis analysis, int caretOffset) {
            return List.of(etiquette);
        }
    }

    private static LanguageRegistry registreIntegre() {
        return LanguagePlatform.with(FABRIQUE).builtinPacks().build();
    }

    /** Le support résolu — le banc meurt si l'identifiant manque. */
    private static LanguageSupport supportDe(LanguageRegistry registre, String id) {
        LanguageSupport support = registre.byId(id);
        assertNotNull(support, id);
        return support;
    }

    @Test
    void lesSeizeLangagesIntegresSAssemblent() {
        LanguageRegistry registre = registreIntegre();
        assertEquals(16, registre.supports().size());
        for (String id : new String[] {"java", "kotlin", "kts", "groovy",
                "xml", "json", "toml", "yaml", "properties", "markdown",
                "python", "shell", "sql", "c", "cpp", "proguard"}) {
            assertNotNull(registre.byId(id), id);
        }
    }

    @Test
    void lesAnalyseursEcritsSAttachentParPreference() {
        LanguageRegistry registre = registreIntegre();
        assertSame(JavaSyntax.INSTANCE, supportDe(registre, "java").syntax());
        assertSame(JsonSyntax.INSTANCE, supportDe(registre, "json").syntax());
        assertSame(XmlSyntax.INSTANCE, supportDe(registre, "xml").syntax());
    }

    @Test
    void laFamilleKtsHeriteDeKotlin() {
        LanguageRegistry registre = registreIntegre();
        LanguageSupport kts = supportDe(registre, "kts");
        LanguageSupport kotlin = supportDe(registre, "kotlin");
        assertSame(kotlin, kts.parent());
        assertEquals("kotlin", kts.descriptor().parentId());
    }

    @Test
    void unFournisseurGeneriqueSertTousLesLangages() {
        Mots mots = new Mots("mots");
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .builtinPacks()
                .generic(Completion.class, mots)
                .build();
        for (LanguageSupport support : registre.supports()) {
            assertEquals(mots, support.capability(Completion.class),
                    support.id() + " sert la complétion générique");
        }
    }

    @Test
    void unContributeurAjouteUneCapaciteDeLangage() {
        Mots duXml = new Mots("xml");
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .builtinPacks()
                .contributor((contribution) ->
                        contribution.capability("xml", Completion.class, duXml))
                .build();
        assertEquals(duXml, supportDe(registre, "xml").capability(Completion.class));
        assertNull(supportDe(registre, "java").capability(Completion.class));
    }

    @Test
    void unParentInconnuRefuseLassemblee() {
        LanguageSupport orphelin = LanguageSupport.builder(
                new LanguageDescriptor("orphelin", "Orphelin", List.of(),
                        List.of("orph"), List.of(), List.of(), null, List.of(),
                        "inconnu", null, "O", 0xFF000000))
                .build();
        LanguagePlatform plateforme = LanguagePlatform.with(FABRIQUE)
                .builtinPacks().support(orphelin);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                plateforme::build);
        assertTrue(String.valueOf(echec.getMessage()).contains("unknown parent"));
    }

    @Test
    void leReglageDeLutilisateurPasseAuRegistre() {
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .builtinPacks()
                .userChoice("donnees.h", "python")
                .build();
        LanguageSupport impose = registre.byFilename("donnees.h");
        assertNotNull(impose, "le réglage résout");
        assertEquals("python", impose.id());
    }

    @Test
    void leBadgeEtLaCouleurViennentDesDonnees() {
        LanguageRegistry registre = registreIntegre();
        assertEquals("Jv", supportDe(registre, "java").descriptor().badge());
        assertEquals(0xFFCC7832, supportDe(registre, "java").descriptor().badgeColor());
        assertEquals("C+", supportDe(registre, "cpp").descriptor().badge());
        assertEquals("Kotlin Script", supportDe(registre, "kts").displayName());
    }

    @Test
    void laDetectionDesPaquetsIntegresResolveLesFamilles() {
        LanguageRegistry registre = registreIntegre();
        assertEquals("kts", idDeFichier(registre, "build.gradle.kts"));
        assertEquals("groovy", idDeFichier(registre, "build.gradle"));
        assertEquals("kotlin", idDeFichier(registre, "Conversions.kt"));
        assertEquals("proguard", idDeFichier(registre, "regles.pro"));
        assertNull(registre.byFilename("inconnu.xyz"));
    }

    @Test
    void lesInjectionsEtPreferencesSePortentDansLePack() {
        LanguageRegistry registre = registreIntegre();
        assertTrue(supportDe(registre, "java").injections().isEmpty());
        LanguagePack pack = packDe("java");
        assertEquals("handwritten", pack.syntaxPreference().preferred());
        assertNull(pack.syntaxPreference().fallback());
    }

    /** L'identifiant résolu d'un nom de fichier — le banc meurt sinon. */
    private static String idDeFichier(LanguageRegistry registre, String nom) {
        LanguageSupport support = registre.byFilename(nom);
        assertNotNull(support, nom);
        return support.id();
    }

    private static LanguagePack packDe(String id) {
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        try {
            for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
                LanguagePack pack = chargeur.load(fichiers);
                if (pack.manifest().id().equals(id)) {
                    return pack;
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("paquet inconnu : " + id);
    }
}
