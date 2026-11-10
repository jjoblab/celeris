# Audit d'accessibilité — M9 (2026-11-10)

> **Déclenchement** : le jalon M9 (feuille de route) — « Audit
> d'accessibilité complet ». L'ADR-008 promettait un « auditeur
> d'accessibilité en CI dès M3 » : il n'existait pas, l'audit le livre.
>
> **État** : l'audit est exécuté, deux défauts réels corrigés, l'auditeur
> automatique vit dans `check` (bloquant) avec ses dix auto-tests.

## Méthode

L'audit suit les six décisions de l'ADR-008, une par une : pour chaque
mécanisme, le fichier qui le porte, les bancs qui le prouvent, les
limites qui restent. La part vérifiable mécaniquement vit désormais
dans `scripts/check-accessibilite` (CI bloquante, câblée à
`policyAccessibilityCheck`, auto-testée par
`scripts/test-check-accessibilite` — dix fixtures, dix verdicts).

## Décision 1 — l'arbre d'accessibilité virtuel

**Constaté.** `AccessibilityHost`
(`android-view/src/main/java/jo/celeris/android/view/AccessibilityHost.java`,
269 lignes) expose l'éditeur comme nœud éditable (`EditText`, actions
de mouvement à granularité, sélection, défilement) dont les enfants
virtuels sont les RANGÉES VISIBLES (`VirtualRows`) — la fenêtre seule
se sert, un fichier de cent mille lignes n'expose que l'écran. Les
actions s'exécutent : mouvement à granularité avec extension de
sélection, sélection posée, défilement page, clic de rangée posant le
curseur. La sélection s'annonce
(`TYPE_VIEW_TEXT_SELECTION_CHANGED`, `CurseurClavier`), le contenu
changé aussi (`TYPE_WINDOW_CONTENT_CHANGED`, `EditorView`).

**Preuves** : `VirtualRowsTest`, `MovementGranularityTest` (G-05),
`CaretsMotionTest`. **Limite** : `findFocus` du fournisseur rend
toujours null — le focus virtuel par rangée n'est pas suivi par
l'hôte ; le lecteur d'écran d'Android garde la gestion du focus
d'exploration, constat sans suite demandée à ce jour.

## Décision 2 — ressources en et fr à 100 %

**Constaté.** 116 chaînes en défaut, 116 en français, clés
identiques ; la parité vit dans `scripts/check-language-policy`
(règle 4) depuis M3 et dans l'auditeur M9 (parité des clés par
module). Les libellés des layouts et les titres des menus vivent tous
en `@string/…` — aucun littéral (l'auditeur l'exige désormais, règle
de l'ADR-008 n° 2).

**Preuve** : `scripts/check-accessibilite` — 25 items de menu titrés,
zéro littéral.

## Décision 3 — texte bidirectionnel (UAX #9)

**Constaté.** Le cœur porte la directionnalité (`Bidi`, `BidiRuns`,
`BidiEmbeddings`, `BidiClassifier` dans `core-text`), le rendu suit
l'ordre visuel (`VisualRuns`, `VisualProjection` dans `android-view`).

**Preuves** : `BidiTest` (core-text), `VisualRunsTest`,
`VisualProjectionTest` (vue).

## Décision 4 — groupes de graphèmes (UAX #29)

**Constaté.** Les opérations « par caractère » opèrent sur les
clusters (`Grapheme` dans `core-text`) ; la granularité CHARACTER de
l'accessibilité comme la suppression avant du clavier matériel.

**Preuves** : `GraphemeTest` (corpus emoji composés par ZWJ, CJK,
combinants), `CaretsMotionTest`, `MovementGranularityTest`.

## Décision 5 — contraste élevé

**Constaté.** Trois thèmes (clair, sombre, contraste élevé) écrits
dans `ScopeThemes` ; chaque encre des thèmes contraste ≥ 4,5:1 sur le
fond de SON thème, banc WCAG à l'appui — le quatrième thème
« Atelier » importé du format VS Code est exclu du banc (sa palette
vient du fichier seul, l'importeur ne promet pas le contraste).

**Preuves** : `ScopeThemeContrastTest`, `WcagContrastTest` (utilitaire
`WcagContrast`), `SyntaxPaletteTest`.

## Décision 6 — polices de repli et mesure injectée

**Constaté.** La mesure de police vit dans `ViewFonts` (mesure
injectée, jamais lue en direct — tests déterministes), les variantes
gras mémoïsées (`TypefaceCache`, PIEGE-14), les tailles en sp
converties une fois à la densité, échelle de police de
l'accessibilité comprise (`TaillesSp`, PIEGE-23).

**Preuves** : `TypefaceCacheTest`, `TaillesSpTest`,
`EditorMetricsTest` (PIEGE-05), `PinchZoomGestureTest` (PIEGE-28).

## Les deux défauts trouvés — et corrigés

L'auditeur naissant a fait tomber DEUX défauts réels dans les réglages
de la démonstration (`activity_settings.xml`), rouges au premier
passage, verts après correctif :

1. **Les CINQ interrupteurs MaterialSwitch sans nom** : les
   interrupteurs de numéros de ligne, retour à la ligne, thème,
   consentement IA et surimpression étaient nus — le lecteur
   d'écran annonçait « interrupteur » sans dire lequel. Le libellé
   visible (TextView titre) ne portait pas d'association. Corrigé :
   chaque libellé porte désormais `android:labelFor="@id/…"`
   (l'association sémantique canonique) vers son interrupteur.
2. **Le curseur de lenteur d'analyse sans nom** : le Slider de réglage
   (zéro à six cents millisecondes) était nu lui aussi. Corrigé : son
   libellé porte `android:labelFor="@id/curseur_lenteur"`.

Après correctif : **9 widgets interactifs, 9 nommés** (trois
ImageButtons de la barre de recherche par `contentDescription` —
déjà conformes —, cinq interrupteurs et un curseur par `labelFor`),
25 items de menu titrés, parité en/fr des clés, câblage de l'arbre
virtuel vivant.

## Ce que l'auditeur vérifie désormais (CI bloquante)

1. chaque widget interactif d'un layout porte un nom de ressource —
   label propre (`contentDescription`/`text`/`hint` en `@string/…`)
   ou `labelFor` d'un libellé ; un interactif EXCLU (le lecteur
   d'écran se tait) est une violation ;
2. aucun label littéral dans les layouts (i18n, ADR-008 n° 2) ;
3. chaque item de menu porte un titre `@string/…` ;
4. parité des clés `values/strings.xml` ↔ `values-fr/strings.xml`
   par module qui a des ressources ;
5. le câblage de l'arbre virtuel vit toujours : `AccessibilityHost`
   présent, la vue expose son fournisseur de nœuds
   (`getAccessibilityNodeProvider`), la sélection s'annonce
   (`TYPE_VIEW_TEXT_SELECTION_CHANGED`).

Les images décoratives restent explicitement muettes
(`contentDescription="@null"`, huit occurrences) — c'est le patron
correct, l'auditeur l'accepte pour le non interactif.

## Limites de l'audit, honnêtement

- **Aucun test sur appareil réel** (le statut « Vérifié appareil » de
  la matrice compte zéro ligne) : l'IME réelle par fabricant, les
  gestes sur vitre et TalkBack vivant restent au plan du propriétaire
  (`docs/qualite/VERIFICATION-APPAREIL.md`) — l'audit ne peut pas
  les remplacer.
- **L'auditeur est déclaratif** : il audite les surfaces XML et le
  câblage des sources ; le comportement (annonces, granularités,
  contraste) est prouvé par les bancs JVM cités ci-dessus, pas par
  lui.
- **Les conteneurs cliquables** (rangées de réglages, cartes de
  fichiers de l'accueil) ne sont pas dans son périmètre : leurs
  enfants textuels se lisent au lecteur d'écran par construction
  Android ; aucun constat de défaut n'y a été relevé à la main.
- **Le zoom de loupe** (D-16) et les poignées de sélection sont des
  peintures Canvas : leur accessibilité passe par l'arbre virtuel
  (rangées) et le clavier (D-17), pas par des nœuds propres —
  documenté, assumé par l'option A de l'ADR-008.
