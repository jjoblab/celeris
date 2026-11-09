package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Les instantanés des analyseurs écrits (M7.6, H-10) : la factorisation
 * C-like de Java, Kotlin, KTS et Groovy ne doit RIEN changer à
 * l'observable — chaque corpus embarqué se fige en un texte canonique
 * (jeton par jeton, nœud par nœud) AVANT le remaniement, et le test
 * rejoue l'analyse contre ce témoin APRÈS : les instantanés identiques
 * sont la preuve exigée par la matrice de parité.
 *
 * <p>Le témoin couvre tout ce que les consommateurs lisent : les jetons
 * de chaque ligne (sorte et bornes — la coloration, l'appariement et
 * les plis de commentaires), les bornes des chunks, et l'arbre entier
 * (sorte, catégorie, nom, bornes, erreur — le plan, les plis, les
 * diagnostics et l'indentation). Les états transportés ne s'écrivent
 * pas : ils ne comptent qu'à travers les jetons qu'ils produisent aux
 * lignes suivantes, toutes présentes.</p>
 *
 * <p>Régénération assumée (un changement de comportement VOLONTAIRE
 * passe par ici) : {@code CELERIS_UPDATE_SNAPSHOTS=1 ./gradlew
 * :core-syntax:test --tests ClikeSnapshotsTest} réécrit les témoins
 * puis échoue en le disant — relire la différence, jamais la
 * committer aveuglée.</p>
 */
class ClikeSnapshotsTest {

    /** Un corpus et son analyseur écrit. */
    private record Corpus(String name, LanguageSyntax syntax,
                          java.util.function.Supplier<String> source) {
    }

    private static final List<Corpus> CORPORA = List.of(
            new Corpus("c-like-java", JavaSyntax.INSTANCE, CorpusChargeur::exempleJava),
            new Corpus("c-like-kotlin", KotlinSyntax.INSTANCE, CorpusChargeur::exempleKotlin),
            new Corpus("c-like-kts", KtsSyntax.INSTANCE, CorpusChargeur::exempleKts),
            new Corpus("c-like-groovy", GroovySyntax.INSTANCE, CorpusChargeur::exempleGroovy));

    @Test
    void snapshotsIdentiques() {
        boolean maj = "1".equals(System.getenv("CELERIS_UPDATE_SNAPSHOTS"))
                || Boolean.getBoolean("celeris.updateSnapshots");
        StringBuilder echecs = new StringBuilder();
        for (Corpus corpus : CORPORA) {
            String voulu = instantane(corpus);
            Path temoin = Path.of("src/test/resources/snapshots",
                    corpus.name() + ".txt");
            if (maj) {
                reecrire(temoin, voulu);
                echecs.append(corpus.name()).append(" : témoin réécrit — relire\n");
                continue;
            }
            String attendu = lire(temoin);
            if (!attendu.equals(voulu)) {
                echecs.append(corpus.name()).append(" : l'analyse a changé — ")
                        .append(difference(attendu, voulu)).append('\n');
            }
        }
        if (!echecs.isEmpty()) {
            fail("instantanés divergents (H-10) :\n" + echecs);
        }
    }

    /** Le texte canonique d'une analyse complète du corpus. */
    private static String instantane(Corpus corpus) {
        Document doc = Document.of(corpus.source().get());
        Analysis analyse = SyntaxService.of(corpus.syntax()).analyseFull(doc);
        StringBuilder sortie = new StringBuilder();
        LexDocument lex = analyse.lex();
        for (int ligne = 0; ligne < lex.lineCount(); ligne++) {
            for (Token jeton : lex.line(ligne).tokens()) {
                sortie.append("lex ").append(ligne).append(' ')
                        .append(jeton.kind()).append(' ')
                        .append(jeton.start()).append(' ')
                        .append(jeton.end()).append('\n');
            }
        }
        SyntaxTree arbre = analyse.tree();
        List<Chunk> chunks = arbre.chunks();
        for (int c = 0; c < chunks.size(); c++) {
            Chunk chunk = chunks.get(c);
            sortie.append("chunk ").append(c).append(' ')
                    .append(arbre.chunkStartOffset(doc, chunk)).append('\n');
            ecrireNoeud(sortie, chunk.root(), 0);
        }
        return sortie.toString();
    }

    /** Un nœud par ligne : profondeur, sorte, catégorie, nom, bornes, erreur. */
    private static void ecrireNoeud(StringBuilder sortie, SynNode noeud, int profondeur) {
        String nom = noeud.outlineName() == null ? "-" : noeud.outlineName();
        sortie.append("node ").append(profondeur).append(' ')
                .append(noeud.kind()).append(' ')
                .append(noeud.category()).append(' ')
                .append(nom).append(' ')
                .append(noeud.start()).append(' ')
                .append(noeud.end()).append(' ')
                .append(noeud.isError()).append('\n');
        for (SynNode enfant : noeud.children()) {
            ecrireNoeud(sortie, enfant, profondeur + 1);
        }
    }

    /** La première ligne divergente du témoin, pour le message d'échec. */
    private static String difference(String attendu, String voulu) {
        String[] lignesA = attendu.split("\n", -1);
        String[] lignesV = voulu.split("\n", -1);
        int borne = Math.min(lignesA.length, lignesV.length);
        for (int i = 0; i < borne; i++) {
            if (!lignesA[i].equals(lignesV[i])) {
                return "première divergence ligne " + (i + 1) + " : attendu « "
                        + lignesA[i] + " », obtenu « " + lignesV[i] + " »";
            }
        }
        return "longueurs divergentes : " + lignesA.length + " contre " + lignesV.length;
    }

    private static String lire(Path temoin) {
        try {
            return Files.readString(temoin, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("témoin illisible : " + temoin, e);
        }
    }

    private static void reecrire(Path temoin, String contenu) {
        try {
            Files.createDirectories(temoin.getParent());
            Files.writeString(temoin, contenu, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("témoin inécrivable : " + temoin, e);
        }
    }
}
