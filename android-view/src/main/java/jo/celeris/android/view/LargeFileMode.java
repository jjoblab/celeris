package jo.celeris.android.view;

/**
 * Le régime des fonctions lourdes de l'éditeur (chantier C10) : sur un
 * gros document — au-delà des seuils « gros document » du dépôt de
 * départ, 2,5 M de caractères ou 50 000 lignes — les fonctions qui
 * balayent le document se coupent, l'édition reste entière :
 * coloration, frappe et annulation ne se coupent jamais.
 *
 * <p>Tri-état à la manière du dépôt de départ : AUTO suit les seuils
 * du document, ON force les coupures, OFF les ferme même au-delà des
 * seuils. La voie explicite de l'hôte reste ouverte —
 * {@code editorView.intelligence().setLargeFileMode(...)}.</p>
 */
public enum LargeFileMode {

    /** Coupures pilotées par les seuils du document (défaut). */
    AUTO,

    /** Force les coupures, quel que soit le document. */
    ON,

    /** Force les coupures fermées, même au-delà des seuils. */
    OFF
}
