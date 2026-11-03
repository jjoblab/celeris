# ADR-006 — Rendu et budgets de performance

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

Le dépôt de départ prouve la viabilité du rendu Canvas une vue (acquis :
cache de rendu par ligne, ponts IME par niveau d'API, mesure injectée) mais
concentre aussi ses pires pièges : pinceau de texte partagé et muté en cours
d'image (PIEGE-01), clé de cache dépendant d'un état mutable (PIEGE-02),
confusion rangées visuelles / lignes document (PIEGE-04), allocations par
image (PIEGE-15). Le spike de rendu (proxy JVM, `docs/audit/SPIKES.md`)
mesure le rapport création/consultation d'une mise en page de ligne à
~10^5 : le cache par rangée est une condition d'existence.

Les budgets ci-dessous sont les **cibles initiales du mandat** ; celles qui
concernent le rendu sont des proxy JVM tant que l'appareil de référence
n'a pas calibré (M1) — le mandat exige cette calibration avant M3.

## Décision proposée

1. **Une seule vue Canvas** ; pipeline à couches (texte, chrome, popups)
   au tracé par image, chaque couche possédant ses pinceaux (PIEGE-01
   interdit par revue et par test).
2. **Zéro allocation par image en régime établi** : objets de travail
   réutilisés (transmis en paramètre), aucune collection intermédiaire dans
   le chemin de dessin (PIEGE-15 vérifié par test d'allocations).
3. **Caches indexés par valeurs immuables** (identifiant d'instantané +
   numéro de rangée + thème + police), jamais par état mutable (PIEGE-02).
4. **Invalidation par plages de rangées** ; pré-calcul asynchrone des mises
   en page hors fil UI avec jeton d'annulation ; la vue dessine ce qui est
   prêt et retombe sur le contenu périmé le temps d'une image.
5. **Instrumentation** : `FrameMetrics` par scénario de la démo, JankStats
   en régime de défilement, budgets consignés dans la matrice de parité.
6. Budgets initiaux (calibrés sur l'appareil de référence à M1, cliquet en
   CI ensuite) :

| Mesure | Cible initiale | Outil |
| --- | --- | --- |
| Frappe jusqu'au pixel, p95 | 16 ms (8 ms à 120 Hz) | macrobenchmark + `FrameMetrics` |
| Édition sur le modèle, 50 000 lignes | 1 ms | JMH — déjà mesuré à 0,002 ms (rope seule) |
| Ouverture de 100 000 lignes jusqu'à la première image | 500 ms | macrobenchmark |
| Défilement | 99 % d'images sans saut à 60 et 120 Hz | JankStats |
| Ré-analyse incrémentale après 1 caractère, 50 000 lignes | 10 ms hors fil UI | JMH — plancher mesuré : 1,6 µs (lexicographie) |
| Allocations par image de défilement en régime établi | 0 | test d'allocations |
| Mémoire du modèle et de son index | 3 × la taille du texte | profil mémoire — mesuré : 1,22× (modèle seul) |
| Démarrage à froid de la vue | 100 ms | benchmark |

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Canvas une vue + caches par rangée (proposition) | **retenue** | héritage validé du dépôt de départ ; ratio mesure du spike ; tactile d'abord |
| B. Afficheur recyclable par ligne (liste virtuelle) | écartée | retour à la ligne souple, plis, gouttière continue et loupe deviennent des cas d'exception coûteux ; le dépôt de départ l'avait écartée à juste titre |
| C. Composition déclarative par frame | écartée | allocations et indirections sur le chemin chaud ; contredit le mandat |

## Compromis

- Canvas une vue concentre la complexité dans le peintre : payé par les
  plafonds de classe (ADR-002) et les tests de capture.
- Le pré-calcul asynchrone introduit une image de retard possible sur les
  lignes fraîchement éditées : borné par le budget de frappe, visible dans
  la démo (scénario de mesure en direct).

## Conséquences

- Les budgets deviennent des seuils de benchmark en CI dès M3 ; la
  régression d'un budget bloque la fusion (cliquet : uniquement vers le bas).
- L'appareil de référence (milieu de gamme, désigné par le propriétaire à
  M1) fixe les chiffres définitifs ; ce document sera mis à jour par
  génération, pas à la main.
- Les pièges PIEGE-01, PIEGE-02, PIEGE-04, PIEGE-11, PIEGE-12, PIEGE-14 et
  PIEGE-15 sont écrits comme tests **avant** les couches de rendu
  correspondantes.

## Actions

- [ ] M1 : désigner l'appareil de référence et calibrer les budgets.
- [ ] M3 : harnais `FrameMetrics` + scénario de démo « mesures en direct ».
- [ ] M3 : test d'allocations par image branché en CI.
