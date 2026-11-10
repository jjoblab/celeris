package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jo.celeris.core.language.LanguageDescriptor;
import jo.celeris.core.language.LanguageServerDeclaration;
import jo.celeris.core.language.LanguageSupport;

import org.junit.jupiter.api.Test;

/**
 * Le choix du serveur par la DÉCLARATION (M7.6, H-13) : le fichier
 * {@code servers.json} de la démonstration — de la donnée, la sienne —
 * dit quel serveur sert quel langage ; le choix monte le serveur
 * déclaré disponible, replie sur le serveur de référence sinon (M7.5,
 * §8) — l'éditeur, lui, ne connaît aucun serveur, il porte la
 * déclaration jusqu'à son hôte (règle du propriétaire, ADR-005).
 */
class DeclaredServersTest {

    @Test
    void laDeclarationDeLaDemonstrationDescritLeServeurRetire() {
        List<LanguageServerDeclaration> declarations =
                DeclaredServers.declarations();
        assertEquals(1, declarations.size(),
                "la démonstration déclare un serveur, en données");
        LanguageServerDeclaration declaration = declarations.get(0);
        assertEquals("xml", declaration.language());
        assertEquals(LanguageServerDeclaration.EMBEDDED,
                declaration.transport());
        assertTrue(declaration.clientName().startsWith("celeris"),
                "le nom annoncé reste celui de l'application");
    }

    @Test
    void leLangageDeclareMonteSonServeur() {
        LanguageSession session = DeclaredServers.open(
                LangagesDemo.langageDe("exemple.xml", null, null));
        try {
            assertInstanceOf(DeclaredServerSession.class, session,
                    "le langage déclaré reçoit la session de son serveur");
            assertTrue(session.sertCompletion(),
                    "le serveur réel sert la complétion, la négociation le dit");
        } finally {
            session.close();
        }
    }

    @Test
    void unLangageSansDeclarationRepondLeServeurDeReference() {
        LanguageSession session = DeclaredServers.open(
                LangagesDemo.langageDe("exemple.java", null, null));
        try {
            assertInstanceOf(ServeurLangage.class, session,
                    "sans déclaration, le serveur de référence répond");
            assertFalse(session.sertCompletion(),
                    "le serveur de référence ne sert pas la complétion");
        } finally {
            session.close();
        }
    }

    @Test
    void sansLangageLeServeurDeReferenceRepond() {
        LanguageSession session = DeclaredServers.open(null);
        try {
            assertInstanceOf(ServeurLangage.class, session,
                    "sans langage, le serveur de référence répond");
        } finally {
            session.close();
        }
    }

    @Test
    void uneDeclarationIndisponibleReplieSurLeServeurDeReference() {
        LanguageSupport bogue = LanguageSupport
                .builder(LanguageDescriptor.of("bogus", "Bogus", List.of()))
                .servers(List.of(LanguageServerDeclaration.embedded("bogus",
                        "org.inexistant.Serveur", "org.inexistant.Client",
                        "banc", Map.of())))
                .build();
        LanguageSession session = DeclaredServers.open(bogue);
        try {
            assertInstanceOf(ServeurLangage.class, session,
                    "le serveur déclaré indisponible cède la place au repli");
        } finally {
            session.close();
        }
    }
}
