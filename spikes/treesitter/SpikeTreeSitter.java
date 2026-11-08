// SPDX-License-Identifier: Apache-2.0
package spike.treesitter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spike tree-sitter (M7.6, alimente l'ADR-016) : le niveau 2 de
 * l'analyse structurelle vaut-il son poids ? La liaison JNI (fichier
 * {@code liaison.c} du même répertoire) est volontairement MINIMALE :
 * parser, éditer, re-parser depuis l'ancien arbre, compter — jamais
 * de JNI dans un chemin de rendu (règle du jalon, tenue par
 * construction : le spike matérialise tout en Java).
 *
 * <p>Mesures : analyse complète de 50 000 lignes (min de 3 tours,
 * PIEGE-13), re-parse incrémental après insertion d'un caractère
 * (p50/p95 sur 1 000 éditions), mémoire résidente avant/après.
 * Corpus : les mêmes gabarits JSON que le spike du moteur
 * d'expressions rationnelles — les deux niveaux se comparent sur les
 * mêmes données.</p>
 */
public final class SpikeTreeSitter {

    static {
        System.load(Path.of("spikes/treesitter/out/libspiketreesitter.so")
                .toAbsolutePath().toString());
    }

    private static final int LIGNES = 50_000;
    private static final int EDITIONS = 1_000;
    private static final int TOURS = 3;

    private static final int JSON = 0;
    private static final int PYTHON = 1;
    private static final int JAVASCRIPT = 2;

    // natives : voir liaison.c
    private static native long parserNew(int index);

    private static native long parse(long parser, byte[] source, int longueur);

    private static native long reparse(long parser, long ancien, byte[] source, int longueur);

    private static native void treeEdit(long arbre, int debutOctet, int ancienneFin,
                                        int nouvelleFin, int ligne, int colonne,
                                        int ancienneFinLigne, int ancienneFinColonne,
                                        int nouvelleFinLigne, int nouvelleFinColonne);

    private static native int nodeCount(long arbre);

    private static native boolean hasError(long arbre);

    private static native int rootEndByte(long arbre);

    private static native void treeFree(long arbre);

    private static native void parserFree(long parser);

    public static void main(String[] args) {
        System.out.println("== Spike tree-sitter v0.27.2 (M7.6) ==");
        for (int langue : new int[]{JSON, PYTHON, JAVASCRIPT}) {
            mesurer(langue);
        }
    }

    private static void mesurer(int langue) {
        String nom = langue == JSON ? "json" : langue == PYTHON ? "python" : "javascript";
        List<String> lignes = corpus(langue, LIGNES);
        byte[] octets = String.join("\n", lignes).getBytes(StandardCharsets.UTF_8);

        long parser = parserNew(langue);
        if (parser == 0) {
            System.out.println(nom + " : langue refusée par le runtime (ABI)");
            return;
        }

        // 1. Analyse complète, min de 3 tours.
        long meilleur = Long.MAX_VALUE;
        int noeuds = 0;
        for (int tour = 0; tour < TOURS; tour++) {
            long t0 = System.nanoTime();
            long arbre = parse(parser, octets, octets.length);
            long t1 = System.nanoTime();
            if (tour == 0) {
                noeuds = nodeCount(arbre);
            }
            treeFree(arbre);
            meilleur = Math.min(meilleur, t1 - t0);
        }
        System.out.printf("%-11s analyse complète de %d lignes (%d octets) : %d ms "
                        + "(min de %d) — %d nœuds, erreur : %s, racine finit à l'octet %d%n",
                nom, LIGNES, octets.length, meilleur / 1_000_000, TOURS, noeuds,
                hasError(parse(parser, octets, octets.length)),
                rootEndByte(parse(parser, octets, octets.length)));

        // 2. Incrémental : insertion d'un caractère au milieu d'une valeur,
        //    1 000 éditions, re-parse depuis l'ancien arbre.
        Random alea = new Random(20261108L);
        long[] durees = new long[EDITIONS];
        long arbre = parse(parser, octets, octets.length);
        byte[] travail = octets.clone();
        for (int i = 0; i < EDITIONS; i++) {
            int position = 1 + alea.nextInt(travail.length - 2);
            // point (ligne, colonne) de la position et des fins
            int ligne = 0;
            int colonne = 0;
            for (int p = 0; p < position; p++) {
                if (travail[p] == '\n') {
                    ligne++;
                    colonne = 0;
                } else {
                    colonne++;
                }
            }
            byte[] edite = new byte[travail.length + 1];
            System.arraycopy(travail, 0, edite, 0, position);
            edite[position] = 'x';
            System.arraycopy(travail, position, edite, position + 1, travail.length - position);
            travail = edite;
            long t0 = System.nanoTime();
            treeEdit(arbre, position, position, position + 1,
                    ligne, colonne, ligne, colonne, ligne, colonne + 1);
            long nouvelArbre = reparse(parser, arbre, travail, travail.length);
            long t1 = System.nanoTime();
            durees[i] = t1 - t0;
            treeFree(arbre);
            arbre = nouvelArbre;
        }
        java.util.Arrays.sort(durees);
        System.out.printf("%-11s incrémental après insertion d'un caractère : "
                        + "p50 %.1f ms, p95 %.1f ms, max %.1f ms (%d éditions)%n",
                nom, durees[durees.length / 2] / 1e6,
                durees[(int) (durees.length * 0.95)] / 1e6,
                durees[EDITIONS - 1] / 1e6, EDITIONS);

        // 3. Mémoire résidente (VmRSS, /proc) avant/après analyse.
        long avant = rssKo();
        long arbreGros = parse(parser, octets, octets.length);
        long apres = rssKo();
        System.out.printf("%-11s mémoire résidente de l'arbre : %.1f Mo (RSS %.1f → %.1f Mo)%n",
                nom, (apres - avant) / 1024.0, avant / 1024.0, apres / 1024.0);
        treeFree(arbreGros);
        parserFree(parser);
    }

    private static long rssKo() {
        try {
            for (String ligne : Files.readAllLines(Path.of("/proc/self/status"))) {
                if (ligne.startsWith("VmRSS:")) {
                    return Long.parseLong(ligne.replaceAll("[^0-9]", ""));
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    /**
     * Corpus VALIDES par langage : les gabarits se répètent en blocs
     * fermés (un document aléatoire non fermé produirait des arbres
     * d'erreur pathologiques — la récupération serait alors la mesure,
     * pas l'analyse).
     */
    static List<String> corpus(int langue, int lignes) {
        String bloc = switch (langue) {
            case JSON -> """
                { "nom": "valeur", "compte": 42, "actif": true, "nul": null,
                  "liste": [1, 2.5e3, "x"], "imbrique": { "a": -7, "b": [true, false] },
                  "chemin": "src/main/kt" },""";
            case PYTHON -> """
                class Exemple:
                    def faire(self, valeur):
                        total = valeur * 3
                        return f"total={total}"

                    @staticmethod
                    def util():
                        # commentaire
                        x = [i for i in range(10)]
                        if x and valeur is not None:
                            raise ValueError('non')
                        return x
                from pathlib import Path
                import re, json
                """;
            default -> """
                import { serveur } from './module.js';
                const reponse = await fetch('/api');
                function calculer(a, b = 2) {
                  return a * b;
                }
                class Depot {
                  constructor(nom) { this.nom = nom; }
                }
                const liste = [1, 2, 3].map(x => x * 2);
                export default calculer;
                """;
        };
        String[] lignesBloc = bloc.split("\n", -1);
        List<String> sortie = new ArrayList<>(lignes);
        int i = 0;
        while (sortie.size() < lignes) {
            sortie.add(lignesBloc[i % lignesBloc.length]);
            i++;
        }
        return sortie;
    }

    private SpikeTreeSitter() {
    }
}
