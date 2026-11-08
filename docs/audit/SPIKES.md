# Spikes de mesure (M0)

> Résultats produits par le code de `spikes/` — brutes consignées dans
> `spikes/results/`, commandes de reproduction ci-dessous. Environnement de
> mesure : JVM de bureau (OpenJDK 21, Linux x86-64). **Ce ne sont pas des
> mesures sur appareil** : les budgets de rendu seront calibrés sur
> l'appareil de référence dès M1 (voir ADR-006).

## Reproduction

```bash
javac -d /tmp/spike-classes spikes/rope/RopeSpike.java \
    spikes/syntaxe/SyntaxSpike.java spikes/rendu/RenderSpike.java
java -cp /tmp/spike-classes spike.rope.RopeSpike <clone>/editor/cel-core/src/main/java/jo/codeeditor/rope/Rope.java
java -cp /tmp/spike-classes spike.syntax.SyntaxSpike
java -cp /tmp/spike-classes spike.render.RenderSpike
```

## Spike 1 — re-mesure de la rope du dépôt de départ (ADR-003)

La rope du dépôt de départ est compilée **depuis le clone en lecture seule**
(aucune copie dans ce dépôt avant validation) puis mesurée sur JVM pure :
construire un document de 50 000 lignes (2 M de caractères), appliquer
1 000 éditions d'un caractère par tour, trois tours, minimum retenu
(PIEGE-13).

| Mesure | Valeur |
| --- | --- |
| Édition d'un caractère, moyenne (min de 3 tours) | 0,0019 ms |
| p50 / p95 par édition | 0,0020 ms / 0,0046 ms |
| Maximum observé (première édition, rééquilibrage) | 25,3 ms |
| Mémoire du modèle (différence de tas) | ≈ 1,22 × la taille du texte |
| Budget visé (mandat, ADR-006) | 1 ms par édition |

**Conclusions.** (1) La rope est portée par un facteur ~500 au regard du
budget d'édition : le portage est viable avec une marge énorme. (2) Le chiffre
« 0,11 ms par édition » du mandat mesure visiblement le pipeline complet de
session (décalage d'annotations, plis, composition), pas la rope seule —
l'écart sera re-mesuré à M2 sur la chaîne complète. (3) L'empreinte mémoire
(1,22 × le texte) est déjà sous le budget global de 3 × (modèle + index).
(4) `Rope.java` (395 lignes) n'importe que `java.util` : portage en JVM pure
sans retouche confirmé.

## Spike 2 — lexicographie incrémentale (ADR-004)

Un lexeur volontairement simple (mots-clés, chaînes, commentaires ligne et
bloc, accolades) parcourt un fichier Java synthétique de 50 000 lignes. Chaque
ligne porte l'état transporté (commentaire de bloc ouvert ou non) : après
l'édition d'un caractère, on ne re-lexicographie que depuis la ligne touchée
jusqu'à retrouver l'état déjà mémorisé.

| Mesure | Valeur |
| --- | --- |
| Lexicographie complète des 50 000 lignes (min de 3 tours) | 8,05 ms |
| Re-lexicographie incrémentale après 1 caractère, p50 | 1,6 µs |
| p95 | 4,5 µs |
| Maximum observé (cascade d'un commentaire de bloc + premier tour) | 33,1 ms |
| Lignes re-lexicographiées en moyenne | 1,0 |
| Insertion de ligne (index naïf par recopie) | 39 µs |
| Budget de ré-analyse visé | 10 ms |

**Conclusions.** (1) L'incrémental divise le coût par ~5 000 : la voie
« analyseurs écrits ici, incrémentaux par construction » est crédible avec
une marge massive. (2) Même la re-lexicographie **complète** tient dans le
budget sur JVM de bureau (8 ms) — mais un analyseur syntaxique réel coûte
plusieurs fois le coût de la lexicographie : l'incrémental reste indispensable.
(3) Les cascades existent (éditer l'ouverture d'un commentaire de bloc
 invalide l'état de toutes les lignes suivantes) : elles doivent être bornées
et documentées — c'est un piège à inscrire au registre des tests.

## Spike 3 — rendu par proxy JVM (ADR-006)

Façonnage de lignes de code (80 caractères) avec la bibliothèque graphique de
la JVM en mode headless, en guise de proxy du façonnage natif d'Android.

| Mesure | Valeur |
| --- | --- |
| Création d'une mise en page par ligne (moyenne / p50 / p95) | 47,5 µs / 30,8 µs / 39,8 µs |
| Fenêtre de 50 rangées **sans** cache | 2,38 ms par image |
| Fenêtre de 50 rangées **avec** cache | 0,0007 ms par image |
| Budgets d'image | 16 ms (60 Hz), 8 ms (120 Hz) |

**Conclusions.** (1) Le rapport création/consultation est de l'ordre de
100 000 : le cache de mise en page par rangée n'est pas une optimisation,
c'est une condition d'existence. (2) En extrapolant prudemment un facteur 3 à
5 entre cette JVM et un appareil mobile milieu de gamme, une fenêtre non
cachée consommerait 7 à 12 ms par image : inacceptable à 120 Hz — le
pré-calcul asynchrone hors fil UI et l'invalidation par plage de rangées
s'imposent. (3) La calibration réelle attend M1 et l'appareil de référence ;
ce spike fixe le protocole (mesure par ligne, par fenêtre, distribution p50/p95).

## Limites communes

- Mesures sur JVM de bureau (OpenJDK 21) : utiles pour les ordres de grandeur
  et les ratios, pas pour les budgets absolus de l'appareil.
- Le JIT introduit des maximums trompeurs (premier appel) : seuls p50, p95 et
  le minimum de trois tours sont retenus.
- La rope mesurée est celle du dépôt de départ, hors pipeline de session ;
  la chaîne complète sera budgétée à M2.

## Spike M7.6 — moteur d'expressions rationnelles (ADR-014)

Le jalon M7.6 rouvre la question du moteur que suppose le format TextMate.
Le code vit dans `spikes/regex/` (grammaires réelles et bibliothèques
ramenées par `spikes/regex/recuperer-grammaires`, jamais versionnées) ;
les résultats bruts sont dans `spikes/regex/results/regex.txt` (et les
avertissements du JDK, piécés à l'appui de la divergence silencieuse,
dans `regex-avertissements.txt`).

Reproduction :

```bash
./spikes/regex/recuperer-grammaires
javac -cp demo/libs/serveur-xml-0.31.2.jar:spikes/regex/libs/joni-2.2.9.jar:spikes/regex/libs/jcodings-1.0.64.jar \
    -d spikes/regex/out spikes/regex/SpikeRegex.java
java -cp spikes/regex/out:demo/libs/serveur-xml-0.31.2.jar:spikes/regex/libs/joni-2.2.9.jar:spikes/regex/libs/jcodings-1.0.64.jar \
    spike.regex.SpikeRegex
```

| Mesure (11 grammaires réelles, 3 700 motifs) | Traduction java.util.regex | joni 2.2.9 |
| --- | --- | --- |
| Compilation sans échec | 3 349 (91 %) | 3 670 (99 %) |
| Réécritures silencieuses (sémantique changée) | 122 | 0 |
| Temps, 40 motifs × 50 000 lignes, min de 3 tours | 50–2 778 ms | 73–3 304 ms (rapport 0,46× à 2,56×) |
| Android statique (d8 + dexdump) | — | 615 Ko, 546 méthodes, zéro Unsafe |

Décision : **ADR-014 — joni retenue.**

## Spike M7.6 — client LSP sur LSP4J (ADR-015)

Le harnais `spikes/lsp4j/SpikeLsp4j.java` lance LemMinX 0.31.2 (l'archive
découpée de M8.6) par le `LSPLauncher` de LSP4J sur des tubes à file
(jamais les tubes du JDK — la leçon M8.6 sur la vivacité du fil écrivain),
un lanceur par bout, puis rejoue poignée de main, ouverture, diagnostics
poussés, complétion documentée, survol de DTD et extinction. La version de
LSP4J qui sert les DEUX bouts est décidée par l'ordre du classpath ;
bibliothèques ramenées par `spikes/lsp4j/recuperer-bibliotheques`.

Reproduction :

```bash
./spikes/lsp4j/recuperer-bibliotheques
CP="spikes/lsp4j/libs/lsp4j-1.0.0.jar:spikes/lsp4j/libs/lsp4j-jsonrpc-1.0.0.jar:spikes/lsp4j/libs/gson-2.10.1.jar:demo/libs/serveur-xml-0.31.2.jar"
javac -cp "$CP" -d spikes/lsp4j/out spikes/lsp4j/SpikeLsp4j.java
java -cp "spikes/lsp4j/out:$CP" spike.lsp4j.SpikeLsp4j
```

| Essai (min de 3 lancements, JVM de bureau) | initialize (à froid) | complétion | survol | fil complet |
| --- | --- | --- | --- | --- |
| LSP4J **1.0.0** + LemMinX 0.31.2 | 133–163 ms | 36–64 ms | 11–25 ms | ~600 ms |
| LSP4J 0.24.0 (repli LSP 3.17) | ~130 ms | ~62 ms | ~9 ms | ~570 ms |
| LSP4J 0.23.1 (celle de LemMinX) | ~123 ms | ~68 ms | ~5 ms | ~660 ms |

Les trois versions passent le fil entier — la 1.0.0 est donc compatible
à l'exécution avec le serveur réel, au-delà de sa version de
construction. Android statique (d8 + dexdump, lsp4j + jsonrpc + gson
2.10.1) : 728 Ko de dex, 1 636 méthodes, zéro référence à sun.misc.Unsafe
ou jdk.internal. Arbre des dépendances (POM) : gson seul, intervalle
[2.9.1, 3.0) — la 2.10.1 embarquée par le serveur convient.

Décision : **ADR-015 — LSP4J 1.0.0 derrière la frontière `lsp-lsp4j`,
copie unique avec le serveur par découpe excluant lsp4j et Gson.**
