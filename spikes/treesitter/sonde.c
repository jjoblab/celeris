// SPDX-License-Identifier: Apache-2.0
// Instrumentation du spike tree-sitter : petit corpus VALIDE (un
// tableau JSON d'objets), éditions une à une — où va le temps ?
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "tree_sitter/api.h"

const TSLanguage *tree_sitter_json(void);

static double maintenant_ms(void) {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec * 1e3 + t.tv_nsec / 1e6;
}

int main(void) {
    // Un tableau JSON valide de 1 200 objets (environ 4 800 lignes).
    size_t capacite = 4u << 20;
    char *texte = malloc(capacite);
    size_t longueur = 1;
    texte[0] = '[';
    const char *objet = "{ \"nom\": \"valeur\", \"compte\": 42, \"actif\": true, \"nul\": null, "
            "\"liste\": [1, 2.5e3, \"x\"], \"imbrique\": { \"a\": -7, \"b\": [true, false] }, "
            "\"chemin\": \"src/main/kt\" },\n";
    for (int i = 0; i < 1200; i++) {
        size_t n = strlen(objet);
        memcpy(texte + longueur, objet, n);
        longueur += n;
    }
    longueur -= 2; /* la virgule et le saut de ligne du dernier objet */
    memcpy(texte + longueur, "]\n", 2);
    longueur += 2;

    TSParser *parser = ts_parser_new();
    ts_parser_set_language(parser, tree_sitter_json());

    double t0 = maintenant_ms();
    TSTree *arbre = ts_parser_parse_string(parser, NULL, texte, (uint32_t)longueur);
    double t1 = maintenant_ms();
    TSNode racine = ts_tree_root_node(arbre);
    printf("analyse complète : %.1f ms (%zu octets), erreur : %s\n",
            t1 - t0, longueur, ts_node_has_error(racine) ? "OUI" : "non");

    srand(20261108);
    for (int i = 0; i < 100; i++) {
        size_t position = 14; /* dans « valeur », première chaîne : jamais d'erreur */
        memmove(texte + position + 1, texte + position, longueur - position);
        texte[position] = 'x';
        longueur++;
        uint32_t ligne = 0, colonne = 0;
        for (size_t p = 0; p < position; p++) {
            if (texte[p] == '\n') {
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
        double a = maintenant_ms();
        ts_tree_edit(arbre, &edition);
        double b = maintenant_ms();
        TSTree *nouveau = (i % 2 == 0)
                ? ts_parser_parse_string(parser, arbre, texte, (uint32_t)longueur)
                : ts_parser_parse_string(parser, NULL, texte, (uint32_t)longueur);
        double c = maintenant_ms();
        printf("édition %2d à l'octet %7zu : tree_edit %.3f ms, re-parse %.3f ms (%s), "
                "erreur : %s\n",
                i, position, b - a, c - b, i % 2 == 0 ? "AVEC ancien" : "SANS ancien",
                ts_node_has_error(ts_tree_root_node(nouveau)) ? "OUI" : "non");
        ts_tree_delete(arbre);
        arbre = nouveau;
    }
    return 0;
}
