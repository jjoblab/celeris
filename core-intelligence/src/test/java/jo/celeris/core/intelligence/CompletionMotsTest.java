package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * La complétion de base (B-03) : le préfixe tape, les mots du fichier
 * répondent — sans serveur, sans réseau, au mot près. Les mots du CODE
 * seulement : les jetons du lexique décident, jamais les chaînes ni les
 * commentaires.
 */
class CompletionMotsTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    private List<String> completer(String texte, int caret, List<String> motsCles) {
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        return CompletionMots.completer(doc, analyse, caret, motsCles);
    }

    /** Le préfixe attrape les mots du fichier, insensible à la casse. */
    @Test
    void lePrefixeAttrapeLesMots() {
        String texte = """
                class Client {
                    int largeur;
                    int longueur;
                    void lire() { }
                }
                """;
        // le curseur après « l » de lire : le préfixe est l — le mot tapé
        // ne compte pas, les deux autres répondent
        int caret = texte.indexOf("lire") + 1;
        assertEquals(List.of("largeur", "longueur"),
                completer(texte, caret, List.of()));
        // le même fichier en majuscules : la casse n'y change rien
        String majuscules = texte.toUpperCase(java.util.Locale.ROOT);
        int caretMaj = majuscules.indexOf("LIRE") + 1;
        assertEquals(List.of("LARGEUR", "LONGUEUR"),
                completer(majuscules, caretMaj, List.of()));
    }

    /** Le mot en cours de frappe n'est pas son propre candidat. */
    @Test
    void leMotEnCoursNEstPasCandidat() {
        String texte = "int alpha;\nint alphabeta;\n";
        // curseur à la fin de « alpha » (première occurrence) : alpha n'est pas proposé,
        // alphabeta oui — le mot tapé ne se propose pas à lui-même
        int caret = texte.indexOf("alpha") + 5;
        assertEquals(List.of("alphabeta"), completer(texte, caret, List.of()));
    }

    /** Un mot identique ailleurs dans le fichier reste candidat. */
    @Test
    void leMotIdentiqueAilleursResteCandidat() {
        String texte = "int alpha;\nint beta = alpha;\n";
        // curseur après « al » de la seconde occurrence d'alpha
        int caret = texte.lastIndexOf("alpha") + 2;
        // l'autre occurrence rend alpha candidat ; beta ne commence pas par al
        assertEquals(List.of("alpha"), completer(texte, caret, List.of()));
    }

    /** Les chaînes et les commentaires ne prêtent jamais leurs mots. */
    @Test
    void lesChainesEtCommentairesNeComptentPas() {
        String texte = """
                class A {
                    // horloge en commentaire
                    String s = "horloge dans une chaine";
                    int hors;
                    int hote;
                }
                """;
        // « ho » : le mot tapé hors ne compte pas, hote répond — horloge
        // ni en commentaire ni en chaîne ne se propose
        int caret = texte.indexOf("hors") + 2;
        assertEquals(List.of("hote"), completer(texte, caret, List.of()));
    }

    /** Les mots réservés du langage se joignent aux mots du fichier. */
    @Test
    void lesMotsReservesSeJoignent() {
        String texte = "int retour;\n";
        // curseur après « re » : le mot en cours ne compte pas, les réservés oui
        int caret = texte.indexOf("retour") + 2;
        assertEquals(List.of("reader", "return"),
                completer(texte, caret, List.of("return", "reader")));
    }

    /** Le curseur dans le blanc : les mots du fichier, triés sans égard à la casse. */
    @Test
    void leCurseurDansLeBlanc() {
        String texte = "int Zebre;\nint abeille;\n";
        // curseur sur le saut de ligne : aucun mot touché, le préfixe est
        // vide — les mots réservés (int) ne sont pas des mots du fichier,
        // ils se joignent par le paramètre dédié
        int caret = texte.indexOf('\n');
        assertEquals(List.of("abeille", "Zebre"),
                completer(texte, caret, List.of()));
    }

    /** Le document vide : rien à proposer, pas d'exception. */
    @Test
    void leDocumentVide() {
        assertTrue(completer("", 0, List.of()).isEmpty());
        assertEquals(List.of("return"), completer("", 0, List.of("return")));
    }

    /** Le curseur au milieu d'un mot : le préfixe est la partie déjà tapée. */
    @Test
    void leCurseurAuMilieu() {
        String texte = "int alphabet;\nint alpha;\n";
        // curseur après « alp » d'alphabet : le préfixe est alp — le mot tapé
        // ne compte pas, l'autre mot répond
        int caret = texte.indexOf("alphabet") + 3;
        assertEquals(List.of("alpha"), completer(texte, caret, List.of()));
        // curseur après « alph » : le préfixe grandit, la réponse tient
        int apres = texte.indexOf("alphabet") + 4;
        assertEquals(List.of("alpha"), completer(texte, apres, List.of()));
    }
}
