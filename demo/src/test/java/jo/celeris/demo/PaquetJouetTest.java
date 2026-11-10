package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jo.celeris.core.language.LanguageConfiguration;
import jo.celeris.core.language.LanguagePack;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.language.PackLoader;
import jo.celeris.core.language.Packs;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.IndentationFolds;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.text.Document;
import jo.celeris.core.syntax.grammars.PackGrammars;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le paquet jouet de l'utilisateur (H-04, M9) : copié au dossier des
 * paquets, le langage ENTIER vit — détection par extension, coloration
 * par la grammaire (commentaires et mots-clés), plis par
 * l'indentation, commentaires par la configuration, plan et gabarits
 * par les données — sans un mot de Logo dans le code ; retiré, il
 * disparaît du registre. La démonstration démontre qu'ajouter un
 * langage ne recompile rien.
 */
class PaquetJouetTest {

    @TempDir
    Path bac;

    /** Le registre assemblé du dossier des paquets, comme la démo (H-04). */
    private static LanguageRegistry registre(Path dossier) {
        return LanguagePlatform.with(PackGrammars.INSTANCE)
                .addPackSource(Packs.folder(dossier))
                .build();
    }

    /** L'échantillon du paquet jouet — le contenu de la carte tortue.lgo. */
    private static String echantillon() {
        return FichiersExemple.parNom("tortue.lgo", true).contenu();
    }

    @Test
    void installePuisCharge_toutLeLangageVit() throws IOException {
        PaquetJouet.installer(bac);
        assertTrue(PaquetJouet.installe(bac), "le paquet vit au stockage");
        for (String nom : PaquetJouet.noms()) {
            assertTrue(Files.isRegularFile(bac.resolve(PaquetJouet.LANGAGE)
                    .resolve(nom)), "le fichier " + nom + " y est");
        }

        LanguageSupport logo = registre(bac).byFilename("tortue.lgo");
        assertNotNull(logo, "le langage du paquet se détecte par extension");
        assertEquals(PaquetJouet.LANGAGE, logo.id());
        assertNotNull(logo.syntax(), "la grammaire du paquet s'attache");

        // La coloration : l'analyse porte commentaires et mots-clés —
        // la grammaire du paquet, jamais un analyseur écrit.
        Analysis analyse = SyntaxService.of(logo.syntax())
                .analyseFull(Document.of(echantillon()));
        boolean commentee = false;
        boolean motCle = false;
        for (int ligne = 0; ligne < analyse.lex().lineCount(); ligne++) {
            for (Token jeton : analyse.lex().line(ligne).tokens()) {
                commentee |= jeton.kind() == TokenKind.COMMENT;
                motCle |= jeton.kind() == TokenKind.KEYWORD;
            }
        }
        assertTrue(commentee, "les commentaires se colorent");
        assertTrue(motCle, "les mots-clés se colorent");

        // Les plis : l'indentation de la configuration replie le corps
        // des procédures.
        LanguageConfiguration.Folding pliage = logo.configuration().folding();
        assertTrue(pliage.byIndentation(), "le paquet se replie au décalage");
        List<FoldSpan> plis = TreeFolds.regions(Document.of(echantillon()),
                analyse, new IndentationFolds.Settings(
                        logo.configuration().indentSize(), pliage.offSide()));
        assertFalse(plis.isEmpty(), "les procédures se replient");

        // Les commentaires : le point-virgule de la configuration.
        assertEquals("; ", logo.configuration().comments().lineComment());

        // Le plan et les gabarits : les données du paquet.
        assertFalse(logo.outlineRules().isEmpty(),
                "le plan vit dans outline.json");
        LanguagePack pack = PackLoader.with(PackGrammars.INSTANCE)
                .load(Packs.folder(bac).packs().get(0));
        assertEquals(2, pack.snippets().size(),
                "les gabarits vivent dans snippets.json");
    }

    @Test
    void retire_leLangageDisparait() throws IOException {
        PaquetJouet.installer(bac);
        PaquetJouet.retirer(bac);
        assertFalse(PaquetJouet.installe(bac), "le paquet a disparu du stockage");
        LanguageRegistry registre = registre(bac);
        assertNull(registre.byFilename("tortue.lgo"),
                "l'échantillon ne se détecte plus");
        assertNull(registre.byId(PaquetJouet.LANGAGE),
                "le langage a quitté le registre");
    }

    @Test
    void sansPaquet_lEchantillonNEstPasAuCatalogue() {
        assertFalse(FichiersExemple.catalogue(false).stream()
                        .anyMatch(fichier -> "tortue.lgo".equals(fichier.nom())),
                "sans paquet, pas de carte jouet");
        assertTrue(FichiersExemple.catalogue(true).stream()
                        .anyMatch(fichier -> "tortue.lgo".equals(fichier.nom())),
                "avec paquet, la carte apparaît");
    }
}
