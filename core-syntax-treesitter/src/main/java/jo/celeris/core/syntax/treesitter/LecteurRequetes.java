package jo.celeris.core.syntax.treesitter;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le lecteur de requêtes de surbrillance du chantier D (prompt du
 * 2026-11-22) : un {@code highlights.scm} se lit SANS la liaison —
 * la grammaire de la requête est close (un nœud nommé, une capture,
 * éventuellement des prédicats), la lecture en sort le MOTIF par
 * nœud et sa CAPTURE, la classe en nourrit le chemin de teinte.
 *
 * <p>La requête exécutée nativement ({@code TSQuery} de la liaison)
 * rend les noms de nœuds RÉELS de la grammaire — le lecteur ici sert
 * les bancs JVM (sans Android) et la vérification du fichier : la
 * teinte d'une feuille se résout par sa capture lue au
 * chargement.</p>
 *
 * <p>La grammaire du format (close et régulière) :
 * {@code (motif de nœud) @capture} — un motif est un nom de nœud,
 * éventuellement imbriqué avec des champs
 * {@code (parent enfant: (nom))} ; les prédicats {@code #match?}
 * suivent le motif ; les commentaires vivent en tête de ligne
 * ({@code ;}). Le lecteur garde le premier niveau : le NOM DU NŒUD
 * terminal et sa capture.</p>
 */
final class LecteurRequetes {

    /** Les entrées lues : le nom du nœud terminal, sa capture. */
    record Entree(String noeud, String capture) {
    }

    /** Les entrées d'un fichier de requête — dans l'ordre du texte. */
    static List<Entree> lire(String texte) {
        List<Entree> entrees = new ArrayList<>();
        int curseur = 0;
        int longueur = texte.length();
        while (curseur < longueur) {
            char c = texte.charAt(curseur);
            if (c == ';' || Character.isWhitespace(c)) {
                curseur = ligneSuivante(curseur, texte, c);
                continue;
            }
            if (c == '(') {
                curseur = motif(curseur, texte, entrees);
                continue;
            }
            if (c == '[') {
                curseur = liste(curseur, texte, entrees);
                continue;
            }
            // Une entrée atomique : "nom" @capture (la forme des
            // mots-clés littéraux — « "if" @keyword »). Le filet
            // d'avancement : un chemin qui rendrait la position
            // SANS avancer (un caractère inconnu du format) y
            // gagne un pas — la lecture d'une requête réelle ne
            // boucle JAMAIS (le garde du xml de référence).
            int avant = curseur;
            curseur = litteral(curseur, texte, entrees);
            if (curseur == avant) {
                curseur++;
            }
        }
        return entrees;
    }

    /** Une liste alternative : {@code [ (a) (b) "c" ] @capture} pose
     *  une entrée par membre — la capture de la liste s'attache à
     *  chacun (la forme dominante des requêtes de référence : les
     *  mots-clés et littéraux y vivent par listes). Les membres se
     *  lisent par un balayage dédié : un nom entre parenthèses (le
     *  premier mot du membre), ou un littéral entre guillemets. */
    private static int liste(int curseur, String texte,
            List<Entree> entrees) {
        curseur++; // le crochet ouvrant
        List<String> membres = new ArrayList<>();
        while (curseur < texte.length()) {
            char c = texte.charAt(curseur);
            if (c == ';' || Character.isWhitespace(c)) {
                curseur = ligneSuivante(curseur, texte, c);
                continue;
            }
            if (c == ']') {
                curseur++;
                break;
            }
            if (c == '(') {
                curseur = membreParenthese(curseur, texte, membres);
                continue;
            }
            if (c == '"') {
                curseur = membreLitteral(curseur, texte, membres);
                continue;
            }
            if (c == '[') {
                // une liste imbriquée : ses propres entrées se posent
                curseur = liste(curseur, texte, entrees);
                continue;
            }
            curseur++; // un signe inconnu (quantificateur) : passer
        }
        Capture capture = captureApres(curseur, texte);
        if (capture != null) {
            for (String membre : membres) {
                entrees.add(new Entree(membre, capture.nom()));
            }
            return capture.fin();
        }
        return curseur;
    }

    /** Un membre parenthésé d'une liste : le PREMIER mot après
     *  l'ouverture est le nom du nœud — le membre se consomme jusqu
     *  à sa parenthèse fermante APPARIÉE (les membres imbriqués
     *  existent). Le membre-prédicat ({@code (#match? ...)}) n'est
     *  pas un nœud : il ne s'enregistre pas. */
    private static int membreParenthese(int curseur, String texte,
            List<String> membres) {
        curseur++; // l'ouverture
        StringBuilder nom = new StringBuilder();
        int profondeur = 1;
        while (curseur < texte.length() && profondeur > 0) {
            char c = texte.charAt(curseur);
            if (c == '(') {
                profondeur++;
            } else if (c == ')') {
                profondeur--;
                if (profondeur == 0) {
                    curseur++;
                    break;
                }
            } else if (profondeur == 1 && nom.length() == 0
                    && estNom(c)) {
                nom.append(c);
            } else if (profondeur == 1 && nom.length() > 0
                    && estNom(c)) {
                nom.append(c);
            } else if (profondeur == 1 && nom.length() > 0) {
                // le nom du membre est complet au premier blanc ou
                // deux-points : le reste se saute jusqu'à l'appariement
                nom.append('\u0000');
            }
            curseur++;
        }
        String lu = nom.toString();
        if (!lu.isEmpty() && lu.charAt(0) != '#'
                && !lu.contains("\u0000\u0000")) {
            membres.add(lu.replace("\u0000", ""));
        }
        return curseur;
    }

    /** Un membre littéral d'une liste : le texte entre guillemets
     *  EST le nom (les mots-clés : « "if" »). */
    private static int membreLitteral(int curseur, String texte,
            List<String> membres) {
        curseur++; // l'ouverture du guillemet
        StringBuilder nom = new StringBuilder();
        while (curseur < texte.length()
                && texte.charAt(curseur) != '"') {
            nom.append(texte.charAt(curseur));
            curseur++;
        }
        if (curseur < texte.length()) {
            curseur++; // la fermeture
        }
        membres.add(nom.toString());
        return curseur;
    }

    /** La fin d'un commentaire ou d'un blanc : la ligne suivante ou
     *  le caractère suivant. */
    private static int ligneSuivante(int curseur, String texte, char c) {
        if (c == ';') {
            while (curseur < texte.length()
                    && texte.charAt(curseur) != '\n') {
                curseur++;
            }
            return curseur;
        }
        return curseur + 1;
    }

    /** Un motif parenthésé : la capture s'attache au NŒUD qui la
     *  porte — dans {@code (racine champ: (terminal) @capture)}, le
     *  nœud capturé est {@code terminal} (le dernier fermé), le nom
     *  accumulé depuis SA parenthèse ouvrante. Le nom d'un nœud est
     *  COMPLET à sa fermeture : ce qui suit jusqu'à la prochaine
     *  parenthèse est champ ou capture, jamais du nom. */
    private static int motif(int curseur, String texte, List<Entree> entrees) {
        String nomCourant = null;
        String dernierFerme = null;
        int profondeur = 0;
        // Vrai dès qu'un nœud a fermé : son nom est complet, plus
        // RIEN ne le remplace (champs et captures qui suivent ne
        // sont pas des noms).
        boolean nomFige = false;
        StringBuilder accumulateur = new StringBuilder();
        while (curseur < texte.length()) {
            char c = texte.charAt(curseur);
            if (c == '(') {
                profondeur++;
                accumulateur.setLength(0);
                nomCourant = null;
                nomFige = false;
                curseur++;
                continue;
            }
            if (c == ')') {
                int apres = fermer(curseur, texte, entrees,
                        nomCourant, dernierFerme, profondeur);
                // La fermeture rend la position SUIVANTE en négatif
                // quand le motif racine vient de clore.
                if (apres < 0) {
                    return -apres;
                }
                curseur = apres;
                profondeur--;
                dernierFerme = nomCourant != null && nomCourant.length() > 0
                        ? nomCourant : dernierFerme;
                nomFige = true;
                accumulateur.setLength(0);
                continue;
            }
            nomCourant = accumuler(nomFige, nomCourant, accumulateur, c);
            curseur++;
        }
        return curseur;
    }

    /** Un littéral : « "if" @keyword » — le nom EST le littéral. */
    /** Un littéral : « "if" @keyword » — le nom EST le littéral. La
     *  capture consommée, le caractère qui suit le littéral peut
     *  être une FERMANTE orpheline (un motif parent mal fini dans
     *  le texte) : elle se consomme aussi — la lecture ne s'y
     *  arrête JAMAIS sans avancer (la boucle infinie d'une requête
     *  réelle, constatée sur le xml de référence : le motif
     *  {@code "]]>" @…)} laissait sa fermante au niveau racine). */
    private static int litteral(int curseur, String texte,
            List<Entree> entrees) {
        if (texte.charAt(curseur) != '"') {
            // Un mot inconnu (prédicat, champ…) : sauter jusqu'au
            // prochain blanc ou parenthèse — la parenthèse se
            // consomme (l'ouverture mène au motif, la fermante
            // orpheline du texte se digère).
            while (curseur < texte.length()) {
                char c = texte.charAt(curseur);
                if (c == '(' || c == ')') {
                    return curseur + 1;
                }
                if (Character.isWhitespace(c)) {
                    return curseur;
                }
                curseur++;
            }
            return curseur;
        }
        curseur++;
        StringBuilder nom = new StringBuilder();
        while (curseur < texte.length() && texte.charAt(curseur) != '"') {
            nom.append(texte.charAt(curseur));
            curseur++;
        }
        curseur++; // la fermeture
        Capture capture = captureApres(curseur, texte);
        if (capture != null) {
            entrees.add(new Entree(nom.toString(), capture.nom()));
            // La capture consommée, une FERMANTE orpheline peut
            // suivre (un motif parent refermé par le texte) : elle
            // se consomme — la lecture ne s'y arrête jamais sans
            // avancer (la boucle infinie du xml de référence).
            int apres = capture.fin();
            if (apres < texte.length() && texte.charAt(apres) == ')') {
                apres++;
            }
            return apres;
        }
        return curseur;
    }

    /** La capture qui suit un motif fermé — « @nom » après blancs :
     *  la position de fin s'y rend (l'appelant y reprend, blancs
     *  consommés compris — le retour ne saute jamais un motif). */
    private record Capture(String nom, int fin) {
    }

    /** La capture qui suit un motif fermé, null sans arobase. */
    private static @Nullable Capture captureApres(int curseur, String texte) {
        int position = curseur;
        while (position < texte.length()
                && Character.isWhitespace(texte.charAt(position))) {
            position++;
        }
        if (position < texte.length() && texte.charAt(position) == '@') {
            int fin = position + 1;
            while (fin < texte.length() && estNom(texte.charAt(fin))) {
                fin++;
            }
            return new Capture(texte.substring(position + 1, fin), fin);
        }
        return null;
    }

    /** La fermeture d'un nœud : son nom devient le candidat de la
     *  capture qui le SUIT — la capture s'attache au nœud qu'elle
     *  suit (interne : elle vit DANS le motif, la racine ferme
     *  après ; racine : elle suit le motif entier).
     *
     * @return la position de lecture suivante, ou sa NÉGATIVE quand
     *         le motif racine vient de clore (l'appelant y termine)
     */
    private static int fermer(int curseur, String texte,
            List<Entree> entrees, String nomCourant, String dernierFerme,
            int profondeur) {
        String fermant = nomCourant != null && nomCourant.length() > 0
                ? nomCourant : dernierFerme;
        curseur++;
        int profondeurApres = profondeur - 1;
        Capture capture = captureApres(curseur, texte);
        if (fermant != null && capture != null) {
            entrees.add(new Entree(fermant, capture.nom()));
            curseur = capture.fin();
            return profondeurApres <= 0 ? -curseur : curseur;
        }
        return profondeurApres <= 0 ? -curseur : curseur;
    }

    /** Accumule une lettre de nom : le nom du nœud courant croît,
     *  ou se FIGE après une fermeture (rien ne le remplace).
     *  Le blanc ou deux-points coupe le mot : le mot lu reste LE nom
     *  du nœud courant, l'accumulateur repart. */
    private static String accumuler(boolean nomFige, String nomCourant,
            StringBuilder accumulateur, char c) {
        if (nomFige) {
            return nomCourant;
        }
        if (estNom(c)) {
            accumulateur.append(c);
            return accumulateur.toString();
        }
        if (accumulateur.length() > 0 && nomCourant == null) {
            return accumulateur.toString();
        }
        return nomCourant;
    }

    /** Vrai si le caractère participe à un nom de nœud ou de capture. */
    private static boolean estNom(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
    }
}
