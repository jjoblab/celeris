# ADR-014 — Moteur d'expressions rationnelles des grammaires : joni

- **Statut** : Proposition (spike exécuté le 2026-11-08 — les chiffres sont mesurés, la validation reste au propriétaire)
- **Date** : 2026-11-08

## Contexte

Les grammaires TextMate supposent Oniguruma. Le moteur de `core-syntax-grammars`
(voie héritée du dépôt de départ) passe aujourd'hui par `java.util.regex`, qui
en diffère sur des points que les grammaires réelles utilisent. Le prompt M7.6
exige de charger **sans modification** les grammaires telles que les extensions
de VS Code les publient, et tranche par spike chiffré entre deux voies :

- (a) une couche de traduction vers `java.util.regex`, échouant **au
  chargement** avec un message précis sur toute construction non traduite ;
- (b) joni, le portage Java d'Oniguruma (jruby/joni, licence MIT).

Le spike (`spikes/regex/SpikeRegex.java`, résultats bruts dans
`spikes/regex/results/`) mesure les onze grammaires réelles des extensions de
VS Code (json, yaml, python, shell, c, cpp, markdown, html, css, javascript,
typescript — 3 700 expressions extraites de match/begin/end/while), avec la
couche de traduction écrite pour le spike (\h, classes POSIX, \x{16 bits},
refus bruyant de (?x, (?~, conditionnels, \R).

## Résultats mesurés

| Mesure | Traduction vers java.util.regex | joni 2.2.9 (+ jcodings 1.0.64) |
| --- | --- | --- |
| Motifs compilés sans échec | 3 349 / 3 700 (91 %) | 3 670 / 3 700 (99 %) |
| Refus dominants | 234 × mode étendu (?x ; 27 × répétitions illégales ; ~20 × séquences d'échappement non supportées ; 4 × lookbehind sans longueur max évidente) | 30 refus (motifs réellement invalides que l'éditeur de référence tolère aussi) |
| **Réécritures silencieuses** | **122 motifs** : `nested repeat operator '+' and '?' was replaced with '*'` — le JDK compile en CHANGEANT la sémantique des quantificateurs possessifs, sans échec | aucune (joni implante la sémantique Oniguruma, possessifs compris : 405 quantificateurs possessifs et 19 groupes atomiques dans le corpus) |
| Temps, 40 motifs × 50 000 lignes (min de 3 tours, corpus identique) | 50 à 2 778 ms selon la grammaire | 73 à 3 304 ms — rapport selon la grammaire entre 0,46× (json : joni deux fois plus rapide) et 2,56× (yaml) ; jamais un facteur disqualifiant |
| Jetons trouvés | diffère de joni sur 6 grammaires de 11 (sémantique \w, possessifs) | correspondance de référence |
| Tas consommé par une passe (motifs compilés) | 0,8 Mo pour 1,1 Mo de texte | comparable |
| Android (statique : d8 + dexdump, API 26) | — | dex de 615 Ko, **546 méthodes définies**, zéro référence à sun.misc.Unsafe ou jdk.internal ; licences MIT (POM vérifiés) |

Le taux utile de la voie (a) n'est pas 91 % : il faut lui retirer les 122
motifs compilés avec une sémantique modifiée — la coloration divergerait de
la référence sans qu'aucun échec ne le signale. La fidélité réelle tombe à
87 %, et la divergence est **invisible**, ce qui est pire qu'un refus.

## Décision

**Voie (b) : joni** (version 2.2.9, jcodings 1.0.64, épinglées au catalogue).

1. `core-syntax-grammars` compile ses expressions avec joni ; la sémantique
   de référence est celle d'Oniguruma (le format TextMate la suppose —
   1 223 lookbehinds, 405 possessifs et 68 classes POSIX dans le seul corpus
   du spike).
2. La couche de traduction du spike est conservée comme **outillage de
   diagnostic** : elle nomme précisément les constructions exotiques d'un
   paquet utilisateur ; elle ne sert plus au moteur.
3. **Positions en octets** : joni travaille sur des octets UTF-8 ; le moteur
   de grammaires transcode par ligne (ADR-003 : UTF-16 en interne) et la
   conversion est testée sur des lignes non-ASCII (le corpus du spike était
   ASCII, l'écart est consigné comme coût d'intégration).
4. **Bornage** (exigence du prompt, conservée) : la validation d'un paquet
   rejette les formes catastrophiques visibles (quantificateurs imbriqués non
   bornés), et l'analyse s'exécute sous jeton d'annulation avec budget par
   ligne — au dépassement, la ligne garde ses couleurs précédentes et
   l'incident est journalisé, jamais un gel de l'interface.
5. La montée de version de joni passe par le catalogue et les tests de
   conformité du banc de grammaires (le taux d'acceptation par grammaire
   devient un seuil de CI).

## Options

| Option | Verdict | Raison mesurée |
| --- | --- | --- |
| a. Traduction vers java.util.regex | écartée | 13 % des motifs réels refusés ou réécrits en silence ; le mode étendu (?x) à lui seul en rejette 234 ; la divergence silencieuse contredit le critère « coïncider avec la référence » |
| b. joni | **retenue** | 99 % d'acceptation sur les grammaires du commerce, sémantique Oniguruma par construction, perf comparable, 615 Ko / 546 méthodes sur Android, MIT |
| c. Moteur à écrire en entier | écartée | réécrire Oniguruma dépasse le mandat ; le portage existe et se mesure |

## Compromis

- jcodings porte une famille d'encodages dont un seul sert (UTF-8) : le dex
  mesure 615 Ko avant réduction — R8 élaguera les encodages inutilisés si
  les règles de conservation le permettent, sinon l'excès est documenté et
  assumé (moins de 1 % de l'APK de démonstration).
- Le rapport de vitesse défavorable sur certaines grammaires (yaml 2,56×)
  se paie en temps d'analyse : le moteur limite les règles actives par
  position et l'incrémental borne la replanification — le budget ADR-004
  (re-analyse ≤ 10 ms) reste le seuil de CI du moteur de grammaires.
- Une dépendance de plus entre dans `core-syntax-grammars` (aujourd'hui
  zéro dépendance du cœur) : elle y entre par ADR, comme l'exige la règle 5,
  avec SBOM, sommes de contrôle et NOTICE mis à jour.

## Conséquences

- `gradle/libs.versions.toml` porte `joni` et `jcodings` ; `NOTICE` et
  `docs/REFERENCES.md` consignent licences et provenance (fait pour
  REFERENCES.md, reste le catalogue à l'intégration du moteur).
- Le banc de conformité (instantanés de portées par grammaire livrée) rejoue
  chaque grammaire intégrée et son taux d'acceptation.
- `java.util.regex` reste au moteur pour rien : aucun appel direct dans
  `core-syntax-grammars` une fois la migration faite.

## Actions

- [ ] M7.6 moteur de grammaires : intégrer joni, transcodage UTF-8 par
      ligne, bornage par ligne, tests non-ASCII.
- [ ] M7.6 : banc de conformité — taux d'acceptation par grammaire comme
      seuil bloquant.
- [ ] M7.6 : catalogue, SBOM, NOTICE, empreintes (règle 5).
