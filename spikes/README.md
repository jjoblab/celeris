# Spikes de mesure (M0)

Code jetable de mesure : il ne deviendra jamais du code de production et
n'est pas publié. Chaque spike répond à une question d'ADR :

| Répertoire | Question | ADR alimenté | Résultats |
| --- | --- | --- | --- |
| `rope/` | la rope du dépôt de départ est-elle portable et dans quel budget ? | ADR-003 | `results/rope.txt` |
| `syntaxe/` | coût d'une re-lexicographie incrémentale après un caractère ? | ADR-004 | `results/syntaxe.txt` |
| `rendu/` | ordre de grandeur du coût de mise en page de ligne (proxy JVM) ? | ADR-006 | `results/rendu.txt` |
| `regex/` | traduction vers `java.util.regex` ou joni pour les grammaires réelles ? (M7.6) | ADR-014 | `regex/results/regex.txt` |
| `lsp4j/` | LSP4J 1.0.0 peut-il lancer LemMinX 0.31.2 et que coûte la voie ? (M7.6) | ADR-015 | `lsp4j/results/essai-1.0.0.txt` |

Le spike rope compile la classe du dépôt de départ **depuis le clone en
lecture seule** (chemin passé en argument) : aucune copie n'est committée
ici avant la validation du propriétaire.
