package jo.celeris.android.widgets;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Les pictogrammes des actions des popups (l'audit Android Studio du
 * propriétaire du 2026-11-14 : « tu as utilisé la même icône pour
 * tout ») : chaque action de la barre de sélection, du menu
 * d'intentions et des débordements porte SON glyphe — les ciseaux de
 * la coupe, les pages de la copie, le presse-papiers du collage,
 * l'ampoule de l'intention SEULEMENT (l'IDE de référence la réserve
 * aux correctifs, jamais elle ne marque toutes les rangées).
 *
 * <p>Le tracé tient en traits, cercles et arcs du pinceau de ligne —
 * la graisse du trait se dérive du rayon, les couleurs se posent par
 * l'appelant avant le tracé. Pur dessin : aucune mesure ne s'y
 * calcule, la respiration vit chez le panneau.</p>
 */
public enum IconeAction {

    /** Les ciseaux — la coupe de la sélection. */
    COUPER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx - r * 0.55f, cy - r * 0.5f, r * 0.24f, trait);
            c.drawCircle(cx - r * 0.55f, cy + r * 0.5f, r * 0.24f, trait);
            c.drawLine(cx - r * 0.35f, cy - r * 0.4f, cx + r * 0.75f, cy + r * 0.5f, trait);
            c.drawLine(cx - r * 0.35f, cy + r * 0.4f, cx + r * 0.75f, cy - r * 0.5f, trait);
        }
    },

    /** Les deux pages — la copie. */
    COPIER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.65f, cy - r * 0.95f, cx + r * 0.3f, cy + r * 0.05f, trait);
            c.drawRect(cx - r * 0.3f, cy - r * 0.05f, cx + r * 0.65f, cy + r * 0.95f, trait);
        }
    },

    /** Le presse-papiers — le collage (la planche et sa pince). */
    COLLER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.6f, cy - r * 0.7f, cx + r * 0.6f, cy + r * 0.95f, trait);
            c.drawRect(cx - r * 0.3f, cy - r, cx + r * 0.3f, cy - r * 0.7f, trait);
            c.drawLine(cx - r * 0.25f, cy, cx + r * 0.25f, cy, trait);
        }
    },

    /** Les quatre coins — tout sélectionner. */
    TOUT {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            coins(c, trait, cx, cy, r);
        }
    },

    /** Les pages jumelles et leur signe — la duplication. */
    DUPLIQUER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.25f, cy + r * 0.25f, trait);
            c.drawRect(cx - r * 0.25f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.75f, trait);
            c.drawLine(cx - r * 0.05f, cy + r * 0.25f, cx + r * 0.45f, cy + r * 0.25f, trait);
            c.drawLine(cx + r * 0.2f, cy, cx + r * 0.2f, cy + r * 0.5f, trait);
        }
    },

    /** Les deux barres — le commentaire de ligne. */
    COMMENTER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            dessinerTexte(c, texte, "//", cx, cy, r);
        }
    },

    /** Les grandes capitales — tout en majuscules. */
    MAJUSCULES {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            dessinerTexte(c, texte, "AA", cx, cy, r);
        }
    },

    /** Les petites basses — tout en minuscules. */
    MINUSCULES {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            dessinerTexte(c, texte, "aa", cx, cy, r);
        }
    },

    /** Le chevron et les lignes — l'indentation d'un cran. */
    INDENTER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r, cy - r * 0.55f, cx - r * 0.5f, cy, trait);
            c.drawLine(cx - r, cy + r * 0.55f, cx - r * 0.5f, cy, trait);
            c.drawLine(cx - r * 0.3f, cy - r * 0.55f, cx + r * 0.75f, cy - r * 0.55f, trait);
            c.drawLine(cx - r * 0.3f, cy, cx + r * 0.75f, cy, trait);
            c.drawLine(cx - r * 0.3f, cy + r * 0.55f, cx + r * 0.75f, cy + r * 0.55f, trait);
        }
    },

    /** Le curseur inséré et sa liste — la complétion. */
    COMPLETER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.55f, cy - r, cx - r * 0.55f, cy + r, trait);
            c.drawLine(cx - r * 0.85f, cy - r, cx - r * 0.25f, cy - r, trait);
            c.drawLine(cx - r * 0.85f, cy + r, cx - r * 0.25f, cy + r, trait);
            c.drawLine(cx - r * 0.05f, cy - r * 0.45f, cx + r * 0.8f, cy - r * 0.45f, trait);
            c.drawLine(cx - r * 0.05f, cy + r * 0.45f, cx + r * 0.8f, cy + r * 0.45f, trait);
        }
    },

    /** L'ampoule — l'intention au curseur, l'icône de l'IDE de
     *  référence : elle marque l'entrée du menu, JAMAIS toutes les
     *  rangées (l'audit du propriétaire). */
    INTENTIONS {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx - r * 0.15f, cy - r * 0.3f, r * 0.55f, trait);
            c.drawLine(cx - r * 0.45f, cy + r * 0.15f, cx + r * 0.15f, cy + r * 0.15f, trait);
            c.drawLine(cx - r * 0.45f, cy + r * 0.45f, cx + r * 0.15f, cy + r * 0.45f, trait);
            c.drawLine(cx + r * 0.55f, cy - r * 0.9f, cx + r * 0.75f, cy - r, trait);
            c.drawLine(cx + r * 0.8f, cy - r * 0.55f, cx + r, cy - r * 0.65f, trait);
        }
    },

    /** L'œil — le survol de documentation. */
    SURVOL {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            oeil(c, trait, cx, cy, r, true);
        }
    },

    /** Le cadre et sa flèche entrante — l'aperçu de définition. */
    APERCU {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.85f, cy - r * 0.45f, cx + r * 0.45f, cy + r * 0.6f, trait);
            c.drawLine(cx + r * 0.05f, cy - r * 0.85f, cx + r * 0.8f, cy - r * 0.1f, trait);
            c.drawLine(cx + r * 0.35f, cy - r * 0.85f, cx + r * 0.05f, cy - r * 0.85f, trait);
            c.drawLine(cx + r * 0.8f, cy - r * 0.4f, cx + r * 0.8f, cy - r * 0.1f, trait);
        }
    },

    /** La loupe au point — la recherche des usages. */
    USAGES {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx - r * 0.15f, cy - r * 0.15f, r * 0.55f, trait);
            c.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.85f, cy + r * 0.85f, trait);
            c.drawCircle(cx - r * 0.15f, cy - r * 0.15f, r * 0.12f, trait);
        }
    },

    /** Le disque et son i — l'info de type. */
    TYPE_INFO {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r * 0.9f, trait);
            dessinerTexte(c, texte, "i", cx, cy, r);
        }
    },

    /** L'arbre — la hiérarchie de types ou d'appels. */
    HIERARCHIE {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx, cy - r * 0.8f, r * 0.18f, trait);
            c.drawLine(cx, cy - r * 0.6f, cx, cy - r * 0.2f, trait);
            c.drawLine(cx, cy - r * 0.2f, cx - r * 0.8f, cy + r * 0.2f, trait);
            c.drawLine(cx, cy - r * 0.2f, cx + r * 0.8f, cy + r * 0.2f, trait);
            c.drawCircle(cx - r * 0.8f, cy + r * 0.4f, r * 0.18f, trait);
            c.drawCircle(cx + r * 0.8f, cy + r * 0.4f, r * 0.18f, trait);
        }
    },

    /** Le coude et sa flèche — l'aller à. */
    ALLER_A {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r, cy - r * 0.6f, cx + r * 0.3f, cy - r * 0.6f, trait);
            c.drawLine(cx + r * 0.3f, cy - r * 0.6f, cx + r * 0.3f, cy + r * 0.7f, trait);
            c.drawLine(cx + r * 0.05f, cy + r * 0.4f, cx + r * 0.3f, cy + r * 0.7f, trait);
            c.drawLine(cx + r * 0.55f, cy + r * 0.4f, cx + r * 0.3f, cy + r * 0.7f, trait);
        }
    },

    /** Le livre ouvert — la documentation externe. */
    DOC_EXTERNE {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx, cy - r * 0.8f, cx, cy + r * 0.8f, trait);
            c.drawLine(cx, cy - r * 0.8f, cx - r * 0.85f, cy - r * 0.5f, trait);
            c.drawLine(cx - r * 0.85f, cy - r * 0.5f, cx - r * 0.85f, cy + r * 0.5f, trait);
            c.drawLine(cx - r * 0.85f, cy + r * 0.5f, cx, cy + r * 0.8f, trait);
            c.drawLine(cx, cy - r * 0.8f, cx + r * 0.85f, cy - r * 0.5f, trait);
            c.drawLine(cx + r * 0.85f, cy - r * 0.5f, cx + r * 0.85f, cy + r * 0.5f, trait);
            c.drawLine(cx + r * 0.85f, cy + r * 0.5f, cx, cy + r * 0.8f, trait);
        }
    },

    /** La baguette et ses étincelles — la génération de code. */
    GENERER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.75f, cy + r * 0.75f, cx + r * 0.5f, cy - r * 0.5f, trait);
            croix(c, trait, cx + r * 0.25f, cy - r * 0.85f, r * 0.16f);
            croix(c, trait, cx + r * 0.75f, cy - r * 0.35f, r * 0.16f);
        }
    },

    /** La poubelle — la suppression sûre. */
    SUPPRIMER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.65f, cy - r * 0.45f, cx + r * 0.65f, cy - r * 0.45f, trait);
            c.drawLine(cx - r * 0.35f, cy - r * 0.45f, cx - r * 0.35f, cy - r * 0.95f, trait);
            c.drawLine(cx + r * 0.35f, cy - r * 0.45f, cx + r * 0.35f, cy - r * 0.95f, trait);
            c.drawLine(cx - r * 0.35f, cy - r * 0.95f, cx + r * 0.35f, cy - r * 0.95f, trait);
            c.drawRect(cx - r * 0.5f, cy - r * 0.45f, cx + r * 0.5f, cy + r * 0.95f, trait);
            c.drawLine(cx - r * 0.15f, cy - r * 0.25f, cx - r * 0.15f, cy + r * 0.7f, trait);
            c.drawLine(cx + r * 0.15f, cy - r * 0.25f, cx + r * 0.15f, cy + r * 0.7f, trait);
        }
    },

    /** La croix de flèches — le déplacement sûr. */
    DEPLACER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx, cy - r * 0.55f, cx, cy + r * 0.55f, trait);
            c.drawLine(cx - r * 0.55f, cy, cx + r * 0.55f, cy, trait);
            c.drawLine(cx - r * 0.25f, cy - r * 0.35f, cx, cy - r * 0.55f, trait);
            c.drawLine(cx + r * 0.25f, cy - r * 0.35f, cx, cy - r * 0.55f, trait);
            c.drawLine(cx - r * 0.25f, cy + r * 0.35f, cx, cy + r * 0.55f, trait);
            c.drawLine(cx + r * 0.25f, cy + r * 0.35f, cx, cy + r * 0.55f, trait);
        }
    },

    /** L'œil barré — le masquage d'inspection. */
    MASQUER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            oeil(c, trait, cx, cy, r, false);
            c.drawLine(cx - r * 0.9f, cy + r * 0.9f, cx + r * 0.9f, cy - r * 0.9f, trait);
        }
    },

    /** Le triangle — l'exécution du fichier. */
    EXECUTER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.6f, cy - r * 0.85f, cx - r * 0.6f, cy + r * 0.85f, trait);
            c.drawLine(cx - r * 0.6f, cy - r * 0.85f, cx + r * 0.75f, cy, trait);
            c.drawLine(cx - r * 0.6f, cy + r * 0.85f, cx + r * 0.75f, cy, trait);
        }
    },

    /** L'insecte — l'analyse du lint. */
    LINT {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawOval(cx - r * 0.45f, cy - r * 0.5f, cx + r * 0.45f, cy + r * 0.6f, trait);
            c.drawLine(cx - r * 0.3f, cy - r * 0.5f, cx - r * 0.75f, cy - r * 0.85f, trait);
            c.drawLine(cx + r * 0.3f, cy - r * 0.5f, cx + r * 0.75f, cy - r * 0.85f, trait);
            c.drawLine(cx - r * 0.4f, cy, cx - r * 0.95f, cy, trait);
            c.drawLine(cx + r * 0.4f, cy, cx + r * 0.95f, cy, trait);
            c.drawLine(cx - r * 0.3f, cy + r * 0.4f, cx - r * 0.75f, cy + r * 0.85f, trait);
            c.drawLine(cx + r * 0.3f, cy + r * 0.4f, cx + r * 0.75f, cy + r * 0.85f, trait);
        }
    },

    /** La page et son signe — le gabarit de méthode. */
    GABARIT {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.7f, cy - r * 0.9f, cx + r * 0.5f, cy + r * 0.9f, trait);
            c.drawLine(cx + r * 0.2f, cy - r * 0.9f, cx + r * 0.5f, cy - r * 0.6f, trait);
            c.drawLine(cx + r * 0.5f, cy - r * 0.6f, cx + r * 0.5f, cy - r * 0.9f, trait);
            c.drawLine(cx - r * 0.1f, cy, cx + r * 0.5f, cy, trait);
            c.drawLine(cx + r * 0.2f, cy - r * 0.3f, cx + r * 0.2f, cy + r * 0.3f, trait);
        }
    },

    /** Les lignes rangées — le formatage. */
    FORMATER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.8f, cy - r * 0.6f, cx + r * 0.8f, cy - r * 0.6f, trait);
            c.drawLine(cx - r * 0.8f, cy, cx + r * 0.35f, cy, trait);
            c.drawLine(cx - r * 0.8f, cy + r * 0.6f, cx + r * 0.8f, cy + r * 0.6f, trait);
        }
    },

    /** Le crayon — le renommage. */
    RENOMMER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.85f, cy + r * 0.85f, cx + r * 0.35f, cy - r * 0.35f, trait);
            c.drawLine(cx - r * 0.6f, cy + r * 0.85f, cx + r * 0.6f, cy - r * 0.35f, trait);
            c.drawLine(cx - r * 0.85f, cy + r * 0.85f, cx - r * 0.6f, cy + r * 0.85f, trait);
            c.drawLine(cx + r * 0.35f, cy - r * 0.35f, cx + r * 0.6f, cy - r * 0.35f, trait);
            c.drawLine(cx - r * 0.85f, cy + r * 0.6f, cx - r * 0.6f, cy + r * 0.85f, trait);
        }
    },

    /** La courbe qui monte — la prédiction IA. */
    IA_PREDICTION {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx - r * 0.8f, cy + r * 0.6f, cx - r * 0.1f, cy - r * 0.1f, trait);
            c.drawLine(cx - r * 0.1f, cy - r * 0.1f, cx + r * 0.6f, cy - r * 0.75f, trait);
            c.drawLine(cx + r * 0.25f, cy - r * 0.75f, cx + r * 0.6f, cy - r * 0.75f, trait);
            c.drawLine(cx + r * 0.6f, cy - r * 0.75f, cx + r * 0.6f, cy - r * 0.4f, trait);
        }
    },

    /** L'arc de retour — l'annulation. */
    ANNULER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            retour(c, trait, cx, cy, r, -1f);
        }
    },

    /** L'arc de retour renversé — le rétablissement. */
    RETABLIR {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            retour(c, trait, cx, cy, r, 1f);
        }
    },

    /** Le croissant — le thème. */
    THEME {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawArc(cx - r, cy - r, cx + r, cy + r, 45f, 270f, false, trait);
            c.drawArc(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f,
                    45f, 270f, false, trait);
        }
    },

    /** Le A amoindri — la diminution de la taille. */
    DIMINUER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            dessinerTexte(c, texte, "A-", cx, cy, r);
        }
    },

    /** Le A accru — l'agrandissement de la taille. */
    AGRANDIR {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            dessinerTexte(c, texte, "A+", cx, cy, r);
        }
    },

    /** La rose et ses dents — les réglages. */
    REGLAGES {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r * 0.55f, trait);
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60d);
                c.drawLine(cx + (float) Math.cos(angle) * r * 0.75f,
                        cy + (float) Math.sin(angle) * r * 0.75f,
                        cx + (float) Math.cos(angle) * r,
                        cy + (float) Math.sin(angle) * r, trait);
            }
        }
    },

    /** Les pages duales, l'une accrue, l'autre amoindrie — le diff. */
    DIFF {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.85f, cy - r * 0.95f, cx + r * 0.1f, cy, trait);
            c.drawRect(cx - r * 0.1f, cy, cx + r * 0.85f, cy + r * 0.95f, trait);
            c.drawLine(cx - r * 0.6f, cy - r * 0.48f, cx - r * 0.35f, cy - r * 0.48f, trait);
            c.drawLine(cx + r * 0.35f, cy + r * 0.48f, cx + r * 0.6f, cy + r * 0.48f, trait);
        }
    },

    /** La clé — le correctif LSP. */
    CORRECTIF {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawCircle(cx - r * 0.5f, cy - r * 0.5f, r * 0.32f, trait);
            c.drawLine(cx - r * 0.25f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.75f, trait);
            c.drawLine(cx + r * 0.45f, cy + r * 0.75f, cx + r * 0.75f, cy + r * 0.75f, trait);
            c.drawLine(cx + r * 0.75f, cy + r * 0.45f, cx + r * 0.75f, cy + r * 0.75f, trait);
        }
    },

    /** Le robot — l'agent et ses révocations. */
    AGENT {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.7f, cy - r * 0.5f, cx + r * 0.7f, cy + r * 0.6f, trait);
            c.drawLine(cx, cy - r * 0.5f, cx, cy - r * 0.85f, trait);
            c.drawCircle(cx, cy - r * 0.95f, r * 0.1f, trait);
            c.drawCircle(cx - r * 0.3f, cy - r * 0.1f, r * 0.1f, trait);
            c.drawCircle(cx + r * 0.3f, cy - r * 0.1f, r * 0.1f, trait);
        }
    },

    /** L'étincelle — la suggestion fantôme de l'IA. */
    IA_FANTOME {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx, cy - r * 0.2f, cx, cy + r * 0.2f, trait);
            c.drawLine(cx - r * 0.2f, cy, cx + r * 0.2f, cy, trait);
            croix(c, trait, cx + r * 0.55f, cy - r * 0.55f, r * 0.14f);
            croix(c, trait, cx - r * 0.55f, cy + r * 0.55f, r * 0.14f);
        }
    },

    /** Le paquet et sa flèche — l'import des classes. */
    IMPORTER {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawRect(cx - r * 0.75f, cy - r * 0.9f, cx + r * 0.75f, cy, trait);
            c.drawLine(cx, cy + r * 0.1f, cx, cy + r * 0.8f, trait);
            c.drawLine(cx - r * 0.3f, cy + r * 0.5f, cx, cy + r * 0.8f, trait);
            c.drawLine(cx + r * 0.3f, cy + r * 0.5f, cx, cy + r * 0.8f, trait);
        }
    },

    /** La flèche qui revient à la marge — le retour à la ligne
     *  souple (A-18). */
    RETOUR_LIGNE {
        @Override
        void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r) {
            c.drawLine(cx + r * 0.8f, cy - r * 0.6f, cx + r * 0.8f,
                    cy + r * 0.4f, trait);
            c.drawLine(cx + r * 0.8f, cy + r * 0.4f, cx - r * 0.4f,
                    cy + r * 0.4f, trait);
            c.drawLine(cx - r * 0.05f, cy + r * 0.05f, cx - r * 0.4f,
                    cy + r * 0.4f, trait);
            c.drawLine(cx - r * 0.05f, cy + r * 0.75f, cx - r * 0.4f,
                    cy + r * 0.4f, trait);
        }
    };

    /**
     * Le tracé du glyphe — traits, cercles et arcs autour du centre,
     * au demi-rayon donné ; les encres et la graisse se posent par
     * {@link #dessiner(IconeAction, Canvas, Paint, Paint, float,
     * float, float, int)} qui entoure ce tracé.
     */
    abstract void tracer(Canvas c, Paint trait, Paint texte, float cx, float cy, float r);

    /**
     * Dessine le pictogramme : les encres se posent AVANT le tracé,
     * la graisse du trait se dérive du rayon, les tailles se
     * restaurent après — le panneau reprend ses pinceaux intacts.
     *
     * @param picto  le pictogramme à tracer
     * @param c      le canevas du panneau
     * @param trait  le pinceau de ligne du panneau
     * @param texte  le pinceau de texte du panneau (les glyphes lettrés)
     * @param cx     l'abscisse du centre du glyphe
     * @param cy     l'ordonnée du centre du glyphe
     * @param r      le demi-rayon du glyphe
     * @param encre  la couleur du glyphe, alpha compris
     */
    public static void dessiner(IconeAction picto, Canvas c, Paint trait,
            Paint texte, float cx, float cy, float r, int encre) {
        trait.setColor(encre);
        texte.setColor(encre);
        float graisse = trait.getStrokeWidth();
        float taille = texte.getTextSize();
        trait.setStrokeWidth(Math.max(1f, r / 5f));
        picto.tracer(c, trait, texte, cx, cy, r);
        trait.setStrokeWidth(graisse);
        texte.setTextSize(taille);
    }

    /**
     * L'encre ALLÉGÉE d'un pictogramme — l'audit du propriétaire du
     * 2026-11-14 (« il est trop foncé, améliore-la ») : le glyphe se
     * mêle à l'atténuée de la palette aux deux tiers du texte, il se
     * lit sans crier ; l'ampoule d'intentions et les encres de
     * sévérité restent telles quelles — leur couleur EST leur sens.
     *
     * @param muet  l'encre atténuée de la palette
     * @param texte l'encre du texte de la palette
     * @return le mélange aux deux tiers vers l'atténuée
     */
    public static int encreAllgeee(int muet, int texte) {
        return melanger(muet, texte, 2f / 3f);
    }

    /** Le mélange linéaire de deux encres ARGB — la part du premier. */
    private static int melanger(int premier, int second, float part) {
        int a = Math.round(((premier >>> 24) & 0xFF) * part
                + ((second >>> 24) & 0xFF) * (1f - part));
        int r = Math.round(((premier >>> 16) & 0xFF) * part
                + ((second >>> 16) & 0xFF) * (1f - part));
        int g = Math.round((premier & 0xFF00) / 256f * part
                + (second & 0xFF00) / 256f * (1f - part));
        int b = Math.round((premier & 0xFF) * part
                + (second & 0xFF) * (1f - part));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Les quatre coins — le cadre de tout sélectionner. */
    private static void coins(Canvas c, Paint trait, float cx, float cy, float r) {
        float k = r * 0.45f;
        c.drawLine(cx - r, cy - r + k, cx - r, cy - r, trait);
        c.drawLine(cx - r, cy - r, cx - r + k, cy - r, trait);
        c.drawLine(cx + r - k, cy - r, cx + r, cy - r, trait);
        c.drawLine(cx + r, cy - r, cx + r, cy - r + k, trait);
        c.drawLine(cx - r, cy + r - k, cx - r, cy + r, trait);
        c.drawLine(cx - r, cy + r, cx - r + k, cy + r, trait);
        c.drawLine(cx + r - k, cy + r, cx + r, cy + r, trait);
        c.drawLine(cx + r, cy + r, cx + r, cy + r - k, trait);
    }

    /** L'œil — le contour amande et sa pupille pleine. */
    private static void oeil(Canvas c, Paint trait, float cx, float cy, float r,
            boolean pupille) {
        c.drawOval(cx - r, cy - r * 0.55f, cx + r, cy + r * 0.55f, trait);
        if (pupille) {
            Paint.Style style = trait.getStyle();
            trait.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, r * 0.22f, trait);
            trait.setStyle(style);
        }
    }

    /** Le glyphe lettré — centré sur le point, taillé au rayon. */
    private static void dessinerTexte(Canvas c, Paint texte, String lettres,
            float cx, float cy, float r) {
        float taille = texte.getTextSize();
        texte.setTextSize(r * 1.7f);
        float x = cx - texte.measureText(lettres) / 2f;
        c.drawText(lettres, x, cy + r * 0.6f, texte);
        texte.setTextSize(taille);
    }

    /** La petite croix des étincelles. */
    private static void croix(Canvas c, Paint trait, float cx, float cy, float k) {
        c.drawLine(cx - k, cy, cx + k, cy, trait);
        c.drawLine(cx, cy - k, cx, cy + k, trait);
    }

    /** L'arc de retour et sa pointe — l'annulation et son miroir. */
    private static void retour(Canvas c, Paint trait, float cx, float cy, float r,
            float miroir) {
        c.drawArc(cx - r * 0.8f, cy - r * 0.8f, cx + r * 0.8f, cy + r * 0.8f,
                90f, 270f, false, trait);
        c.drawLine(cx - miroir * r * 0.1f, cy - r * 0.8f,
                cx + miroir * r * 0.35f, cy - r * 0.8f, trait);
        c.drawLine(cx - miroir * r * 0.1f, cy - r * 0.8f,
                cx - miroir * r * 0.1f, cy - r * 0.4f, trait);
    }
}
