/**
 * Socle d'analyse syntaxique incrémental (ADR-004) : jetons, états
 * transportés par ligne, arbre tolérant ancré par chunks, crochets
 * appariés, plis, plan et diagnostics — tous consommateurs du même
 * arbre. Les analyseurs des langages P0 (Java, Kotlin, KTS, XML, JSON,
 * Groovy) s'appuient sur ce socle ; la voie de grammaires couvre la
 * longue traîne.
 */
@org.jspecify.annotations.NullMarked
package jo.celeris.core.syntax;
