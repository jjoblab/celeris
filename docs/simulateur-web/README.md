# Le simulateur web des popups — la référence visuelle de l'éditeur

Ce dossier porte le **simulateur interactif de l'éditeur Celeris** : la même
machine à états que l'éditeur Android (le `PopupController` porté mot pour mot
en TypeScript), les 26 genres de popups avec leurs **sections exactes**, et
les deux traits de l'IDE de référence désormais vivants dans l'éditeur —
**l'ampoule jaune d'intentions** et **les inlay hints**.

> **Rôle.** Le simulateur est la RÉFÉRENCE DE DESIGN des popups : chaque carte
> de l'éditeur Android doit rendre exactement ce que sa carte du simulateur
> rend — mêmes sections, mêmes encres, mêmes paliers. La notification y porte
> sa **barre de progression** (l'horloge qui coule), le menu d'intentions y
> vit derrière l'**ampoule de la gouttière**, les **inlay hints** se lisent en
> fin de ligne utile.

## Lancer le simulateur

```bash
cd docs/simulateur-web
npm install      # ou bun install / pnpm install
npm run dev      # http://localhost:3000
```

Aucune autre dépendance : Next.js 16, React 19, Tailwind CSS 4 et
lucide-react. Aucune base de données, aucun réseau au fonctionnement — les
polices Google (Geist) se chargent au build.

## Ce qu'il contient

| Fichier | Rôle |
| --- | --- |
| `app/page.tsx` | La page : le simulateur à gauche, le catalogue à droite, les régimes documentés |
| `app/layout.tsx` | Le cadre racine (polices, métadonnées) |
| `components/simulateur/SimulateurEditeur.tsx` | L'éditeur miniature : cadre, gouttière (**ampoule d'intentions**), caret, diagnostics soulignés, **inlay hints** en fin de ligne, chrome (fil d'Ariane, bande, minimap, pouce, barre de touches), cartes flottantes ancrées, voile de la feuille |
| `components/popup/cartes.tsx` | Le contenu de chaque genre : les sections exactes, les encres (erreur `#F44747`, avertissement `#CCA700`, accent `#E6C178`…), les badges de genre, la **barre de progression** de la notification |
| `components/catalogue/CataloguePopups.tsx` | Le catalogue documentaire : les 26 genres par catégorie A–G, la fiche détaillée (sections du design, déclencheurs, régimes, ancre, API hôte) |
| `lib/machine.ts` | La machine à états : genres ouverts, caret, fenêtre, journal, annonce (horloge 4 000 ms), chrome, **inlay hints du terrain simulé** |
| `lib/popups.ts` | Les transitions pures : le portage exact du `PopupController` (frappe, défilement, tap hors cartes, échappement, voile, exclusions) |

## Les deux traits de l'IDE de référence

**L'ampoule jaune d'intentions.** Dans la gouttière, à la ligne du caret qui
porte un diagnostic, l'ampoule (`#FFCB6B`) remplace le marqueur `×` / `!` —
exactement comme l'IDE de référence. Son tap ouvre le menu d'intentions. Elle
se coupe dans le groupe « Le chrome et la langue ».

**Les inlay hints.** Le type d'un `var` se lit en fin de ligne utile, à
l'encre muette italique — jamais dans le flux : le texte ne se décale pas
(`var resultat = 0;` porte `: int`). L'éditeur Android suit le même
dessin (`InlayHintsPainter` d'android-view). Ils se coupent aussi dans le
groupe du chrome.

## Les régimes à essayer

La machine journalise chaque transition en français — déclenchez une popup,
puis :

- **la frappe** (`frapperLettre`) ferme les bulles, la complétion filtre et
  reste, l'annonce vit son quart ;
- **le défilement** ferme l'ancre au curseur, le reste se repositionne ;
- **le tap hors cartes** referme les cartes d'actions, le caret se pose
  quand même ;
- **l'horloge** referme la notification à 4 000 ms — la barre de progression
  coule, la re-pose la réarme sans cumul ;
- **l'échappement** referme la première carte interactive de l'ordre clavier ;
- **le voile modal** coupe l'intelligence et les actions.

Le catalogue de droite documente chaque genre : quand l'ouvrir, ses sections,
son ancrage, sa fermeture, les régimes qui l'épargnent.

## Le lien avec l'éditeur Android

| Simulateur (ce dossier) | Éditeur Android |
| --- | --- |
| `lib/popups.ts` (transitions) | `android-widgets` · `PopupController` |
| `components/popup/cartes.tsx` (sections, encres) | `android-widgets` · `TextRowsPopup`, les vues |
| La barre de progression de la notification | `android-widgets` · `NotificationView` (jauge + compte à rebours) |
| L'ampoule jaune de la gouttière | `android-view` · `GutterPainter` (marque d'ampoule) + `android-host` · le tap ouvre le menu d'intentions |
| Les inlay hints en fin de ligne utile | `android-view` · `InlayHintsPainter` + `android-host` · le moteur `var` |

La parité est la règle : une divergence entre une carte réelle et sa carte du
simulateur est un défaut à corriger — le simulateur fait foi.
