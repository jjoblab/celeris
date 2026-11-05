# Corpus Python — outillage d'un éditeur embarquable
# -*- coding: utf-8 -*-

import os
from pathlib import Path


class Corpus:
    """Un corpus embarque, repliable."""

    def __init__(self, racine, limite=1000):
        self.racine = Path(racine)
        self.limite = limite
        self._cache = {}

    @property
    def taille(self):
        return len(self._cache)

    def charger(self, nom, encodage="utf-8"):
        if nom not in self._cache:
            chemin = self.racine / nom
            self._cache[nom] = chemin.read_text(encoding=encodage)
        return self._cache[nom]


def repliquer(base, cible=50_000):
    """Replique un corpus jusqu'a la cible."""
    sortie = []
    tour = 0
    while sum(len(s.splitlines()) for s in sortie) < cible:
        sortie.append(base.replace("Exemple", f"Exemple{tour}"))
        tour += 1
    return "\n".join(sortie)


DOCUMENTATION = """
Une chaine triple guillemets,
elle traverse les lignes
et se replie d'un pli.
"""

if __name__ == "__main__":
    corpus = Corpus(".", limite=0x1F)
    print(DOCUMENTATION.strip())
    print(os.linesep.join(["pret", "a", "l'essai"]))
