# Règles de conservation de la démonstration (ADR-015) — côté
# terrain. Un serveur de langage embarqué monté PAR DÉSIGNATION s'y
# lance : la classe du serveur et l'interface cliente se nomment en
# toutes lettres chez ServeurEmbarque.monter, l'instanciation et le
# proxy passent par la réflexion — invisibles au réducteur.
#
# Les règles du serveur vivent avec lui : chaque serveur déclaré par
# l'hôte apporte ici ses propres -keep. Celles de la frontière
# protocolaire (rouage JSON réflexif, proxys, TypeToken) vivent dans
# lsp-protocole/regles-conservation.pro, côté consommateur.
