# core-ai — les contrats IA et l'API d'outils

Les contrats IA du cœur : texte fantôme, prédiction d'édition, revue
d'agent par blocs et API d'outils compatible MCP. Aucun transport ne
vit ici — aucun réseau, aucun processus : l'hôte branche ses modèles,
rien ne part sans son consentement explicite (ADR-012).

## En bref

| | |
| --- | --- |
| Nature | JVM pure — se construit et se teste sans SDK Android |
| Coordonnées | `jo.celeris:core-ai:1.1.0` (jar, sources, Javadoc, SBOM, sommes — ADR-011) |
| Manifeste d'API | [`api/core-ai.api`](../api/core-ai.api), vérifié à chaque `check` (ADR-009) |
| Dépendances | `core-text`, `core-syntax`, `core-language` |
| Bancs | 10 bancs, 60 méthodes `@Test` (relevé du 2026-11-12) |

## Ce qu'il porte

- **Les contrats** — `ModeleTexteFantome` et `SessionTexteFantome` (le
  texte fantôme), `ModelePrediction` (la prédiction d'édition,
  `AppliquerPrediction` l'applique), `RevueAgent` (la revue de
  modifications d'agent par blocs avec points de contrôle),
  `ConnecteurMcp` (l'API d'outils pour agents, compatible MCP, avec
  détection de conflit), `InstructionsAgent`, `EtiquetteAgent`.
- **La garde** — `GardeContexte` exécute la politique de consentement
  (rien ne sort sans elle), `ExclusionsContexte` borne ce qui se lit.
- **Les faux modèles** — `FauxModeleLocal` et `FauxModeleDistant`
  déterministes : la démonstration et les bancs ne parlent jamais à un
  vrai modèle.

## Exemple

```java
// Le contrat du texte fantôme : le modèle répond au récepteur, ou se
// tait — l'appelant ne bloque jamais (ADR-012).
ModeleTexteFantome modele = hote.modeleFantome();
modele.completer(requete, new ModeleTexteFantome.Recepteur() {
    @Override public void suggestion(SuggestionFantome s) {
        vue.setFantome(s);
    }
    @Override public void aucune() {
        vue.setFantome(null);
    }
});
```

## Bancs et cliquets

Bancs des faux modèles, de la latence fantôme, de la revue par blocs ;
le test « aucune requête sans consentement » est vert. Couverture
JaCoCo ≥ 90 % de lignes.

## Pour aller plus loin

- [ADR-012](../docs/adr/012-contrats-ia-agents.md) — contrats IA et
  outils d'agent ; vie privée au [README](../README.md) racine.
- [USAGE.md](../USAGE.md) — brancher vos modèles et votre consentement
  (§ 11).
