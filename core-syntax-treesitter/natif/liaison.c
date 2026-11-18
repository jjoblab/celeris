// SPDX-License-Identifier: Apache-2.0
//
// Liaison JNI du niveau 2 (ADR-020 — la révision de l'ADR-016 sur
// signal du propriétaire) : créer un analyseur, parser, éditer l'arbre,
// re-parser depuis l'ancien, APLATIR l'arbre entier en un tampon
// d'entiers (symbole, octets de début et de fin, point de début, nombre
// d'enfants — la matérialisation Java lit TOUT, l'arbre natif ne survit
// qu'entre deux éditions de la voie bornée), libérer. Volontairement
// fine : la bibliothèque interdite ne touche jamais ce fichier (règle
// 7), la technologie se nomme dans la documentation, jamais ici.
#include <jni.h>
#include <stdlib.h>
#include <string.h>
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

JNIEXPORT jlong JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_parserNew(
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

/** La table des symboles du langage — leurs noms, dans l'ordre. */
JNIEXPORT jobjectArray JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_symboles(
        JNIEnv *env, jclass cls, jlong parser) {
    (void)cls;
    const TSLanguage *lang = ts_parser_language((TSParser *)(intptr_t)parser);
    uint32_t nombre = ts_language_symbol_count(lang);
    jobjectArray noms = (*env)->NewObjectArray(env, (jsize)nombre,
            (*env)->FindClass(env, "java/lang/String"),
            (*env)->NewStringUTF(env, ""));
    for (uint32_t i = 0; i < nombre; i++) {
        const char *nom = ts_language_symbol_name(lang, (TSSymbol)i);
        if (nom == NULL) {
            nom = "";
        }
        (*env)->SetObjectArrayElement(env, noms, (jsize)i,
                (*env)->NewStringUTF(env, nom));
    }
    return noms;
}

JNIEXPORT jlong JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_parse(
        JNIEnv *env, jclass cls, jlong parser, jbyteArray source, jint longueur) {
    (void)cls;
    jbyte *octets = (*env)->GetByteArrayElements(env, source, NULL);
    TSTree *arbre = ts_parser_parse_string(
            (TSParser *)(intptr_t)parser, NULL,
            (const char *)octets, (uint32_t)longueur);
    (*env)->ReleaseByteArrayElements(env, source, octets, JNI_ABORT);
    return (jlong)(intptr_t)arbre;
}

JNIEXPORT jlong JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_reparse(
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

JNIEXPORT void JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_treeEdit(
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

/** L'arbre entier aplati, en préordre : six entiers par nœud — le
 *  symbole, l'octet de début, l'octet de fin (exclusif), la ligne de
 *  début, la colonne de début (en octets), le nombre d'enfants. La
 *  matérialisation Java reconstruit la hiérarchie de la pile des
 *  enfants : tout est matérialisé, rien ne survit côté natif. */
JNIEXPORT jintArray JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_aplatir(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)cls;
    TSNode racine = ts_tree_root_node((const TSTree *)(intptr_t)arbre);
    size_t capacite = 6 * 1024;
    jint *tampon = malloc(capacite * sizeof(jint));
    size_t nombre = 0;
    TSTreeCursor curseur = ts_tree_cursor_new(racine);
    int descendre = 1;
    while (descendre) {
        TSNode noeud = ts_tree_cursor_current_node(&curseur);
        if (nombre + 6 > capacite) {
            capacite *= 2;
            tampon = realloc(tampon, capacite * sizeof(jint));
        }
        TSPoint point = ts_node_start_point(noeud);
        tampon[nombre++] = (jint)ts_node_symbol(noeud);
        tampon[nombre++] = (jint)ts_node_start_byte(noeud);
        tampon[nombre++] = (jint)ts_node_end_byte(noeud);
        tampon[nombre++] = (jint)point.row;
        tampon[nombre++] = (jint)point.column;
        tampon[nombre++] = (jint)ts_node_child_count(noeud);
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
    jintArray resultat = (*env)->NewIntArray(env, (jsize)nombre);
    (*env)->SetIntArrayRegion(env, resultat, 0, (jsize)nombre, tampon);
    free(tampon);
    return resultat;
}

JNIEXPORT jboolean JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_hasError(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    TSNode racine = ts_tree_root_node((const TSTree *)(intptr_t)arbre);
    return ts_node_has_error(racine) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_treeFree(
        JNIEnv *env, jclass cls, jlong arbre) {
    (void)env;
    (void)cls;
    ts_tree_delete((TSTree *)(intptr_t)arbre);
}

JNIEXPORT void JNICALL Java_jo_celeris_core_syntax_LiaisonArbre_parserFree(
        JNIEnv *env, jclass cls, jlong parser) {
    (void)env;
    (void)cls;
    ts_parser_delete((TSParser *)(intptr_t)parser);
}
