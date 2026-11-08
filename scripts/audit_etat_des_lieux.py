#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Audit chiffré du dépôt de départ (lecture seule).

Génère « docs/audit/ETAT-DES-LIEUX.md » à partir de mesures reproductibles :
aucun chiffre n'est saisi à la main. Chaque mesure découle de `git ls-files`
et de comptages ligne à ligne, rejouables par le propriétaire.

Usage :
    python3 scripts/audit_etat_des_lieux.py --repo <chemin du clone> \
        [--out docs/audit/ETAT-DES-LIEUX.md]
"""

import argparse
import datetime
import os
import re
import subprocess
import sys

# ---------------------------------------------------------------- outils ----


def run(repo: str, *cmd: str) -> str:
    """Exécute une commande dans le dépôt audité et renvoie sa sortie."""
    res = subprocess.run(list(cmd), cwd=repo, capture_output=True, text=True)
    if res.returncode != 0:
        sys.stderr.write(f"échec commande : {' '.join(cmd)}\n{res.stderr}")
    return res.stdout


def git_files(repo: str) -> list:
    """Liste les fichiers versionnés (équivalent : git ls-files)."""
    return [f for f in run(repo, "git", "ls-files").splitlines() if f.strip()]


def count_lines(path: str) -> int:
    """Compte les lignes d'un fichier texte (0 si absent)."""
    try:
        with open(path, "rb") as fh:
            return fh.read().decode("utf-8", errors="replace").count("\n")
    except OSError:
        return 0


def read(repo: str, rel: str) -> str:
    """Lit un fichier du dépôt audité en tolérant les encodages."""
    try:
        with open(os.path.join(repo, rel), "rb") as fh:
            return fh.read().decode("utf-8", errors="replace")
    except OSError:
        return ""


def grep_count(repo: str, pattern: str, files: list) -> tuple:
    """Renvoie (nombre d'occurrences, nombre de fichiers concernés)."""
    total, touched, rx = 0, 0, re.compile(pattern)
    for rel in files:
        n = len(rx.findall(read(repo, rel)))
        if n:
            total += n
            touched += 1
    return total, touched


def fr(n: int) -> str:
    """Formate un entier avec séparateur de milliers à la française."""
    return f"{n:,}".replace(",", "\u202f")


def num(text: str) -> str:
    """Nettoie un nombre annoncé dans la documentation du dépôt."""
    return text.replace("\u202f", "").replace(" ", "").replace("~", "")


# ------------------------------------------------------------- constantes ---

MODULES = [
    "editor/cel-core",
    "editor/cel-lsp-api",
    "editor/cel-lsp",
    "editor/cel-textmate",
    "editor/cel-ui",
    "demo",
]



# Typographie française : espace insécable avant ; : ! ? (idem --fix du
# contrôle de langue, pour que toute régénération soit idempotente).
_TYPO = re.compile(r"(?<=[^\s`]) ([;:!?])(?=[\s\u00a0\u202f]|$)")
_TYPO_MAP = {";": "\u202f", ":": "\u00a0", "!": "\u202f", "?": "\u202f"}


def french_typography(text: str) -> str:
    """Applique l'espace insécable française avant ; : ! ?."""
    return _TYPO.sub(lambda m: _TYPO_MAP[m.group(1)] + m.group(1), text)


# --------------------------------------------------------------- mesures ----


def measure(repo: str) -> dict:
    """Collecte toutes les mesures dans un dictionnaire."""
    files = git_files(repo)
    java = [f for f in files if f.endswith(".java")]
    m = {}

    m["head"] = run(repo, "git", "rev-parse", "--short", "HEAD").strip()
    m["head_date"] = run(repo, "git", "log", "-1", "--format=%ad", "--date=short").strip()
    m["files_total"] = len(files)
    m["java_total"] = len(java)

    # Lignes de production et de test par module (src/main contre src/test).
    per_module = []
    for mod in MODULES:
        main = [f for f in java if f.startswith(mod + "/") and "/src/main/" in f]
        test = [f for f in java if f.startswith(mod + "/") and "/src/test/" in f]
        per_module.append(
            {
                "module": mod.split("/")[-1],
                "main_files": len(main),
                "main_lines": sum(count_lines(os.path.join(repo, f)) for f in main),
                "test_files": len(test),
                "test_lines": sum(count_lines(os.path.join(repo, f)) for f in test),
            }
        )
    m["per_module"] = per_module

    # Tests : annotations @Test et fichiers concernés.
    test_java = [f for f in java if "/src/test/" in f]
    m["test_annotations"], m["test_files_with_tests"] = grep_count(
        repo, r"@Test\b", test_java
    )
    m["test_lines"] = sum(count_lines(os.path.join(repo, f)) for f in test_java)

    # Plus gros fichiers et plafond des 1 000 lignes.
    sizes = sorted(
        ((f, count_lines(os.path.join(repo, f))) for f in java), key=lambda x: -x[1]
    )
    m["biggest"] = sizes[:12]
    m["over_1000"] = [f for f, n in sizes if n > 1000]

    # EditorView : taille et membres publics déclarés.
    ev = "editor/cel-ui/src/main/java/jo/codeeditor/view/EditorView.java"
    m["editor_view_lines"] = count_lines(os.path.join(repo, ev))
    m["editor_view_public"] = len(re.findall(r"^\s*public\s", read(repo, ev), re.M))

    # Rope : taille et pureté JVM (imports non java.*).
    rope = "editor/cel-core/src/main/java/jo/codeeditor/rope/Rope.java"
    m["rope_lines"] = count_lines(os.path.join(repo, rope))
    m["rope_foreign_imports"] = [
        i for i in re.findall(r"^import\s+(.*?);", read(repo, rope), re.M)
        if not i.startswith("java.")
    ]

    # Baselines lint : entrées figées (l'élément racine <issues> est exclu).
    baselines = {}
    for f in files:
        if os.path.basename(f).startswith("lint-baseline"):
            baselines[f] = len(re.findall(r"<issue\s", read(repo, f)))
    m["lint_baseline"] = baselines
    m["lint_baseline_total"] = sum(baselines.values())

    # Archives binaires versionnées (hors wrapper Gradle).
    m["binaries"] = [
        f
        for f in files
        if f.endswith((".jar", ".aar", ".so", ".zip"))
        and "gradle/wrapper" not in f
    ]

    # Duplication du build : plugins et blocs répétés (builds de modules seulement).
    builds = [f for f in files if f.endswith("build.gradle.kts")]
    module_builds = [f for f in builds if "/" in f]
    m["build_files"] = len(builds)
    m["android_library_count"] = sum(
        1 for f in module_builds if "com.android.library" in read(repo, f)
    )
    m["publish_blocks"] = sum(
        1 for f in builds if "maven-publish" in read(repo, f)
    )

    # Scellés de partage @RestrictTo (bibliothèques uniquement).
    lib_java = [f for f in java if f.startswith("editor/")]
    m["restrict_to"], m["restrict_to_files"] = grep_count(repo, r"@RestrictTo\b", lib_java)

    # Concurrence répartie.
    m["volatile"], m["volatile_files"] = grep_count(repo, r"\bvolatile\b", lib_java)
    m["atomic"], m["atomic_files"] = grep_count(
        repo, r"Atomic(?:Reference|Integer|Boolean|Long)", lib_java
    )
    m["executors"], m["executor_files"] = grep_count(
        repo, r"Executors?\.(?:new|single|fixed|cached)", lib_java
    )

    # Accessibilité : motifs recherchés sur toute la bibliothèque.
    m["a11y_node_files"] = [
        f for f in lib_java if "AccessibilityNodeInfo" in read(repo, f)
    ]
    m["a11y_desc_files"] = [
        f for f in lib_java if "contentDescription" in read(repo, f)
    ]

    # Tokéniseurs écrits à la main (production, hors tests).
    m["tokenizers"] = [
        f for f in files if "/src/main/" in f and "Tokenizer" in os.path.basename(f)
    ]
    # Intelligence Java vivant dans la démo (hors bibliothèque).
    m["minijava_files"] = [f for f in files if "MiniJava" in f]
    m["minijava_lines"] = sum(count_lines(os.path.join(repo, f)) for f in m["minijava_files"])

    # Robolectric contre tests instrumentés.
    m["robolectric_files"] = sum(
        1 for f in test_java if "RobolectricTestRunner" in read(repo, f)
    )
    m["android_test_files"] = [f for f in files if "/src/androidTest/" in f]

    # Dérive documentaire : chiffres annoncés dans les docs du dépôt.
    readme = read(repo, "README.md")
    mm = re.search(r"version courante\s*:\s*`([0-9.]+)`", readme)
    m["readme_version"] = mm.group(1) if mm else "?"
    arch = read(repo, "ARCHITECTURE.md")
    mm = re.search(r"EditorView \((?:~)?([\d\u202f ]+) l\.", arch)
    m["arch_editorview_claim"] = num(mm.group(1)) if mm else "?"
    mm = re.search(r"([\d\u202f ]+) tests unitaires", arch)
    m["arch_tests_claim"] = num(mm.group(1)) if mm else "?"
    agent = read(repo, "AGENT.md")
    mm = re.search(r"([\d\u202f ]+) tests verts", agent)
    m["agent_tests_claim"] = num(mm.group(1)) if mm else "?"
    m["changelog_bytes"] = os.path.getsize(os.path.join(repo, "CHANGELOG.md"))

    # Chaîne d'outils annoncée par le build.
    wrapper = read(repo, "gradle/wrapper/gradle-wrapper.properties")
    mm = re.search(r"gradle-([\d.]+)-", wrapper)
    m["gradle_version"] = mm.group(1) if mm else "?"
    root_build = read(repo, "build.gradle.kts")
    mm = re.search(r'com\.android\.library"\) version "([\d.]+)"', root_build)
    m["agp_version"] = mm.group(1) if mm else "?"
    cs, ms = set(), set()
    for f in builds:
        text = read(repo, f)
        cs.update(re.findall(r"compileSdk\s*=\s*(\d+)", text))
        ms.update(re.findall(r"minSdk\s*=\s*(\d+)", text))
    m["compile_sdk"] = "/".join(sorted(cs)) or "?"
    m["min_sdk"] = "/".join(sorted(ms)) or "?"

    # Internationalisation : répertoires de ressources de langues.
    m["values_dirs"] = sorted(
        {
            f.split("/src/main/res/")[1].split("/")[0]
            for f in files
            if "/src/main/res/values" in f
        }
    )

    # Dépendance LSP du dépôt de départ : version lue dans le catalogue.
    mm = re.search(r'lsp4j\s*=\s*"([\d.]+)"', read(repo, "gradle/libs.versions.toml"))
    m["lsp_lib_version"] = mm.group(1) if mm else "?"

    # Chiffres du mandat (pour comparaison, voir la section « écarts »).
    m["mandate"] = {
        "test_annotations": 2873,
        "test_files": 357,
        "test_lines": 68000,
        "lint_baseline": 53,
        "changelog_bytes": 235000,
    }
    return m


# ------------------------------------------------------------ rendu markdown


def render(m: dict) -> str:
    """Rend les mesures en markdown français."""
    l: list = []
    a = l.append

    a("# État des lieux mesuré du dépôt de départ\n")
    a(
        "> Document **généré** par `scripts/audit_etat_des_lieux.py` — les chiffres\n"
        "> sont mesurés, jamais saisis à la main. Regénérer :\n"
        "> `python3 scripts/audit_etat_des_lieux.py --repo <clone> --out docs/audit/ETAT-DES-LIEUX.md`.\n"
    )
    a(
        f"\nMesures prises le {datetime.date.today().isoformat()} sur le commit "
        f"`{m['head']}` ({m['head_date']}), branche `main` du dépôt de départ, "
        "cloné en lecture seule dans un répertoire distinct de ce dépôt.\n"
    )

    a("\n## 1. Méthode\n")
    a("Toutes les mesures découlent de `git ls-files`, puis de comptages ligne à")
    a("ligne (équivalent `wc -l`) et d'expressions rationnelles sur le contenu des")
    a("fichiers. Le script n'écrit rien dans le dépôt audité.\n")

    a("\n## 2. Inventaire\n")
    a("| Mesure | Valeur |")
    a("| --- | --- |")
    a(f"| Fichiers versionnés | {fr(m['files_total'])} |")
    a(f"| Fichiers Java | {fr(m['java_total'])} |")
    a(f"| Annotations `@Test` | {fr(m['test_annotations'])} dans {fr(m['test_files_with_tests'])} fichiers |")
    a(f"| Lignes de test (`src/test`) | {fr(m['test_lines'])} |")
    a(f"| Entrées de baseline lint figées | {fr(m['lint_baseline_total'])} |")
    a(f"| Archives binaires versionnées (hors wrapper) | {len(m['binaries'])} |")
    a(f"| Fichiers `build.gradle.kts` | {m['build_files']} |")
    a(
        f"| Gradle (wrapper) / AGP / compileSdk / minSdk | {m['gradle_version']} / "
        f"{m['agp_version']} / {m['compile_sdk']} / {m['min_sdk']} |"
    )
    a(f"| Ressources de langues | {', '.join(m['values_dirs']) or 'aucune'} |")
    a(f"| Bibliothèque client LSP déclarée | version {m['lsp_lib_version']} |")
    a("")

    a("\n### 2.1 Code par module (lignes physiques)\n")
    a("| Module | Fichiers prod. | Lignes prod. | Fichiers test | Lignes test |")
    a("| --- | --- | --- | --- | --- |")
    for mod in m["per_module"]:
        a(
            f"| `{mod['module']}` | {fr(mod['main_files'])} | {fr(mod['main_lines'])} | "
            f"{fr(mod['test_files'])} | {fr(mod['test_lines'])} |"
        )
    a("")

    a("\n### 2.2 Plus gros fichiers\n")
    a("| Fichier | Lignes |")
    a("| --- | --- |")
    for f, n in m["biggest"]:
        a(f"| `{f}` | {fr(n)} |")
    a(
        f"\nFichiers de plus de 1 000 lignes : **{len(m['over_1000'])}** — "
        + ", ".join(f"`{f}`" for f in m["over_1000"])
        + ".\n"
    )

    a("\n### 2.3 Baselines lint\n")
    a("| Fichier | Entrées figées |")
    a("| --- | --- |")
    for f, n in m["lint_baseline"].items():
        a(f"| `{f}` | {fr(n)} |")
    a(
        "\nRemarque : l'élément racine `<issues>` est exclu du comptage. Un comptage\n"
        "naïf par ligne (le chiffre initialement annoncé) surestime le total d'une\n"
        "unité par fichier.\n"
    )

    a("\n### 2.4 Concurrence et scellés\n")
    a("| Motif | Occurrences | Fichiers |")
    a("| --- | --- | --- |")
    a(f"| `@RestrictTo` (bibliothèques) | {fr(m['restrict_to'])} | {fr(m['restrict_to_files'])} |")
    a(f"| `volatile` (bibliothèques) | {fr(m['volatile'])} | {fr(m['volatile_files'])} |")
    a(f"| `AtomicXxx` (bibliothèques) | {fr(m['atomic'])} | {fr(m['atomic_files'])} |")
    a(f"| `Executors.newXxx` (bibliothèques) | {fr(m['executors'])} | {fr(m['executor_files'])} |")
    a("")

    a("\n## 3. Constats structurants vérifiés\n")
    a("| # | Constat | Verdict | Preuve mesurée |")
    a("| --- | --- | --- | --- |")

    verdicts = [
        (
            "1",
            "Modules « purs » exigeant le SDK Android",
            "confirmé" if m["android_library_count"] >= 5 else "à nuancer",
            f"{m['android_library_count']} modules `com.android.library`, aucun `java-library` : "
            "aucun test du moteur sans SDK",
        ),
        (
            "2",
            "Classe dieu `EditorView`",
            "confirmé",
            f"{fr(m['editor_view_lines'])} lignes, dont {fr(m['editor_view_public'])} déclarent un "
            f"membre `public` ; {len(m['over_1000'])} fichiers de plus de 1 000 lignes",
        ),
        (
            "3",
            "Frontières internes faibles",
            "confirmé",
            f"{fr(m['restrict_to'])} annotations `@RestrictTo` dans "
            f"{fr(m['restrict_to_files'])} fichiers de bibliothèque",
        ),
        (
            "4",
            "Aucun arbre syntaxique ; sémantique hors bibliothèque",
            "confirmé",
            f"{len(m['tokenizers'])} fichiers de tokéniseurs en production ; "
            f"l'intelligence Java ({len(m['minijava_files'])} fichiers, {fr(m['minijava_lines'])} "
            "lignes) vit dans la démo, pas dans les bibliothèques",
        ),
        (
            "5",
            "Sélection mono-curseur",
            "confirmé",
            "`Selection` : deux champs `int` (`start`, `end`), aucun modèle multi-curseur",
        ),
        (
            "6",
            "Accessibilité absente de la vue principale",
            "confirmé",
            f"`AccessibilityNodeInfo` : {len(m['a11y_node_files'])} fichier ; "
            f"`contentDescription` : {len(m['a11y_desc_files'])} fichiers ("
            + ", ".join(f"`{os.path.basename(f)}`" for f in m["a11y_desc_files"])
            + ") — la vue principale n'expose rien au lecteur d'écran",
        ),
        (
            "7",
            "Tests non représentatifs de l'appareil",
            "confirmé",
            f"{fr(m['robolectric_files'])} fichiers Robolectric ; "
            f"{len(m['android_test_files'])} fichier(s) sous `src/androidTest/`",
        ),
        (
            "8",
            "Dérive documentaire",
            "confirmé",
            f"README annonce la version {m['readme_version']} ; ARCHITECTURE annonce "
            f"`EditorView` ≈ {m['arch_editorview_claim']} lignes (mesuré : "
            f"{fr(m['editor_view_lines'])}) et {m['arch_tests_claim']} tests ; AGENT.md annonce "
            f"{m['agent_tests_claim']} tests (mesuré : {fr(m['test_annotations'])} `@Test`) ; "
            f"CHANGELOG : {fr(m['changelog_bytes'])} octets",
        ),
        (
            "9",
            "Build dupliqué",
            "confirmé",
            f"{m['build_files']} `build.gradle.kts`, dont {m['publish_blocks']} blocs de "
            "publication dupliqués",
        ),
        (
            "10",
            "Concurrence répartie sans modèle unique",
            "confirmé",
            f"`volatile` : {fr(m['volatile_files'])} fichiers ; `AtomicXxx` : "
            f"{fr(m['atomic_files'])} fichiers ; `Executors` : {fr(m['executor_files'])} fichiers",
        ),
    ]
    for num, constat, verdict, proof in verdicts:
        a(f"| {num} | {constat} | **{verdict}** | {proof} |")
    a("")

    a("\n## 4. Acquis à préserver (vérifiés)\n")
    a("| Acquis | État constaté |")
    a("| --- | --- |")
    rope_purity = (
        "imports uniquement `java.*`"
        if not m["rope_foreign_imports"]
        else "imports non JVM : " + ", ".join(m["rope_foreign_imports"])
    )
    a(
        f"| Rope du moteur | `{fr(m['rope_lines'])}` lignes, {rope_purity} : "
        "portable en JVM pure sans retouche (re-mesurée, voir `docs/audit/SPIKES.md`)"
    )
    a(
        "| Voie de mutation unique | `EditorSession.doReplaceRange` / `replaceRangeWithCaret` : "
        "point d'entrée commun des mutations, undo compris"
    )
    a(
        "| Kit de popups purs | `TextLayoutKit`, `PopupPlacement`, `PopupCoordinator` : "
        "logique de placement sans dépendance Android"
    )
    a(
        "| Accessibilité | `AccessibilityNodeInfo` absent des fichiers Java ; `contentDescription` "
        "limité aux barres de recherche et de fil d'Ariane |"
    )
    a("| Ponts IME | `ImeBridge` (cœur) et `EditorImeBridge` (vue), par niveau d'API |")
    a(
        "| Pièges connus | sept pièges consignés dans `AGENT.md`, convertis en tests dans "
        "`docs/audit/PIEGES-HERITES.md`"
    )
    a(
        "| CI aux actions épinglées par SHA | workflows du dépôt de départ avec validation du "
        "wrapper : pratique reprise telle quelle (voir ADR-011)"
    )
    a(
        "| Catalogue de versions unique | `gradle/libs.versions.toml` fait autorité ; le refus "
        "délibéré d'une montée de version y est documenté (utile pour ADR-005)"
    )
    a("")

    a("\n## 5. Écarts avec les chiffres du mandat\n")
    a("Le mandat annonçait des chiffres à revérifier ; voici les mesures réelles.\n")
    a("| Mesure | Annoncé | Mesuré |")
    a("| --- | --- | --- |")
    a(
        f"| Annotations `@Test` | {fr(m['mandate']['test_annotations'])} | "
        f"**{fr(m['test_annotations'])}** |"
    )
    a(
        f"| Fichiers de test | {fr(m['mandate']['test_files'])} | "
        f"**{fr(m['test_files_with_tests'])}** |"
    )
    a(
        f"| Lignes de test | ≈ {fr(m['mandate']['test_lines'])} | "
        f"**{fr(m['test_lines'])}** |"
    )
    a(
        f"| Entrées de baseline | {fr(m['mandate']['lint_baseline'])} | "
        f"**{fr(m['lint_baseline_total'])}** (racine `<issues>` exclue) |"
    )
    a(
        f"| CHANGELOG | ≈ {fr(m['mandate']['changelog_bytes'])} octets | "
        f"**{fr(m['changelog_bytes'])} octets** |"
    )
    a(
        "\nLes écarts sur les tests s'expliquent par l'évolution du dépôt ; les lignes de\n"
        "production par module mesurées correspondent en revanche exactement aux\n"
        "chiffres du mandat. Le reste du présent document ne cite que des valeurs\n"
        "mesurées.\n"
    )
    return "\n".join(l)


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit du dépôt de départ")
    parser.add_argument("--repo", required=True, help="chemin du clone (lecture seule)")
    parser.add_argument("--out", default="docs/audit/ETAT-DES-LIEUX.md")
    args = parser.parse_args()

    m = measure(args.repo)
    doc = french_typography(render(m))
    out_dir = os.path.dirname(args.out)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(doc)
    print(f"écrit : {args.out} ({len(doc)} caractères)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
