# Spikes de mesure (M0)

Code jetable de mesure : il ne deviendra jamais du code de production et
n'est pas publié. Chaque spike répond à une question d'ADR :

| Répertoire | Question | ADR alimenté | Résultats |
| --- | --- | --- | --- |
| `rope/` | la rope du dépôt de départ est-elle portable et dans quel budget ? | ADR-003 | `results/rope.txt` |
| `syntaxe/` | coût d'une re-lexicographie incrémentale après un caractère ? | ADR-004 | `results/syntaxe.txt` |
| `rendu/` | ordre de grandeur du coût de mise en page de ligne (proxy JVM) ? | ADR-006 | `results/rendu.txt` |

Le spike rope compile la classe du dépôt de départ **depuis le clone en
lecture seule** (chemin passé en argument) : aucune copie n'est committée
ici avant la validation du propriétaire.
