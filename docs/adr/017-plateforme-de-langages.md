# ADR-017 — Plateforme de langages : paquets, modèle et coutures

- **Statut** : Acceptée (jalon M7.6, étape 4)
- **Date** : 2026-11-08

## Contexte

Le prompt M7.6 demande une plateforme de langages pilotée par des
données : « un langage de plus doit pouvoir arriver sans recompiler,
sous forme de données ». L'audit mesure la situation d'avant : table
d'extensions dupliquée en quatre endroits au moins (le cli, la démo
deux fois, le registre), `CommentRules` et `TypingRules` aiguillent
par identifiant, le catalogue des grammaires vit dans
`LangagesGrammaires` en deux listes tenues à la main, et le registre
n'a aucun consommateur de production.

## Décision proposée

1. **Le modèle vit dans `core-language`** (descripteur,
   configuration, support, capacités, détection, registre), qui
   dépend déjà de `core-syntax` — le support porte sa source
   syntaxique directement. « Zéro dépendance » s'entend des
   dépendances tierces : le module reste Java pur.
2. **Les paquets intégrés sont des ressources de `core-language`**
   (`langages/<id>/pack.json`, `language.json`, `grammar.json`,
   `snippets.json`). L'index des paquets se GÉNÈRE à la construction
   (tâche `generatePackIndex`) : l'énumération directe du classpath ne
   traverse ni les jar ni les dex de la même façon, l'index si ;
   personne ne tient la liste à la main.
3. **La compilation des grammaires reste dans la voie de grammaires**
   (`core-syntax-grammars`), derrière la couture
   `GrammarFactory` du module du bas : le chargeur compile au
   chargement par la fabrique injectée (`PackGrammars`), le refus est
   entier et précis. Pas de dépendance montante.
4. **Les extraits restent de la donnée validée** (`PackSnippet`) : la
   conversion en `Gabarit` troué se fera au câblage de la capacité
   correspondante (gabarits vivants), pas au chargement — le paquet
   ne connaît pas le moteur.
5. **`LanguageContributor` vit dans `core-language`** : le prompt le
   plaçait côté `android-host`, mais le cli (JVM) le charge par
   `ServiceLoader` — l'interface doit être visible des deux mondes ;
   l'enregistrement Android reste explicite, seul le cli découvre.
6. **L'enregistrement écrit se concentre** : `WrittenLanguages`
   (core-language, l'attache des six analyseurs dédiés) et
   `LangagesDemo` (le choix du serveur de la démo) — les deux seuls
   fichiers hors paquets autorisés à nommer des identifiants de
   langage, avec les façades `*Syntax` de `core-syntax` qui déclarent
   l'identifiant de leur langage (le SPI l'exige) ; le contrôle
   `check-language-registration` l'impose en CI. La règle
   `servers.json` des paquets remplacera la table de la démo au jalon
   LSP.
7. **La détection documente sa précédence** : réglage de
   l'utilisateur, nom exact, motif de nom, extension composée la plus
   longue, première ligne, indice de contenu. L'indice vit dans les
   données (`contentHint`, le `template <` du C++) ; sans indice qui
   parle, le candidat SANS indice l'emporte — il se déclare défaut de
   son extension (le `.h` sans marque C++ est du C) ; sans défaut ni
   indice, l'égalité reste ambiguë et l'interface peut la montrer.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Modèle + paquets dans `core-language`, grammaires injectées (proposition) | **retenue** | aucune dépendance montante, la donnée descend, la compilation reste à sa voie |
| B. Un module neuf pour l'assemblage des paquets | écartée | bouleverse la topologie de l'ADR-002 pour un service que le module du bas rend déjà ; à reconsidérer si les paquets grossissent |
| C. Les paquets dans `core-syntax-grammars` | écartée | les langages écrits (java, kotlin…) ne sont pas des grammaires ; le module de grammaires dépend du module de langages, pas l'inverse |

## Conséquences

- Ajouter un langage = un dossier de données (+ son analyseur écrit
  s'il en a un, enregistré une fois) ; l'index suit, le contrôle
  d'architecture surveille, rien d'autre ne bouge.
- `Language` (trois champs) et `LangagesGrammaires` disparaissent ;
  l'API de `core-language` change — rupture annoncée au changelog
  (pré-1.0, ADR-009).
- Les éléments reportés : conversion des extraits en gabarits vivants
  (avec la capacité), `servers.json` (avec le jalon LSP), la
  persistance du cache de paquets compilés côté stockage Android
  (avec le scénario de paquet jouet de la démo). `outline.json` et
  les plis par configuration sont arrivés avec le niveau 1 de
  l'analyse structurelle (étape 6, H-08) : les paquets portent leurs
  règles de plan, la configuration porte ses plis d'indentation.

## Actions

- [x] M7.6 : modèle, paquets, chargeur, couture `PackGrammars`,
      migration des seize langages et des consommateurs, contrôle
      `check-language-registration`.
