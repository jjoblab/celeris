# ADR-0029 — Cache d'échecs et matchers réutilisés dans le lexeur de grammaires

- **Statut** : Acceptée (chantier B de la coloration — prompt du
  2026-11-22, piste F8 de l'audit ; mesures avant/après au banc JVM
  intercalé, § « Chiffres »)
- **Date** : 2026-11-22

## Contexte

L'audit coloration (F8) chiffrait le lexeur de grammaires : python
27,1 µs/ligne, javascript 296,1 µs/ligne contre 4,3 µs/ligne au lexeur
écrit — l'écart venait du modèle de recherche : à CHAQUE position de
la ligne, CHAQUE règle du contexte paie une recherche joni complète
(un `Regex.matcher(octets)` alloué, un `search(pos, fin)`), même quand
la même règle vient d'échouer au caractère précédent. Le modèle de
référence tm4e (`JoniOnigRegExp.search`) mémorise le résultat par
expression pour la ligne en cours.

## Décision

**Deux caches par ligne dans `GrammarLexer`, remis à vide à chaque
ligne :**

1. **Le cache d'échecs** (`IdentityHashMap<Regex, Integer>`) : une
   expression qui a échoué depuis p a LU toute la fin de ligne sans
   rien trouver — la re-tentative depuis toute position SUPÉRIEURE OU
   ÉGALE rend le même échec sans relire (un sous-ensemble du vide
   reste vide). Le cas réel : le lexeur avance d'un caractère et
   re-visite toutes les règles du contexte — chacune qui vient
   d'échouer se sert du néant mémorisé. (Le sens du test importe : le
   néant couvre [p, fin], toute position postérieure est incluse.)
2. **Les matchers réutilisés** (`IdentityHashMap<Regex, Matcher>`) : le
   `Matcher` joni se bâtit une fois par expression et par ligne, les
   recherches suivantes le réemploient — l'allocation d'un Matcher par
   recherche était un coût mesuré en isolement (50 recherches : 76,5 µs
   allouées contre 13,9 µs réutilisées).

Un cache de CORRESPONDANCE mémorisée (la prochaine occurrence) a été
essayé puis RETIRÉ : une occurrence trouvée depuis p' ne peut pas se
servir à une position antérieure (une occurrence plus ancienne peut
vivre entre les deux), et le lexeur ne re-cherche jamais deux fois à
la même position — le cache ne rapportait rien pour un risque de
faute de coloration.

## Chiffres (banc JVM intercalé, médiane de 6 tours, grammaires réelles)

| Langue | Avant | Après | Gain |
| --- | --- | --- | --- |
| python | 53,0 µs/ligne | 45,0 µs/ligne | ~15 % |
| javascript | 194,3 µs/ligne | 123,2 µs/ligne | ~37 % |
| csharp | 57,5 µs/ligne | 44,0 µs/ligne | ~23 % |
| css | 32,9 µs/ligne | 32,0 µs/ligne | ~3 % |
| html | 41,6 µs/ligne | 42,9 µs/ligne | ~0 (bruit) |

Le compteur d'essais observe le contrat : une ligne de 3 caractères
sans correspondance paie 4 essais (une par règle) contre 12 sans cache
(4 règles × 3 positions).

## Conséquences

- Aucun changement d'API publique (les caches sont des champs privés
  du lexeur ; `essaisDerniereLigne()` est de visibilité paquet pour
  le banc).
- La couleur ne change pas (les 158 tests du module le prouvent,
  bancs d'intégration des paquets livrés compris).
- La ligne reste l'unité de vie des caches : aucune invalidation
  croisée, aucune fuite (les maps se vident à chaque ligne).
