package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le pont session ↔ vue en mode hors fil d'appel (tranche 6 M4, G-01,
 * PIEGE-20) : l'exécuteur s'injecte, la première image ne paie jamais
 * l'arbre — sans analyse publiée, la source passe sans couleur ; la
 * publication habille les rangées et prévient la vue (le rappel
 * d'invalidation) ; l'édition pendant le vol rattrape par remappage —
 * les lignes épargnées gardent leurs couleurs, la ligne touchée
 * attend sa publication à l'encre de base ; jamais une ligne au
 * contenu non prouvé identique ne se colore ; le retrait du langage
 * referme le moteur.
 */
class EditorSyntaxAsyncTest {

    private static final String SOURCE = """
            package demo;

            class Premiere {
                void methode() {
                }
            }

            class Deuxieme {
            }
            """;

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** Exécuteur à commande : les tâches s'enregistrent, on les joue à la main. */
    private static final class ExecuteurACommande implements Executor {
        private final List<Runnable> file = new ArrayList<>();

        @Override
        public void execute(Runnable commande) {
            file.add(commande);
        }

        void jouer() {
            List<Runnable> enAttente = new ArrayList<>(file);
            file.clear();
            enAttente.forEach(Runnable::run);
        }
    }

    /** Une source qui ne rend rien : habiller l'enveloppe ou la laisse, jamais ne la consulte ici. */
    private static final RowSource SOURCE_MUETTE = cle -> {
        throw new UnsupportedOperationException("pas consultée par le pont");
    };

    /** Source de rangées de test : sans pli ni coupure, la rangée r porte la ligne r. */
    private static final class SourceParLigne implements RowSource {
        private final Document document;

        SourceParLigne(Document document) {
            this.document = document;
        }

        @Override
        public RowLayout layoutOf(RowKey key) {
            String texte = document.lineText(key.row());
            return new RowLayout(key.row(), 0, texte.length(), texte,
                    texte.length() * 10f);
        }
    }

    /**
     * Sans analyse publiée, la source passe telle quelle : la première
     * image ne paie jamais l'arbre (PIEGE-20, facette vue).
     */
    @Test
    void sansAnalysePublieeLaSourceResteSansCouleur() {
        ExecuteurACommande executeur = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        assertFalse(executeur.file.isEmpty(), "le moteur a programmé son ouverture");
        Document document = session.document();
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, document),
                "sans analyse publiée, pas de coloration");
        executeur.jouer();
        assertFalse(syntaxe.habiller(SOURCE_MUETTE, document) == SOURCE_MUETTE,
                "après publication, la source se pare");
        syntaxe.fermer();
    }

    /** La publication prévient la vue : le rappel d'invalidation part à chaque instantané. */
    @Test
    void publicationPrevientLaVue() {
        AtomicBoolean prevenue = new AtomicBoolean();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        syntaxe.setSurAnalysePrete(() -> prevenue.set(true));
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        assertTrue(prevenue.get(), "la publication directe prévient la vue");
        syntaxe.fermer();
    }

    /**
     * L'édition pendant le vol rattrape par remappage : les lignes
     * épargnées par l'édition gardent les plages de la publication, la
     * ligne touchée retombe à l'encre de base — puis la publication à
     * jour recolore la ligne touchée (PIEGE-20 : seule une ligne au
     * contenu démontré identique se colore).
     */
    @Test
    void editionPendantLeVolRattrapeParRemappage() {
        ExecuteurACommande executeur = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executeur.jouer();
        int cible = SOURCE.indexOf("methode");
        session.replaceRange(cible, cible + 7, "autre");
        Document edite = session.document();
        RowSource habillee = syntaxe.habiller(
                new SourceParLigne(edite), edite);

        assertFalse(habillee == SOURCE_MUETTE,
                "la publication périmée rattrape : la source se pare");
        RowLayout ligneClasse = habillee.layoutOf(
                new RowKey(edite.revision(), 2, 1, new FontSpec("mono", 14f)));
        assertFalse(ligneClasse.spans().isEmpty(),
                "la ligne épargnée (class Premiere) garde ses couleurs");
        RowLayout ligneTouchee = habillee.layoutOf(
                new RowKey(edite.revision(), 3, 1, new FontSpec("mono", 14f)));
        assertFalse(ligneTouchee.spans().isEmpty(),
                "PIEGE-26 : la ligne touchée garde ses couleurs publiées "
                        + "recalées — plus d'encre de base en attendant");

        executeur.jouer();
        RowSource exacte = syntaxe.habiller(new SourceParLigne(edite), edite);
        RowLayout recoloree = exacte.layoutOf(
                new RowKey(edite.revision(), 3, 1, new FontSpec("mono", 14f)));
        assertFalse(recoloree.spans().isEmpty(),
                "la publication de la bonne révision recolore la ligne touchée");
        syntaxe.fermer();
    }

    /**
     * Une rafale d'éditions au-delà du plafond rompt la chaîne : le
     * rattrapage cède — la source reste sans couleur jusqu'à la
     * publication, jamais le remappage ne devine.
     */
    @Test
    void rafaleAuDelaDuPlafondRomptLaChaine() {
        ExecuteurACommande executeur = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executeur.jouer();
        int cible = SOURCE.indexOf("Premiere");
        for (int i = 0; i < 300; i++) {
            session.replaceRange(cible, cible, "x");
            cible++;
        }
        Document edite = session.document();
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, edite),
                "chaîne rompue : sans couleur, le remappage ne devine pas");
        executeur.jouer();
        assertFalse(syntaxe.habiller(SOURCE_MUETTE, edite) == SOURCE_MUETTE,
                "la publication à jour répare la chaîne et recolore");
        syntaxe.fermer();
    }

    /** Le retrait du langage referme le moteur et vide la coloration. */
    @Test
    void retraitDuLangageRefermeLeMoteur() {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        syntaxe.setLanguage(null);
        assertFalse(syntaxe.active(), "sans langage, plus de moteur");
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, session.document()));
        syntaxe.fermer();
    }
}
