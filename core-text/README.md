# core-text — le modèle de texte

Le socle du moteur celeris : le texte, ses instantanés, ses
transactions et son annulation. Tout ce qui touche un caractère passe
par ici — en JVM pure, sans SDK Android (critère de sortie M1).

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-text:1.1.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-text.api`](../api/core-text.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | aucune |
| Bancs | 33 bancs, 427 méthodes `@Test` (relevé du 2026-11-12) ; mutation ≥ 80 % (ADR-010) |

## Ce qu'il porte

- **Le document** — `Document` (corde et index de lignes), `TextSession`
  (la session éditable), les instantanés immuables, `AnchoredRanges`
  (les plages ancrées qui suivent les éditions).
- **La transaction unique** — `Transaction` porte les éditions
  multi-sélections avec projection des positions (ADR-003) ;
  `UndoStack` annule et refait avec coalescence.
- **La recherche** — `FindReplace` et ses options, les occurrences de
  mots (`WordOccurrences`).
- **L'Unicode** — grammaîtes UAX #29 (`Grapheme`, `Word`), algorithme
  UAX #9 (`Bidi`, `BidiClassifier`, `BidiEmbeddings`), mots à bosses de
  chameau (`CamelWord`, A-04), encodages et BOM (`Utf8Codec`,
  `WideCodecs`), retour à la ligne souple (`SoftWrap`), projection des
  positions (`PositionProjector`).

## Exemple

```java
TextSession session = TextSession.of("class Exemple {}", System::nanoTime);
session.apply(Transaction.of(
        new TextEdit(6, "Exemple", "Essai")));
System.out.println(session.text());   // class Essai {}
session.undo();
System.out.println(session.text());   // class Exemple {}
```

Tout passe par la transaction unique : l'annulation et le refait
traversent le même plan, jamais un chemin parallèle (ADR-003).

## Bancs et cliquets

Fuzz jqwik à oracle `StringBuilder` (ADR-010) ; propriétés des
instantanés, des transactions et des sélections ; le seul module au
cliquet de mutation (`:core-text:mutationThreshold`, seuil 80 %).
Couverture JaCoCo ≥ 90 % de lignes, comme tous les modules purs.

## Pour aller plus loin

- [ADR-003](../docs/adr/003-modele-de-texte.md) — modèle de texte et transactions ;
  [ADR-010](../docs/adr/010-strategie-de-test.md) — stratégie de test.
- [USAGE.md](../USAGE.md) — guide d'intégration hôte ; les exemples du
  modèle de texte y vivent (§ 5).
