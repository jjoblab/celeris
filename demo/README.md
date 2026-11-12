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
| Dépendances | `android-view`, `android-widgets`, `android-host`, les modules purs du cœur, `lsp-protocole`, `serveur-lsp-java` |
| Bancs | 22 bancs JVM, 86 méthodes `@Test`, plus les 4 bancs d'appareil (relevé du 2026-11-12) — Robolectric sur JVM (ADR-013) |

## Ce qu'elle porte

- **Le catalogue de scénarios** — `ScenarioCatalog` et `Scenario` :
  chaque fonctionnalité prouvée vit en scénario rejouable (matrice de
  parité, catégories A à H).
- **Les surfaces** — `MainActivity` (le catalogue), `EditorActivity`
  (l'éditeur complet : la toolbar porte son débordement dans la CARTE
  du kit, la barre de touches de code vit au-dessus du clavier —
  reprise du 2026-11-12), `FeuilleReglages` (les réglages en feuille
  du bas, ADR-018) — plus aucun écran plein cadre, plus aucun dialogue
  système (chantier C12, inventaire consigné).
- **Les langages** — `LangagesDemo` (le registre préchargé hors fil),
  `PaquetJouet` (le paquet jouet H-04), `ServeurLangage` et
  `ServeurReference` (le serveur de langage de référence embarqué en
  processus, tubes mémoire), `DeclaredServers` (le montage PAR
  DÉSIGNATION des serveurs déclarés en données — le mini serveur Java
  de `serveurs/servers.json` s'éprouve entier : complétion, survol,
  signature, symboles, plis, formatage, diagnostics — H-13),
  `FichiersExemple` (les fichiers d'exemple).
- **Les extras** — ligatures Fira Code 6.2 (OFL, attribuée au
  [`NOTICE`](../NOTICE)) derrière l'interrupteur « Ligatures » ; les
  popups de revue d'agent, le consentement IA relus à chaque demande.

## Exemple

Le serveur déclaré de la démo — chaque capacité annoncée se prouve à
l'ouverture (BancSpecs, scénario H-13) :

```json
{ "servers": [ { "language": "java", "transport": "embedded",
    "serverClass": "jo.celeris.serveur.java.JavaLanguageServer" } ] }
```

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
