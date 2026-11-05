package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Budget de sortie M4 (PIEGE-13, facette moteur) : la ré-analyse
 * incrémentale après une édition tient 10 ms sur 50 000 lignes, par
 * langage P0. Le protocole retient le minimum de trois tours après
 * échauffement, rattrapé jusqu'à deux fois — le vol de CPU d'un runner
 * partagé ne doit pas casser la sortie, une régression tenue échoue à
 * tous les essais.
 *
 * <p>L'édition remplace un caractère au milieu du corpus répliqué — le
 * lexique ne relit que les lignes touchées, l'arbre ne ré-analyse que
 * les chunks propriétaires. L'analyse complète de départ est hors
 * mesure : le budget couvre l'entretien, pas la première venue.</p>
 */
class ReanalyseBudgetTest {

    private static final int LIGNES = 50_000;
    private static final long BUDGET_NS = 10_000_000L;
    private static final int TOURS = 3;

    /** Rattrapages complets avant de céder au budget. */
    private static final int RATTRAPAGES = 2;

    /** Java — chunks par type, la ré-analyse est locale au type édité. */
    @Test
    void java() {
        budget(JavaSyntax.INSTANCE, CorpusChargeur.exempleJava());
    }

    /** Kotlin — chunks par déclaration, la ré-analyse est locale. */
    @Test
    void kotlin() {
        budget(KotlinSyntax.INSTANCE, CorpusChargeur.exempleKotlin());
    }

    /** XML — l'élément racine porte le document, la ré-analyse le relit. */
    @Test
    void xml() {
        budget(XmlSyntax.INSTANCE, CorpusChargeur.exempleXml());
    }

    /** JSON — chaque valeur de premier niveau ouvre son chunk (PIEGE-13). */
    @Test
    void json() {
        budget(JsonSyntax.INSTANCE, CorpusChargeur.exempleJson());
    }

    /** Gradle KTS — un script sans déclaration tient en un chunk. */
    @Test
    void kts() {
        budget(KtsSyntax.INSTANCE, CorpusChargeur.exempleKts());
    }

    /** Gradle Groovy — les appels de premier niveau ouvrent leur chunk (C-07). */
    @Test
    void groovy() {
        budget(GroovySyntax.INSTANCE, CorpusChargeur.exempleGroovy());
    }

    private static void budget(LanguageSyntax langage, String corpus) {
        String source = CorpusChargeur.replique(corpus, LIGNES);
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
        assertTrue(meilleur <= BUDGET_NS,
                langage.id() + " : ré-analyse de " + LIGNES + " lignes en "
                        + meilleur / 1_000_000.0 + " ms (budget 10 ms)");
    }
}
