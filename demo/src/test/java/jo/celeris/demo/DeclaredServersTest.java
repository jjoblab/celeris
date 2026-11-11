package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
    void laDemonstrationNeDeclareAucunServeur() {
        List<LanguageServerDeclaration> declarations =
                DeclaredServers.declarations();
        assertEquals(List.of(), declarations,
                "sans déclaration, la démonstration vit sur son repli");
    }

    @Test
    void leLangageSansDeclarationRepondLeServeurDeReference() {
        LanguageSession session = DeclaredServers.open(
                LangagesDemo.langageDe("exemple.xml", null, null));
        try {
            assertInstanceOf(ServeurLangage.class, session,
                    "sans déclaration servable, le serveur de référence répond");
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

    @Test
    void uneDeclarationDePaquetNeMonteJamaisSansLhote() {
        // revue de sécurité M9 : un paquet malveillant déclare un serveur
        // embarqué dont la classe EXISTE au chemin de classes — sans la
        // décision de l'hôte, la classe ne s'instancie jamais
        LanguageSupport malveillant = LanguageSupport
                .builder(LanguageDescriptor.of("malin", "Malin", List.of()))
                .servers(List.of(LanguageServerDeclaration.embedded("malin",
                        "jo.celeris.demo.ServeurLangage",
                        "jo.celeris.demo.LanguageSession",
                        "paquet-piege", Map.of())))
                .build();
        LanguageSession session = DeclaredServers.open(malveillant);
        try {
            assertInstanceOf(ServeurLangage.class, session,
                    "la déclaration d'un paquet ne monte rien : seul "
                            + "l'hôte décide de l'instanciation");
            assertFalse(session.sertCompletion(),
                    "le repli répond, pas le serveur du paquet");
        } finally {
            session.close();
        }
    }
}
