# B4 — Ce que l'AAR impose à l'hôte (vérifié le 2026-11-18)

Le chantier B4 vérifie ce que la consommation des artefacts Android
de celeris impose à l'application hôte : ressources, manifestes,
dépendances transitives. État vérifié au commit de ce document.

## 1. Ressources — préfixe `celeris_` partout

**Constat initial** : `android-view` n'a aucune ressource (aucun
répertoire `res/`), mais `android-host` en avait 35 (les deux tiers
en `values/strings.xml`, le reste en `values-fr/`) — **aucune ne
portait le préfixe** (`menu_plus`, `section_deplacement`,
`titre_notification`… : des noms génériques, exactement ceux qu'une
application hôte définit aussi — la collision de ressources Android
fusionne alors silencieusement la valeur de l'hôte dans le kit, ou
l'inverse).

**Corrigé** :

- les 35 noms portent désormais le préfixe `celeris_` (les deux
  fichiers `strings.xml`, FR inclus) ;
- la convention `celeris.android-library-conventions` pose
  `resourcePrefix = "celeris_"` : AGP **bloque à la compilation**
  toute future ressource des trois bibliothèques qui oublierait le
  préfixe ;
- la démonstration, qui redéfinissait `menu_plus` et
  `titre_notification` en doublon, lit désormais les ressources du
  kit par `jo.celeris.android.host.R` (référence qualifiée) et ses
  doublons sont retirés.

`android-widgets` n'a aucune ressource. **Aucune ressource
traverse l'API publique** : les identifiants `R.string.celeris_*`
ne sortent pas du kit (la démo y accède par le `R` du kit, pas par
des constantes exportées).

## 2. Manifestes — vides, et le rester

Les trois manifestes (`android-view`, `android-widgets`,
`android-host`) sont vides :

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

Aucune permission, aucun composant, aucun `uses-sdk` : la
bibliothèque n'impose rien au manifeste fusionné de l'hôte. La règle
à tenir : tout composant ou permission future doit être justifié en
ADR et documenté ici.

## 3. Dépendances transitives — la sortie consignée

`./gradlew :android-host:dependencies --configuration
releaseRuntimeClasspath` (inter-projets, après B4) :

```
releaseRuntimeClasspath - Runtime classpath of '/release'.
+--- project ':core-language'
|    +--- project ':core-text'
|    \--- project ':core-syntax'
|         \--- project ':core-text'
+--- project ':android-view'
|    +--- project ':core-ai'
|    |    +--- project ':core-language' (*)
|    |    +--- project ':core-syntax' (*)
|    |    \--- project ':core-text'
|    +--- project ':core-intelligence'
|    |    +--- project ':core-text'
|    |    +--- project ':core-syntax' (*)
|    |    \--- project ':core-language' (*)
|    +--- project ':core-language' (*)
|    \--- org.jetbrains.kotlin:kotlin-stdlib:2.2.10
|         \--- org.jetbrains:annotations:13.0
+--- project ':android-widgets'
|    +--- project ':core-intelligence' (*)
|    \--- org.jetbrains.kotlin:kotlin-stdlib:2.2.10 (*)
+--- project ':core-lsp'
|    \--- project ':core-language' (*)
+--- project ':core-intelligence' (*)
\--- org.jetbrains.kotlin:kotlin-stdlib:2.2.10 (*)
```

**Lecture** :

- **aucune dépendance AndroidX** — le constat du PROMPT tient :
  aucun `androidx.*` dans le classpath d'exécution ; l'hôte n'en
  reçoit aucune par celeris ;
- **`kotlin-stdlib:2.2.10` apparaît au premier niveau** :
  `dependencyInsight` le montre direct, sans parent — c'est
  **l'AGP 9.5.1 lui-même** qui pose le stdlib sur chaque
  bibliothèque Android (le support Kotlin intégré d'AGP). Ce n'est
  ni une fuite du dépôt ni une régression : aucun module celeris ne
  déclare de Kotlin (aucun fichier `.kt`, aucun plugin Kotlin, les
  modules purs ont un `runtimeClasspath` propre). Toute application
  hôte moderne a déjà ce stdlib sur son classpath (il vient avec
  toute bibliothèque construite par AGP 8.6+) ; il ne s'ajoute
  donc à rien pour un hôte réel. La découverte est consignée ici
  pour la vérification JitPack du tag final : le consommateur
  vierge de B5 voit le même stdlib, à l'identique ;
- les modules purs (`core-*`) n'exposent **aucune dépendance
  tierce** au runtime — leurs POM sont propres (vérifié en A2).

`lsp-protocole` (optionnel) est le seul module à porter des tiers
(`lsp4j`, `gson`, désucrage) — il reste hors du cas courant
(l'agrégateur `celeris` ne l'exige pas, USAGE § 3.0).

## 4. Ce que l'hôte doit prévoir (récapitulatif)

| Sujet | Impose | Détail |
| --- | --- | --- |
| Ressources | rien de nouveau | préfixe `celeris_` : collision impossible |
| Manifeste | rien | manifestes vides |
| Dépendances | `kotlin-stdlib` (AGP) | commun à toute bibliothèque AGP, déjà présent chez l'hôte |
| Permissions | aucune | aucune permission déclarée |
| Désucrage / R8 | seulement si LSP embarqué | USAGE § 3.0 et § 8, règles R8 embarquées (B2) |

Conclusion : l'AAR est neutre pour l'hôte — la seule constante à
tenir est le préfixe `celeris_` (bloquant par convention) et les
manifestes vides (à réifier par la revue de tout futur ajout).
