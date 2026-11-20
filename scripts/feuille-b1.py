#!/usr/bin/env python3
"""B1 — mises à jour documentaires de la surcouche sémantique."""
from pathlib import Path

NBSP = "\u00a0"
p = Path("docs/feuille-de-route-coloration.md")
t = p.read_text(encoding="utf-8")

# 1. le statut de la section A6 (reportée)
ancien_statut_a6 = (
    "- **Statut**" + NBSP + ": \U0001f7e1 étude posée — la décision appartient au\n"
    "  propriétaire" + NBSP + "; les données vivent dans la réponse de session et la\n"
    "  comparaison des chemins ci-dessus."
)
nouveau_statut_a6 = (
    "- **Statut**" + NBSP + ": \U0001f534 REPORTÉE par le propriétaire le 2026-11-20\n"
    "  («" + NBSP + "A6 une autre fois" + NBSP + "») — les données restent posées,\n"
    "  la décision reviendra à lui."
)
if ancien_statut_a6 in t:
    t = t.replace(ancien_statut_a6, nouveau_statut_a6)
    print("statut A6 remplacé")
else:
    print("ATTENTION : statut A6 introuvable — vérifier à la main")

# 2. la section B1 avant «## Journal de suivi»
section_b1 = """
### B1 — Surcouche sémantique LSP (ADR-0027 acceptée)

- **Décision du propriétaire** (2026-11-20)" + NBSP + ": «" + NBSP + "Je valide
  l'adr 027" + NBSP + "» — câbler en surcouche, exécuté le jour même.
- **Ce qui vit où**" + NBSP + ": `SemanticOverlay` (pure, android-view) porte
  les jetons transposés en offsets absolus, la légende et la révision
  servie" + NBSP + "; `SemanticRowSource` enveloppe la source habillée" + NBSP + ";
  `EditorSyntax.poseSemantique` compose à l'habillage (le même endroit
  que le rattrapage D-05) et RECALE à la frame (PIEGE-20" + NBSP + ": une pose
  périmée se vide, jamais elle ne colore une révision plus récente)" + NBSP + ";
  `EditorView.setSemanticTokens` est la porte publique" + NBSP + "; côté hôte,
  `EditorPopups` demande au calme (300" + NBSP + "ms après la synchronisation,
  une demande par rafale de frappe) et transpose par `LspPositions`
  (frontière ADR-005)" + NBSP + "; `LanguageSession.tokenTypes()` (défaut vide)
  porte la légende convenue.
- **Les règles tenues**" + NBSP + ": le jeton RAFFINE — il ne colore que
  l'intersection avec la preuve syntaxique dessous, jamais un
  remplacement" + NBSP + "; le jeton orphelin s'ignore" + NBSP + "; le recouvrement
  de jetons se rogne (le premier l'emporte)" + NBSP + "; le type inconnu de la
  légende reste muet" + NBSP + "; l'effacement (débranchement du serveur)
  rend la coloration syntaxique seule.
- **Bancs**" + NBSP + ": `SemanticOverlayTest` 7/7 (fusion pure) et
  `ColorationSemantiqueVueTest` 3/3 (bout en bout sur le harnais" + NBSP + ":
  raffinement servi, périmé ignoré, effacement) — le banc
  d'intégration a attrapé une pose périmée qui colorait quand même" + NBSP + ":
  corrigée au recalage à l'habillage.
""".replace('" + NBSP + "', NBSP)
t = t.replace("## Journal de suivi", section_b1 + "\n## Journal de suivi", 1)

# 3. la ligne du journal (insérée après l'en-tête du tableau)
ligne_journal = (
    "| 2026-11-20 | B1 | (ce commit) | ADR-0027 validée par le propriétaire "
    "puis exécutée le jour même\u00a0: surcouche, fusion à l'habillage, "
    "fraîcheur PIEGE-20\u00a0; SemanticOverlayTest 7/7, "
    "ColorationSemantiqueVueTest 3/3 (le banc a attrapé une pose périmée "
    "qui colorait — corrigée au recalage) |\n"
)
ancre_journal = (
    "| 2026-11-19 | création | — | feuille posée après décision du "
    "propriétaire (option A) |\n"
)
if ancre_journal in t:
    t = t.replace(ancre_journal, ancre_journal + ligne_journal, 1)
    print("ligne de journal insérée")
else:
    print("ATTENTION : ancre journal introuvable")

p.write_text(t, encoding="utf-8")
print("feuille coloration : B1 complète")
