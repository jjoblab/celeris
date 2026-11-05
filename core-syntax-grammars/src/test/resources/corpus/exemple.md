# Corpus Markdown — documentation d'un éditeur embarquable

# Celeris

Un éditeur de code embarquable, **rendu Canvas** et *multi-curseurs*.

## Édition

Le texte vit dans un `coeur immutable` — chaque édition passe par une
transaction unique, la sélection suit.

- multi-curseurs tactiles
- annulation groupée
- recherche par expression rationnelle

1. ouvrir un très gros fichier
2. défiler sans à-coup
3. replier les déclarations

## Exemple de code

```java
class Exemple {
    void methode() {
    }
}
```

Un lien vers le [guide de migration](docs/MIGRATION.md) et une
citation :

> Le rendu n'alloue rien par image.

---

## Notes

Le ~~pire des cas~~ pire cas reste couvert par les budgets.
