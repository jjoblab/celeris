# Revue de sécurité — M9 (2026-11-10)

> **Déclenchement** : le jalon M9 (feuille de route) — « Revue de
> sécurité ».
>
> **État** : la revue est exécutée. UNE faille réelle trouvée et fermée
> (montage réflexif par déclaration de paquet), un durcissement de
> manifeste (sauvegarde nuage interdite), tous les garde-fous
> existants vérifiés un à un avec leurs tests.

## Méthode

La revue passe en revue chaque surface d'attaque du dépôt : le code
qui charge de la donnée non fiable (paquets de langage, déclarations
de serveurs), la frontière IA (consentement, exclusions), le
manifeste et les permissions, la provenance des dépendances, les
secrets, la journalisation, la réduction R8. Pour chaque constat :
le fichier, le mécanisme, le test qui le prouve, la limite qui
reste.

## 1. La faille trouvée — et fermée (rouge puis vert)

**Le montage réflexif par déclaration de PAQUET.** La voie des
déclarations (H-13, `servers.json`) fusionne les déclarations des
paquets et celles de l'hôte (`LanguagePlatform.serveursDe` :
`core-language/.../LanguagePlatform.java`) ; la démonstration
montait la première déclaration EMBEDDED DISPONIBLE — quelle que
soit sa SOURCE (`DeclaredServers.open`). Le montage s'exécute par
réflexion : `Class.forName(...).getDeclaredConstructor().newInstance()`
(`lsp-protocole/.../ServeurEmbarque.java`). Un paquet de langage
copié par l'utilisateur (dossier ou archive, H-04) pouvait donc
déclarer un serveur embarqué de classe ARBITRAIRE — le constructeur
vide de n'importe quelle classe du chemin de classes s'exécutait
dans le processus de l'hôte.

**Preuve du rouge** : le test `uneDeclarationDePaquetNeMonteJamaisSansLhote`
(`DeclaredServersTest`) déclare un support de langage fictif portant
une déclaration embedded vers une classe RÉELLE du chemin de classes
— sans le correctif, la session montée était bien celle du serveur
déclaré par le paquet (`DeclaredServerSession`, pas le repli).

**Le correctif** : l'hôte ne monte que SES déclarations —
`DeclaredServers.open` ne considère que les déclarations ÉGALES à
celles de son propre `serveurs/servers.json` ; la donnée du paquet
voyage toujours jusqu'à l'hôte (H-13 inchangé : `serverDeclarations()`
porte tout), mais la DÉCISION de monter reste à l'hôte qui assemble
le registre — exactement la règle du propriétaire : « l'éditeur ne
connaît pas les serveurs de ses hôtes, il porte la déclaration
jusqu'à eux ». Un paquet malveillant ne choisit plus quelle classe
s'exécute dans le processus. Le test repasse vert.

## 2. Le chargeur de paquets — donnée non fiable, portes closes

`PackLoader` et `Packs` (`core-language`) traitent tout paquet copié
comme de la donnée non fiable :

- **zip-slip refusé** : toute entrée d'archive qui remonte (`/`
  initial ou `..`) rejette le paquet avec message précis
  (`Packs.ArchivePacks`) ;
- **liens symboliques refusés** : au dossier du paquet comme à
  chaque lecture de fichier (`Packs.FolderPacks`) ;
- **tailles bornées** : un million d'octets par fichier, quatre
  millions par paquet (`PackLoader.MAX_FILE_BYTES`,
  `MAX_PACK_BYTES`) ;
- **profondeur bornée** et **schéma validé** (`schemaVersion`),
  l'entrée `servers.json` doit servir LE langage du paquet — une
  entrée qui en sert un autre se refuse ;
- la grammaire passe par la fabrique injectée, qui compile avec son
  bornage d'exécution propre (budget d'essais par ligne, garde
  catastrophique — ADR-014 : jamais un gel, jamais une forme qui
  boucle).

**Preuve** : `PackLoaderTest` — schéma invalide, tailles et
profondeur hors bornes, zip qui remonte, lien symbolique, chacun
refusé avec son message ; `GrammairesBudgetTest`,
`GrammaireBornageTest` côté exécution.

## 3. La frontière IA — consentement, exclusions, preuve structurelle

- **Le garde** (`core-ai/GardeContexte`) consulte le consentement à
  CHAQUE départ (relu, jamais figé) ; sans consentement, la requête
  reste au sol, fantôme et prédiction compris.
- **Les exclusions** (`ExclusionsContexte`) : chemin exact, dossier,
  joker — le chemin exclu ne produit AUCUN départ (pas de requête,
  pas de réponse), le journal du faux modèle en témoigne.
- **La neutralité structurelle** : le manifeste de la démonstration
  ne déclare AUCUNE permission — pas même INTERNET (`DemoNeutreTest`
  le lit et l'exige) ; un paquet sans prise ne peut pas en ouvrir.
- **Aucune télémétrie** embarquée dans les paquets celeris (README,
  « Vie privée ») — l'option reste à l'hôte, derrière consentement.

## 4. Le manifeste — durci

La revue relève que le manifeste ne disait rien de la sauvegarde
nuage : `allowBackup` vaut VRAI par défaut — préférences et fichiers
de l'application partiraient au transport Android. Corrigé :
`android:allowBackup="false"`, exigé désormais par
`DemoNeutreTest.leManifesteInterditLaSauvegardeNuage`. Les trois
activités ne s'exportent pas (hors le lanceur), aucune permission,
aucun fournisseur de contenu, aucun récepteur.

## 5. La provenance et les références interdites

- `scripts/check-dependency-provenance` (CI bloquante, auto-testée) :
  catalogue de versions, métadonnées de vérification, URL de
  dépôts — aucun artefact publié par les organisations interdites
  n'entre dans le build.
- `scripts/check-forbidden-refs` (CI bloquante, auto-testée) : les
  motifs nominatifs vivent dans le secret de CI
  `CELERIS_FORBIDDEN_REFS` (jamais committés), le balayage lit les
  fichiers suivis ET les non suivis non ignorés.
- La CI (`.github/workflows/ci.yml`) épingle ses actions par somme
  SHA, valide le wrapper, `permissions: contents: read`, les secrets
  passés sont exactement `CELERIS_FORBIDDEN_REFS`.

## 6. Les licences tierces

Le `NOTICE` consigne les dépendances de RUNTIME des artefacts
publiés (joni MIT — ADR-014 ; LSP4J EPL-2.0 OU BSD-3-Clause, Gson
EPL-2.0 — ADR-015) et l'ubérifié découpé de la démonstration
(serveur-retire EPL-2.0, découpe reproductible par
`scripts/decoupe-serveur-retire`) ; les grammaires du commerce vivent
aux ressources de TEST, hors artefacts (licences MIT vérifiées,
sommes consignées). `docs/REFERENCES.md` tient les projets et normes
cités avec licences vérifiées en ligne.

## 7. La journalisation — rien ne fuit

Les modules purs journalisent par `java.util.logging` (l'API du JDK
présente des deux côtés — constat R8 de M7.6). Le balayage de la
revue ne trouve AUCUN `System.out`, aucun `printStackTrace`, aucune
chaîne de contenu de document journalisée : le seul avertissement de
budget du lexeur dit la LONGUEUR de la ligne
(`GrammaireLexeur` : « une ligne de N caractères »), jamais son
texte. Les diagnostics, erreurs et incidents portent des positions
et des noms de règles.

## 8. La réflexion et la réduction

La réflexion vivante est documentée et bornée : le montage des
serveurs embarqués (ci-dessus, corrigé côté décision), le rouage
JSON de la bibliothèque du protocole (gardé par les règles
CONSOMMATEUR de `lsp-protocole/regles-conservation.pro`, jointes à
tout assemblage réduit — ADR-015). Aucun `Class.forName` ailleurs
dans le dépôt ne prend son argument de la donnée non fiable — le
 seul autre emploi (`DeclaredServers.disponible`) ne CHARGE pas la
classe (`initialize=false`, aucun exécutable), il teste sa présence,
et ne s'applique plus qu'aux déclarations de l'hôte.

## 9. Les secrets

Balayage du dépôt versionné : aucun jeton, aucune clef, aucun mot de
passe. Les trois correspondances du motif sont des faux positifs
vérifiés : deux chaînes de banc de test (du TEXTE de code factice
dans `DemoIntelligenceTest` et `BancSpecs` — le banc des exclusions
DOIT écrire le mot « secret » dans du texte d'essai) et le
`REGISTRY_TOKEN` des conventions de publication, lu de
l'ENVIRONNEMENT (`System.getenv`), jamais en dur. `local.properties`
est ignoré par construction (`AGENT.md`, « à ne pas faire »).

## 10. L'archive du serveur tiers retiré

`demo/libs/serveur-retire.jar` (6 Mo) est versionnée — la
découpe est reproductible au octet près par le script, le NOTICE
consigne identité, licence et provenance. La revue note la limite :
le jar versionné n'est pas vérifié par somme à la construction (le
script produit le même fichier, mais rien ne rejoue la comparaison
en CI) — risque d'acceptabilité faible (le fichier vit dans le
dépôt, la reproductibilité prouvée), consigné comme telle.

## Constats résiduels, acceptés

- **Aucun appareil réel** : la revue est statique et JVM ; le
  comportement sur vitre (IME, partitions, astuce de fabricant)
  reste au plan du propriétaire (`VERIFICATION-APPAREIL.md`).
- **La stdio et tcp déclarés, non montés** : la déclaration les
  porte (données pures), aucun hôte du dépôt ne les monte — un hôte
  futur qui les branchera devra traiter la commande et la prise
  comme des surfaces non fiables (injection d'arguments, prise
  locale contre distante) ; la frontière est documentée dans
  `LanguageServerDeclaration`.
- **Le SPI android-host** manipule des contrats (fichiers virtuels,
  VCS, débogueur, exécution) : les implémentations de référence
  (`MemoireSystemeFichiers`, `FauxVcs`, `FauxDebogueur`,
  `FauxExecution`) ne touchent ni réseau ni stockage — la surface
  réelle appartiendra aux hôtes.
