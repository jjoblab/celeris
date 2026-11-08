package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * Budget de ré-analyse de la voie de grammaires (PIEGE-13, critère de
 * sortie M4) : la ré-analyse incrémentale après une édition tient
 * 10 ms sur 50 000 lignes, par langage à grammaire — le protocole
 * retient le minimum de trois tours après échauffement, rattrapé
 * jusqu'à deux fois : le vol de CPU d'un runner partagé ne doit pas
 * casser la sortie, une régression tenue échoue à tous les essais.
 */
class GrammairesBudgetTest {

    private static final int LIGNES = 50_000;
    private static final long BUDGET_NS = 10_000_000L;
    private static final int TOURS = 3;

    /** Rattrapages complets avant de céder au budget. */
    private static final int RATTRAPAGES = 2;

    /** TOML — clés et valeurs par ligne. */
    @Test
    void toml() {
        budget(syntaxeDePaquet("toml"), corpus("exemple.toml"));
    }

    /** YAML — clés et scalaires par ligne. */
    @Test
    void yaml() {
        budget(syntaxeDePaquet("yaml"), corpus("exemple.yaml"));
    }

    /** properties — clé et valeur par ligne. */
    @Test
    void properties() {
        budget(syntaxeDePaquet("properties"), corpus("exemple.properties"));
    }

    /** Markdown — blocs et en ligne. */
    @Test
    void markdown() {
        budget(syntaxeDePaquet("markdown"), corpus("exemple.md"));
    }

    /** Python — la longue traîne. */
    @Test
    void python() {
        budget(syntaxeDePaquet("python"), corpus("exemple.py"));
    }

    /** Shell — la longue traîne. */
    @Test
    void shell() {
        budget(syntaxeDePaquet("shell"), corpus("exemple.sh"));
    }

    /** SQL — les états de la base de l'hôte. */
    @Test
    void sql() {
        budget(syntaxeDePaquet("sql"), corpus("exemple.sql"));
    }

    /** C — le code natif des bibliothèques embarquées. */
    @Test
    void c() {
        budget(syntaxeDePaquet("c"), corpus("exemple.c"));
    }

    /** C++ — le code natif des moteurs et des bindings. */
    @Test
    void cpp() {
        budget(syntaxeDePaquet("cpp"), corpus("exemple.cpp"));
    }

    private static void budget(LanguageSyntax langage, String corpus) {
        String source = replique(corpus, LIGNES);
        Document doc = Document.of(source);
        SyntaxService service = SyntaxService.of(langage);
        Analysis complet = service.analyseFull(doc);
        int cible = source.length() / 2;
        Document edite = doc.replace(cible, cible + 1, "z");
        EditSite site = EditSite.of(doc, edite, cible, cible + 1, "z");
        // échauffement non mesuré : le JIT ne doit pas compter dans le budget
        service.analyseIncremental(edite, complet, site);
        long meilleur = Long.MAX_VALUE;
        for (int essai = 0; essai <= RATTRAPAGES; essai++) {
            long serie = Long.MAX_VALUE;
            for (int tour = 0; tour < TOURS; tour++) {
                long debut = System.nanoTime();
                Analysis diff = service.analyseIncremental(edite, complet, site);
                long duree = System.nanoTime() - debut;
                if (diff.tree().chunks().isEmpty()) {
                    throw new IllegalStateException("arbre vide pour " + langage.id());
                }
                serie = Math.min(serie, duree);
            }
            meilleur = Math.min(meilleur, serie);
            if (meilleur <= BUDGET_NS) {
                break;
            }
        }
        System.out.println(langage.id() + " : ré-analyse de " + LIGNES
                + " lignes en " + meilleur / 1_000_000.0 + " ms (budget 10 ms)");
        assertTrue(meilleur <= BUDGET_NS,
                langage.id() + " : ré-analyse de " + LIGNES + " lignes en "
                        + meilleur / 1_000_000.0 + " ms (budget 10 ms)");
    }

    /** Réplique un corpus jusqu'à la cible de lignes, déterministe. */
    private static String replique(String base, int lignesCible) {
        StringBuilder sb = new StringBuilder(base.length() * (lignesCible / 10 + 1));
        while (compterLignes(sb) < lignesCible) {
            sb.append(base);
            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static int compterLignes(CharSequence texte) {
        int lignes = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes++;
            }
        }
        return lignes;
    }

    private static String corpus(String nom) {
        String chemin = "corpus/" + nom;
        try (InputStream flux = GrammairesBudgetTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("corpus absent : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("corpus illisible : " + chemin, e);
        }
    }

    /** La source syntaxique d'un paquet intégré, par la vraie fabrique. */
    static LanguageSyntax syntaxeDePaquet(String id) {
        var flux = GrammairesBudgetTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return PackGrammars.INSTANCE.compile(id, new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
