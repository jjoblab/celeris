#!/usr/bin/env python3
"""P3/P7 — cartographie du couplage vue -> familles internes.

Référencé par la feuille des améliorations (P3 : « le verrou
cartographié ») et exécuté pour P7 (la vague des 16 familles
couplées). Pour chaque classe package-privée du racine android-view :
les membres d'EditorView touchés via n'importe quel récepteur typé
EditorView (paramètre vue, editeur, view...), et la liste des
autonomes. La visibilité de chaque classe vient du grep de la
déclaration (pas d'une regex à capture unique).
"""
import re
from pathlib import Path

VUE = Path("android-view/src/main/java/jo/celeris/android/view")
files = {f.stem: f for f in VUE.glob("*.java")}
srcs = {n: f.read_text(encoding="utf-8") for n, f in files.items()}

# --- (1) visibilité réelle : la ligne de déclaration du type -------
def visibilite(nom, src):
    for m in re.finditer(r"^([^\n]*?)\b(?:class|interface|enum|record)\s+" + nom + r"\b", src, re.M):
        return "public" if "public" in m.group(1) else "package"
    return "?"

vis = {n: visibilite(n, s) for n, s in srcs.items()}
pub = sorted(n for n, v in vis.items() if v == "public")
pkg = sorted(n for n, v in vis.items() if v == "package")
print(f"PUBLICS ({len(pub)}) : {', '.join(pub)}")
print(f"\nPACKAGE-PRIVÉS ({len(pkg)}) : {', '.join(pkg)}")

# --- (2) couplage par récepteur EditorView typé ---------------------
# membres de EditorView (champs + méthodes, tout visibilité)
EDITOR = srcs["EditorView"]
noms_membres = set()
for m in re.finditer(r"^ {4}(?:[^\n;{}]*?)\s(\w+)\s*[;=(]", EDITOR, re.M):
    noms_membres.add(m.group(1))
for m in re.finditer(r"^ {4}(?:[^\n;{}]*?)\s(\w+)\(", EDITOR, re.M):
    noms_membres.add(m.group(1))
noms_membres.discard("EditorView")

couplage = {}
for n, s in sorted(srcs.items()):
    if n == "EditorView" or vis[n] != "package":
        continue
    # récepteurs : identifiants déclarés de type EditorView
    recvs = set(re.findall(r"\bEditorView\s+(\w+)", s))
    recvs.discard("vue")  # toujours inclus ci-dessous
    recvs.add("vue")
    accs = set()
    for r in recvs:
        accs |= set(re.findall(r"\b" + r + r"\.(\w+)", s))
    touches = sorted(a for a in accs if a in noms_membres)
    if touches:
        couplage[n] = (len(touches), touches)

print(f"\n### CLASSES PACKAGE-PRIVÉES COUPLÉES ({len(couplage)}) :")
for n, (c, t) in sorted(couplage.items(), key=lambda x: -x[1][0]):
    print(f"   {n} ({c}) : {', '.join(t)}")

autonomes = [n for n in pkg if n != "EditorView" and n not in couplage]
print(f"\n### PACKAGE-PRIVÉES AUTONOMES ({len(autonomes)}) : {', '.join(sorted(autonomes))}")
