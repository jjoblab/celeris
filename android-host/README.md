# android-host — les adaptateurs hôte (SPI) et l'orchestration des popups

Les coutures entre l'éditeur et son hôte : système de fichiers virtuel,
intégration VCS, exécution, débogueur, analyseurs de problèmes, de pile
et de lint, historique local. L'éditeur ne connaît que les interfaces —
l'hôte fournit les implémentations, le module en porte des références
pures (mémoire, faux) pour la démonstration et les bancs.

Depuis le déménagement du 2026-11-14 (décision du propriétaire : les
fonctionnalités vivent dans les modules de l'éditeur, la démonstration
ne teste plus qu'elle-même), le module porte aussi **l'orchestration
des popups** : `PopupsEditeur` — la classe unique qui branche la vue,
la session et le kit, pour un hôte qui n'a presque rien à écrire.

## En bref

| | |
| --- | --- |
| Nature | bibliothèque Android (`minSdk` 26, `compileSdk` 36 — ADR-011) |
| Coordonnées | `jo.celeris:android-host:1.1.0` (AAR de la variante release, sources, Javadoc, SBOM, sommes — depuis le 2026-11-11) |
| Manifeste d'API | pas de manifeste (ADR-009 couvre les modules purs) — l'API publique vit dans `src/main/java` |
| Dépendances | `core-language`, `core-lsp`, `android-view`, `android-widgets` (l'étage de composition de l'hôte — le déménagement du 2026-11-14) |
| Bancs | 12 bancs, 71 méthodes `@Test` (relevé du 2026-11-14) |

## Ce qu'il porte

- **Les intégrations** — `IntegrationVcs` (marques et révisions,
  `MarqueVcs`, `AnnotationBlame`), `IntegrationExecution`
  (`Lancement`, `EtatExecution`, `AuditeurExecution`),
  `IntegrationDebogueur` (`LigneArret`, `Arret`, `CadrePile`,
  `ValeurEnLigne`, `AnalyseurPile`).
- **Les documents** — `SystemeFichiersVirtuel`,
  `ArbreDocuments`, `ArbreDocumentsSaf` (l' accès aux documents de
  l'hôte), `AdaptateurStockageDocuments`, `CheminDoc`,
  `EntreeDocument`.
- **Les analyseurs** — `AnalyseurProblemes` (problèmes du projet),
  `AnalyseurLint` et `CorrectifsLint` (diagnostics et correctifs de
  l'hôte), `DiagnosticHote`, `GraviteDiagnostic`.
- **L'historique local (A-14)** — `HistoriqueLocal` archive le contenu
  remplacé à chaque écriture, isole l'histoire par fichier et restaure
  une version antérieure qui se défait comme elle s'est faite.
- **Les références pures** — `MemoireSystemeFichiers`,
  `FauxVcs`, `FauxExecution`, `FauxDebogueur`,
  `RevisionLocale` : des implémentations de référence complètes, sans
  appareil — la démonstration et les bancs tournent dessus.
- **L'orchestration des popups (déménagement du 2026-11-14)** — le
  paquet `jo.celeris.android.host.popups` :
  `PopupsEditeur` (l'orchestrateur : écouteurs, cartes, cascades — le
  montage complet de l'hôte tient en sa construction), `CoachPopups`
  (les tables du repli local, muet par défaut),
  `ContenuPopups` (les algorithmes de contenu pur), `LanguageSession`
  (la surface commune des serveurs de langage, modèles déjà
  convertis), `DocAncre` (survol, aperçu de définition, `@Preview`),
  `BordDroit` (bande d'erreurs, minimap, pouce), `FilAriane`,
  `SelecteurCouleur`, `PalettesPopups` (le verre dérivé du thème).

## Exemple

```java
// Les popups de l'éditeur tiennent en UNE construction (§ 9 d'USAGE.md).
PopupsEditeur popups = new PopupsEditeur(editeur, session, couche,
        theme, tailleTexteSp, CoachPopups.muet());
popups.brancherServeur(serveur, "file:///Main.java", langage);

// L'éditeur ne connaît que l'interface — l'hôte implémente.
public final class MonVcs implements IntegrationVcs {
    @Override
    public List<AnnotationBlame> blame(CheminDoc fichier) {
        return List.of();   // l'annotation par ligne, ou rien
    }
}
```

## Bancs et cliquets

Bancs des implémentations de référence (mémoire, faux), de l'historique
local, de l'accès aux documents et des algorithmes de contenu des
popups (`ContenuPopupsTest`). Checkstyle/PMD aux mêmes plafonds
que les modules purs (ADR-002/019), lint Android bloquant sans
baseline.

## Pour aller plus loin

- [ADR-002](../docs/adr/002-topologie-des-modules.md) — topologie et
  règles de dépendance.
- [USAGE.md](../USAGE.md) — les popups et feuilles chez l'hôte (§ 9) :
  l'orchestrateur prêt à l'emploi ; le SPI hôte (§ 10).
