// SPDX-License-Identifier: Apache-2.0
// Benchmark C pur du spike tree-sitter (M7.6) : analyse complète puis
// 1 000 re-parses incrémentaux après insertion d'un caractère, sans
// JVM ni JNI — sépare le coût du moteur de celui de la liaison.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "tree_sitter/api.h"

const TSLanguage *tree_sitter_json(void);
const TSLanguage *tree_sitter_python(void);
const TSLanguage *tree_sitter_javascript(void);

static double maintenant_ms(void) {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec * 1e3 + t.tv_nsec / 1e6;
}

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "usage: %s <json|python|javascript> <fichier-corpus>\n", argv[0]);
        return 2;
    }
    const TSLanguage *(*langue)(void) = strcmp(argv[1], "json") == 0
            ? tree_sitter_json
            : strcmp(argv[1], "python") == 0 ? tree_sitter_python : tree_sitter_javascript;

    FILE *f = fopen(argv[2], "rb");
    if (!f) {
        perror("fopen");
        return 2;
    }
    fseek(f, 0, SEEK_END);
    long taille = ftell(f);
    fseek(f, 0, SEEK_SET);
    char *texte = malloc((size_t)taille + 1);
    if (fread(texte, 1, (size_t)taille, f) != (size_t)taille) {
        return 2;
    }
    fclose(f);

    TSParser *parser = ts_parser_new();
    ts_parser_set_language(parser, langue());

    // Analyse complète, min de 3 tours.
    double meilleur = 1e9;
    TSTree *arbre = NULL;
    for (int tour = 0; tour < 3; tour++) {
        double t0 = maintenant_ms();
        TSTree *essai = ts_parser_parse_string(parser, NULL, texte, (uint32_t)taille);
        double t1 = maintenant_ms();
        if (tour == 0 || essai) {
            ts_tree_delete(essai);
        }
        if (t1 - t0 < meilleur) {
            meilleur = t1 - t0;
        }
    }
    arbre = ts_parser_parse_string(parser, NULL, texte, (uint32_t)taille);
    TSNode racine = ts_tree_root_node(arbre);
    printf("%s : analyse complète %.1f ms (%ld octets), erreur : %s, fin à %u\n",
            argv[1], meilleur, taille, ts_node_has_error(racine) ? "oui" : "non",
            ts_node_end_byte(racine));

    // Incrémental : insertions d'un caractère, re-parse. Le mode (3e
    // argument) : « fixe » édite toujours à l'octet 14 (plancher sans
    // erreur) ; « aleatoire » (défaut) insère à une position quelconque —
    // l'éditeur réel tape au milieu de documents imparfaits.
    int editions = argc > 3 ? atoi(argv[3]) : 1000;
    int positionFixe = argc > 4 && strcmp(argv[4], "fixe") == 0;
    char *travail = malloc((size_t)taille + 4096);
    memcpy(travail, texte, (size_t)taille);
    long longueur = taille;
    srand(20261108);
    double *durees = malloc(sizeof(double) * editions);
    for (int i = 0; i < editions; i++) {
        long position = positionFixe ? 14 : 1 + rand() % (int)(longueur - 2);
        // décaler d'un octet vers la droite
        memmove(travail + position + 1, travail + position, (size_t)(longueur - position));
        travail[position] = 'x';
        longueur++;
        // points (ASCII : colonne = octets depuis le dernier saut de ligne)
        uint32_t ligne = 0, colonne = 0;
        for (long p = 0; p < position; p++) {
            if (travail[p] == '\n') {
                ligne++;
                colonne = 0;
            } else {
                colonne++;
            }
        }
        TSInputEdit edition = {
            .start_byte = (uint32_t)position,
            .old_end_byte = (uint32_t)position,
            .new_end_byte = (uint32_t)position + 1,
            .start_point = { ligne, colonne },
            .old_end_point = { ligne, colonne },
            .new_end_point = { ligne, colonne + 1 },
        };
        double t0 = maintenant_ms();
        ts_tree_edit(arbre, &edition);
        TSTree *nouveau = ts_parser_parse_string(parser, arbre, travail, (uint32_t)longueur);
        double t1 = maintenant_ms();
        durees[i] = t1 - t0;
        ts_tree_delete(arbre);
        arbre = nouveau;
    }
    // tri simple des durées
    for (int i = 0; i < editions; i++) {
        for (int j = i + 1; j < editions; j++) {
            if (durees[j] < durees[i]) {
                double tmp = durees[i];
                durees[i] = durees[j];
                durees[j] = tmp;
            }
        }
    }
    printf("%s : incrémental %s p50 %.3f ms, p95 %.3f ms, max %.3f ms (%d éditions)\n",
            argv[1], positionFixe ? "fixe (sans erreur)" : "aléatoire (frappe réelle)",
            durees[editions / 2], durees[(int)(editions * 0.95)], durees[editions - 1], editions);
    return 0;
}
