# ADR-0028 — Registre des descripteurs d'abord, grammaires à la demande

- **Statut** : Acceptée (chantier A de la coloration — prompt du
  2026-11-22, piste F9 de l'audit ; mesure avant/après au banc JVM,
  § « Chiffres »)
- **Date** : 2026-11-22

## Contexte

Le retard de plusieurs secondes de la coloration au lancement (le
symptôme du prompt : texte blanc puis couleur tardive, dans `sample`
ET chez un hôte consommateur) a une cause dominante mesurée au
lancement de la session du 2026-11-22 : l'assemblage du registre de
langues compile les 27 grammaires TextMate livrées AVANT de publier le
moindre langage. La résolution de « la langue de ce fichier » attend
la fin de TOUT — 679 à 835 ms à froid sur JVM de bureau 2 CPU (banc
`MesuresChargementAvant`, rapport du 2026-11-22), des secondes sur
appareil lent (le retour terrain du 2026-11-19 parlait de 30 s).

La vague 2.2.12-2.2.14 avait déjà retiré les autres causes mesurées
(retenue de 250 ms à l'ouverture, créneau unique, re-tokenisation).
Il reste CELLE-CI : la langue du document ouvert patiente les
grammaires des langages qui ne le concernent pas.

## Décision

**Le registre s'assemble des descripteurs seuls ; la grammaire d'un
langage se compile à sa première demande.**

1. `PackLoader.loadSummary(PackFiles)` charge le paquet SANS compiler
   sa grammaire (lecture, validation, empreinte identiques à `load`) ;
   le champ grammaire du `LanguagePack` devient une `SyntaxeParesseuse`
   (paquet `packs`, interne) : compilée au premier `lexer()` /
   `parser()` / `reservedWords()`, UNE fois, mémorisée ; un fil à la
   fois compile, les autres fils en vol servent le repli écrit déclaré
   (ou null : vue sans couleur, le rattrapage recolore à l'arrivée) ;
   une compilation défaillante se mémorise AUSSI (défaillance
   définitive, jamais de boucle de compilation sur le fil interface).
2. `LanguagePlatform.addPackSourceLentement` : la voie d'assemblage sans
   compilation ; `LanguageKit.assembleOnThread` l'emprunte. Le contrat
   public `addPackSource` (compilant) ne change pas.
3. Le compte à rebours de l'assemblage se libère APRÈS la pose du
   registre et AVANT la précompilation de fond : la résolution du
   document ouvert lit le registre VIVANT sans attendre le moindre
   reste. La précompilation (`precompiler`) prépare ensuite les
   grammaires des autres langues sur le même exécuteur : les fichiers
   suivants n'attendront pas leur première vue.
4. Le tri prioritaire de l'étape intermédiaire (descripteurs lus pour
   ordonner la file, `LanguageDetectionRules`) part : rendu inutile
   par la paresse — le registre complet se pose SANS aucune
   compilation, l'ordre n'a plus d'importance.

## Chiffres (banc JVM 17, 2 CPU, grammaires réelles du dépôt)

| Mesure | Avant | Après |
| --- | --- | --- |
| Temps jusqu'à la langue du 1er fichier (froid) | 679-835 ms | 167-194 ms |
| Compilations au moment de la langue | 27 | 0 |
| Grammaire la plus lourde (typescript, 231 Ko) dans le chemin | oui | 26 ms à part |
| Registre prêt (descripteurs des 33 paquets) | après tout | ~130 ms |

Bancs : `LanguageKitPrioriteTest` 6/6 (résolution sans compilation,
première demande mémorisée, fond après la pose, sonde honnête, support
direct servi, ordre du compte à rebours) ; non-régression
`:core-language:test` 164/164, `:core-syntax-grammars:test` vert,
`:core-language:check` vert (Checkstyle/PMD/ErrorProne/NullAway/api).

## Conséquences

- L'API publique gagne `PackLoader.loadSummary` et
  `LanguagePlatform.addPackSourceLentement` (additifs) ; le manifeste
  `api/core-language.api` est régénéré.
- La préférence « grammar » d'un paquet se résout à la première
  demande du support, pas à l'assemblage : un paquet fautif ne jette
  plus jamais l'assemblage (sa grammaire défaillante se mémorise comme
  absente).
- Sur appareil : le gain se transpose au facteur près (les compilations
  disparaissent DU CHEMIN — le protocole de mesure appareil vit dans
  le rapport de session du 2026-11-22, § 6).
