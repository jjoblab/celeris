package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Les usages SANS serveur (le repli local, l'audit des surfaces
 * muettes) : la carte des usages ne se tait plus quand aucun serveur
 * de langage n'est branché — l'index du dépôt sert les occurrences du
 * document ouvert, la langue courante y lit son analyse. Pur, sans
 * appareil : la méthode locale vit dans {@code Usages}, la carte se
 * pose au banc du kit.
 */
final class UsagesSansServeurTest {

    private static final String TEXTE = """
            int total = 1;
            int copie = total;
            int autre = total + 1;
            """;

    /** Le repli local sert les occurrences du document ouvert : les
     *  trois « total » (déclaration et relectures) se listent, triés
     *  par offset croissant — le serveur n'y est pour rien. */
    @Test
    void leRepliLocalSertLesOccurrencesDuDocument() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        Document document = session.document();
        List<Integer> offsets = Usages.usagesLocaux(document,
                JavaSyntax.INSTANCE, "total");
        assertEquals(3, offsets.size(),
                "les trois occurrences se servent sans serveur");
        assertTrue(offsets.equals(offsets.stream().sorted().toList()),
                "les offsets arrivent croissants");
        assertEquals(TEXTE.indexOf("total"), offsets.get(0).intValue(),
                "la déclaration vient en premier");
    }

    /** Le symbole absent ne sert rien : le repli rend vide, jamais un
     *  faux usage à deviner. */
    @Test
    void leSymboleAbsentRendVide() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        assertTrue(Usages.usagesLocaux(session.document(),
                JavaSyntax.INSTANCE, "introuvable").isEmpty(),
                "aucun usage ne s'invente");
    }

    /** Le mot clé du langage ne se liste pas : l'index des symboles
     *  n'indexe que les identificateurs — « int » n'est pas un usage. */
    @Test
    void leMotCleNeSIndexePas() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        assertTrue(Usages.usagesLocaux(session.document(),
                JavaSyntax.INSTANCE, "int").isEmpty(),
                "les mots réservés ne sont pas des usages");
    }
}
