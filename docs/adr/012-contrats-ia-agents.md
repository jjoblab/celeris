# ADR-012 — Contrats IA et outils d'agent

- **Statut** : Proposition (M0)
- **Date** : 2026-11-04

## Contexte

Le mandat classe l'IA native en catégorie F de la matrice de parité : texte
fantôme, prédiction de la prochaine édition, revue des modifications
d'agent, API d'outils pour agents — avec des contraintes fortes : aucune
requête sans consentement explicite, télémétrie en option uniquement,
annulation et budgets de latence, respect d'un fichier d'instructions
d'agent et d'un fichier d'exclusion de contexte. Le cœur ne doit dépendre
d'aucun fournisseur : les modèles sont locaux ou distants, branchés par
l'hôte.

## Décision proposée

`core-ai` définit des **contrats**, pas des clients : le moteur de l'éditeur
n'appelle jamais un réseau ; l'hôte fournit les implémentations.

1. **Texte fantôme** : complétion en ligne multiligne affichée comme
   suggestion éphémère dans le rendu (même chemin de dessin que les indices
   en ligne, PIEGE-11 applicable) ; acceptation par mot, par ligne ou en
   bloc ; refus au clavier et au geste. Budget de latence d'affichage
   consigné ; l'attente ne bloque jamais la frappe (annulation immédiate
   à la frappe suivante — jetons d'ADR-007).
2. **Prédiction de la prochaine édition** : contrat distinct de la
   complétion (le modèle prédit une opération, pas un texte) ; les
   prédictions s'appliquent comme transactions normales, donc annulables
   et diffables.
3. **Revue des modifications d'agent** : les éditions d'agent arrivent
   comme transactions étiquetées ; la vue affiche le diff en ligne par
   blocs acceptés/refusés ; des **points de contrôle** (instantanés
   nommés) permettent le retour arrière par bloc. La vue de différences
   côte à côte est la même que celle du VCS (matrice D-12).
4. **API d'outils de l'éditeur pour agents** (surface compatible avec les
   serveurs d'outils du protocole MCP, norme citée comme norme) :
   - lire des plages et des instantanés ;
   - lister les symboles (index de l'ADR-005) ;
   - lire les diagnostics ;
   - appliquer une transaction **avec détection de conflit** (l'instantané
     de référence doit correspondre, sinon refus explicite — jamais de
     fusion silencieuse).
5. **Exclusions de contexte** : un fichier d'exclusion déclare les
   chemins jamais envoyés à un modèle (secrets, clés, données du
   propriétaire) ; le contrôle en CI vérifie qu'un scénario de la démo le
   respecte. Le fichier d'instructions d'agent du dépôt est honoré par les
   outils exposés.
6. **Consentement et télémétrie** : aucun trafic sortant sans activation
   explicite de l'utilisateur ; la télémétrie est en option, anonyme,
   documentée ; la démo n'active rien par défaut et le prouve par un test
   (aucune prise réseau en mode par défaut).
7. Les contrats IA sont **testables sans modèle** : implémentations de
   référence déterministes (faux modèle scripté) en JVM pure.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Contrats purs + faux modèles en test (proposition) | **retenue** | cœur sans réseau ; hôte maître de ses modèles ; testabilité totale |
| B. Client intégré à un fournisseur | écartée | dépendance au réseau et à un tiers dans le cœur ; contredit le consentement explicite |
| C. IA différée après 1.0 | écartée | catégorie F P0 du mandat ; l'architecture des transactions (ADR-003) est le socle : la construire tard coûte plus cher |

## Compromis

- L'hôte écrit plus de code d'intégration (fourniture du modèle) : la démo
  embarque une implémentation locale de référence qui prouve la
  suffisance du contrat.
- La détection de conflit peut refuser des éditions d'agent valides en cas
  de course : le refus explicite est le comportement demandé (jamais de
  fusion silencieuse), avec message lisible.

## Conséquences

- `core-ai` n'a aucune dépendance transport : pas de client réseau dans
  le cœur, seulement des interfaces et des modèles de données.
- Le scénario de démo « agent modifie le fichier » exerce la revue par
  blocs et les points de contrôle, avec test instrumenté de fumée.
- Les budgets de latence IA sont mesurés sur le faux modèle (chemin de
  code identique, transport substitué) et calibrés sur modèle réel par
  l'hôte.

## Actions

- [ ] M8 : contrats texte fantôme + prédiction, faux modèle, budgets.
- [ ] M8 : API d'outils avec conflits, surface compatible MCP, exclusions.
- [ ] M8 : test « aucune requête sans consentement » branché en CI.
