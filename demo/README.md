# demo — le terrain de démonstration

L'application Android qui prouve le moteur de bout en bout : la vue,
les popups, les serveurs de langage embarqués, les contrats IA, la
recherche projet, les ligatures. La CI la construit, la signe et
vérifie la signature à chaque poussée. Une application, jamais
publiée ; aucune permission réseau déclarée — sa neutralité par défaut
est structurelle (vie privée, [README](../README.md) racine).

## En bref

| | |
| --- | --- |
| Nature | application Android (`minSdk` 26, `compileSdk`/`targetSdk` 36 — ADR-011) |
| Publication | jamais publiée — l'APK debug se construit en CI (`:demo:assembleDebug`), signature vérifiée |
| Dépendances | `android-view`, `android-widgets`, `android-host`, les modules purs du cœur, `lsp-protocole` |
| Bancs | 23 bancs, 97 méthodes `@Test` (2026-11-11) — Robolectric sur JVM (ADR-013) |

## Ce qu'elle porte

- **Le catalogue de scénarios** — `ScenarioCatalog` et `Scenario` :
  chaque fonctionnalité prouvée vit en scénario rejouable (matrice de
  parité, catégories A à H).
- **Les surfaces** — `MainActivity` (le catalogue), `EditorActivity`
  (l'éditeur complet), `FeuilleReglages` (les réglages en feuille du
  bas, ADR-018) — plus aucun écran plein cadre, plus aucun dialogue
  système (chantier C12, inventaire consigné).
- **Les langages** — `LangagesDemo` (le registre préchargé hors fil),
  `PaquetJouet` (le paquet jouet H-04), `ServeurLangage` et
  `ServeurReference` (le serveur de langage de référence embarqué en
  processus, tubes mémoire), `FichiersExemple` (les fichiers
  d'exemple).
- **Les extras** — ligatures Fira Code 6.2 (OFL, attribuée au
  [`NOTICE`](../NOTICE)) derrière l'interrupteur « Ligatures » ; les
  popups de revue d'agent, le consentement IA relus à chaque demande.

## Bancs et cliquets

Tests Robolectric (ADR-013) : scénarios, surfaces, serveur embarqué,
consentement ; l'APK réduit par R8 se construit (les latences de
journalisation y se crient, AGENT.md). Checkstyle/PMD aux mêmes
plafonds que les modules purs (ADR-002/019), lint Android bloquant
sans baseline.

## Pour aller plus loin

- [ADR-018](../docs/adr/018-interface-tout-en-popups.md) — tout en
  popups ; [ADR-013](../docs/adr/013-banc-de-test-de-vue.md) — le banc
  de test de vue.
- [USAGE.md](../USAGE.md) — le guide d'intégration que la démo met en
  scène ; [docs/parite/MATRICE-PARITE.md](../docs/parite/MATRICE-PARITE.md)
  — la matrice servie par les scénarios.
