/*
 * Corpus C — les conversions d'unités d'un éditeur embarquable.
 * Les plis s'essaient sur ce bloc, puis sur le corps des fonctions.
 */
#include <stdio.h>
#include <stdint.h>

#define SEUIL 0.5f
#define MAX_NOM 64

typedef struct {
    char nom[MAX_NOM];
    int32_t largeur;
    int32_t hauteur;
} Mesure;

static const char *PREFIXE = "px";

/* Conversion dp vers px, seuil arrondi à l'entier supérieur. */
int32_t dp_vers_px(float dp, float densite) {
    return (int32_t)(dp * densite + SEUIL);
}

size_t aire(const Mesure *m) {
    if (m == NULL) {
        return 0;
    }
    return (size_t)(m->largeur * m->hauteur);
}

int main(int argc, char **argv) {
    Mesure m = {"atelier", 1080, 1920};
    float densite = 2.75f;
    int32_t px = dp_vers_px(16.0f, densite);
    printf("%s : %d, aire %zu\n", PREFIXE, px, aire(&m));
    for (int i = 0; i < argc; i++) {
        printf("argv[%d] = %s\n", i, argv[i]);
    }
    return 0;
}
