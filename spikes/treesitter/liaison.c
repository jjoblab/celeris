// SPDX-License-Identifier: Apache-2.0
//
// Liaison JNI minimale du spike M7.6 (tree-sitter, alimente l'ADR-016).
// Volontairement fine : créer un analyseur, parser, éditer l'arbre,
// re-parser depuis l'ancien arbre, compter les nœuds, libérer. Rien
// d'autre — la vraie liaison du jalon (si l'ADR la retient) vivra dans
// un module natif à part, construit par CMake et le NDK ; ce fichier
// sert la mesure, pas la conception.
#include <jni.h>
#include <stdlib.h>
#include "tree_sitter/api.h"

const TSLanguage *tree_sitter_json(void);
const TSLanguage *tree_sitter_python(void);
const TSLanguage *tree_sitter_javascript(void);

static const TSLanguage *langue(int index) {
    switch (index) {
        case 0: return tree_sitter_json();
        case 1: return tree_sitter_python();
        default: return tree_sitter_javascript();
    }
}

JNIEXPORT jlong JNICALL Java_spike_treesitter_SpikeTreeSitter_parserNew(
        JNIEnv *env, jclass cls, jint index) {
    (void)env;
    (void)cls;
    TSParser *parser = ts_parser_new();
    if (!ts_parser_set_language(parser, langue(index))) {
        ts_parser_delete(parser);
        return 0;
    }
    return (jlong)(intptr_t)parser;
}

JNIEXPORT jlong JNICALL Java_spike_treesitter_SpikeTreeSitter_parse(
        JNIEnv *env, jclass cls, jlong parser, jbyteArray source, jint longueur) {
    (void)cls;
    jbyte *octets = (*env)->GetByteArrayElements(env, source, NULL);
    TSTree *arbre = ts_parser_parse_string(
            (TSParser *)(intptr_t)parser, NULL,
            (const char *)octets, (uint32_t)longueur);
    (*env)->ReleaseByteArrayElements(env, source, octets, JNI_ABORT);
    return (jlong)(intptr_t)arbre;
}

JNIEXPORT jlong JNICALL Java_spike_treesitter_SpikeTreeSitter_reparse(
        JNIEnv *env, jclass cls, jlong parser, jlong ancien,
        jbyteArray source, jint longueur) {
    (void)cls;
    jbyte *octets = (*env)->GetByteArrayElements(env, source, NULL);
    TSTree *arbre = ts_parser_parse_string(
            (TSParser *)(intptr_t)parser, (const TSTree *)(intptr_t)ancien,
            (const char *)octets, (uint32_t)longueur);
    (*env)->ReleaseByteArrayElements(env, source, octets, JNI_ABORT);
    return (jlong)(intptr_t)arbre;
}

JNIEXPORT void JNICALL Java_spike_treesitter_SpikeTreeSitter_treeEdit(
        JNIEnv *env, jclass cls, jlong arbre,
        jint debutOctet, jint ancienneFin, jint nouvelleFin,
        jint ligne, jint colonne,
        jint ancienneFinLigne, jint ancienneFinColonne,
        jint nouvelleFinLigne, jint nouvelleFinColonne) {
    (void)env;
    (void)cls;
    TSInputEdit edition = {
        .start_byte = (uint32_t)debutOctet,
        .old_end_byte = (uint32_t)ancienneFin,
        .new_end_byte = (uint32_t)nouvelleFin,
        .start_point = { (uint32_t)ligne, (uint32_t)colonne },
        .old_end_point = { (uint32_t)ancienneFinLigne, (uint32_t)ancienneFinColonne },
        .new_end_point = { (uint32_t)nouvelleFinLigne, (uint32_t)nouvelleFinColonne },
    };
    ts_tree_edit((TSTree *)(intptr_t)arbre, &edition);
}

/** Nombre de nœuds par curseur — la preuve que l'arbre existe et vit. */
JNIEXPORT jint JNICALL Java_spike_treesitter_SpikeTreeSitter_nodeCount(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    TSNode racine = ts_tree_root_node((const TSTree *)(intptr_t)arbre);
    TSTreeCursor curseur = ts_tree_cursor_new(racine);
    jint total = 0;
    int descendre = 1;
    while (descendre) {
        total++;
        if (ts_tree_cursor_goto_first_child(&curseur)) {
            continue;
        }
        while (!ts_tree_cursor_goto_next_sibling(&curseur)) {
            if (!ts_tree_cursor_goto_parent(&curseur)) {
                descendre = 0;
                break;
            }
        }
    }
    ts_tree_cursor_delete(&curseur);
    return total;
}

JNIEXPORT jboolean JNICALL Java_spike_treesitter_SpikeTreeSitter_hasError(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    TSNode racine = ts_tree_root_node((const TSTree *)(intptr_t)arbre);
    return ts_node_has_error(racine) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_spike_treesitter_SpikeTreeSitter_rootEndByte(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    TSNode racine = ts_tree_root_node((const TSTree *)(intptr_t)arbre);
    return (jint)ts_node_end_byte(racine);
}

JNIEXPORT void JNICALL Java_spike_treesitter_SpikeTreeSitter_treeFree(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    ts_tree_delete((TSTree *)(intptr_t)arbre);
}

JNIEXPORT void JNICALL Java_spike_treesitter_SpikeTreeSitter_parserFree(
        JNIEnv *env, jclass cls, jlong parser) {
    (void)env;
    (void)cls;
    ts_parser_delete((TSParser *)(intptr_t)parser);
}
