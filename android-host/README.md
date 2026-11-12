# android-host — les adaptateurs hôte (SPI)

Les coutures entre l'éditeur et son hôte : système de fichiers virtuel,
intégration VCS, exécution, débogueur, analyseurs de problèmes, de pile
et de lint, historique local. L'éditeur ne connaît que les interfaces —
l'hôte fournit les implémentations, le module en porte des références
pures (mémoire, faux) pour la démonstration et les bancs.

## En bref

| | |
| --- | --- |
| Nature | bibliothèque Android (`minSdk` 26, `compileSdk` 36 — ADR-011) |
| Coordonnées | `jo.celeris:android-host:1.1.0` (AAR de la variante release, sources, Javadoc, SBOM, sommes — depuis le 2026-11-11) |
| Manifeste d'API | pas de manifeste (ADR-009 couvre les modules purs) — l'API publique vit dans `src/main/java` |
| Dépendances | `core-language` |
| Bancs | 10 bancs, 59 méthodes `@Test` (relevé du 2026-11-12) |

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

## Exemple

```java
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
local et de l'accès aux documents. Checkstyle/PMD aux mêmes plafonds
que les modules purs (ADR-002/019), lint Android bloquant sans
baseline.

## Pour aller plus loin

- [ADR-002](../docs/adr/002-topologie-des-modules.md) — topologie et
  règles de dépendance.
- [USAGE.md](../USAGE.md) — le SPI hôte : VCS, exécution, débogueur,
  fichiers (§ 12).
