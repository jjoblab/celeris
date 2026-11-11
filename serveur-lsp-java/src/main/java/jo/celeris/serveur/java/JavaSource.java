package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le document Java suivi par le serveur : le texte entier, son index de
 * lignes et la transposition décalage ↔ position (les décalages sont
 * en unités UTF-16 — la colonne du protocole est la même unité).
 *
 * <p>Le découpage ignore la nature du saut de ligne : {@link #line(int)}
 * rend le contenu SANS le saut, les positions se calent sur les
 * contenus. Les demandes hors bornes se calment : une ligne au-delà de
 * la dernière rend la dernière ligne, une colonne au-delà du contenu
 * rend la fin du contenu — le protocole tolère les clients généreux.</p>
 */
public final class JavaSource {

    /** La position d'un décalage : ligne et colonne, base zéro. */
    public record Spot(int line, int column) {
    }

    private final String text;

    private final List<Integer> lineStarts;

    private JavaSource(String text, List<Integer> lineStarts) {
        this.text = text;
        this.lineStarts = lineStarts;
    }

    /**
     * Indexe un document : les débuts de chaque ligne, ligne vide
     * comprise.
     *
     * @param text le texte entier du document, jamais null
     * @return le document indexé
     */
    public static JavaSource of(String text) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        return new JavaSource(text, List.copyOf(starts));
    }

    /** Le texte entier du document. */
    public String text() {
        return text;
    }

    /** Le nombre de lignes (au moins une, même pour le texte vide). */
    public int lineCount() {
        return lineStarts.size();
    }

    /**
     * Le contenu d'une ligne, SANS son saut de ligne : les retours
     * chariot des fichiers Windows restent dedans, l'analyse les tient
     * pour des blancs.
     *
     * @param index la ligne cherchée, calée à la dernière si au-delà
     * @return le contenu de la ligne
     */
    public String line(int index) {
        int borne = Math.min(Math.max(index, 0), lineStarts.size() - 1);
        int debut = lineStarts.get(borne);
        int fin = debut;
        while (fin < text.length() && text.charAt(fin) != '\n') {
            fin++;
        }
        if (fin > debut && text.charAt(fin - 1) == '\r') {
            fin--;
        }
        return text.substring(debut, fin);
    }

    /** Le décalage du premier caractère d'une ligne (calée). */
    public int lineStart(int index) {
        int borne = Math.min(Math.max(index, 0), lineStarts.size() - 1);
        return lineStarts.get(borne);
    }

    /** Le décalage de la fin du contenu d'une ligne (hors saut). */
    public int lineEnd(int index) {
        return lineStart(index) + line(index).length();
    }

    /** La ligne d'un décalage (calée au texte). */
    public int lineOf(int offset) {
        int borne = Math.min(Math.max(offset, 0), text.length());
        int bas = 0;
        int haut = lineStarts.size() - 1;
        while (bas < haut) {
            int milieu = (bas + haut + 1) / 2;
            if (lineStarts.get(milieu) <= borne) {
                bas = milieu;
            } else {
                haut = milieu - 1;
            }
        }
        return bas;
    }

    /** Le décalage d'une position (ligne, colonne), calé au contenu. */
    public int offset(int line, int column) {
        int borne = Math.min(Math.max(line, 0), lineStarts.size() - 1);
        int debut = lineStarts.get(borne);
        int fin = lineEnd(borne);
        return Math.min(debut + Math.max(column, 0), fin);
    }

    /** La position d'un décalage, calée au texte. */
    public Spot spot(int offset) {
        int borne = Math.min(Math.max(offset, 0), text.length());
        int ligne = lineOf(borne);
        return new Spot(ligne, borne - lineStarts.get(ligne));
    }

    /**
     * L'identifiant autour d'un décalage : les caractères de nom Java
     * qui l'entourent — le curseur au milieu d'un mot tient le mot
     * entier, le curseur sur une ponctuation rend null.
     *
     * @param offset le décalage du curseur
     * @return l'identifiant autour, ou null s'il n'y en a pas
     */
    public @Nullable String wordAround(int offset) {
        int borne = Math.min(Math.max(offset, 0), text.length());
        if (borne < text.length() && estNom(text.charAt(borne))) {
            int debut = borne;
            while (debut > 0 && estNom(text.charAt(debut - 1))) {
                debut--;
            }
            int fin = borne;
            while (fin < text.length() && estNom(text.charAt(fin))) {
                fin++;
            }
            return text.substring(debut, fin);
        }
        if (borne > 0 && estNom(text.charAt(borne - 1))) {
            return wordAround(borne - 1);
        }
        return null;
    }

    /**
     * Le préfixe d'identifiant qui précède le curseur (la frappe en
     * cours) : les caractères de nom contigus avant le décalage.
     *
     * @param offset le décalage du curseur
     * @return le préfixe, éventuellement vide
     */
    public String identifierPrefix(int offset) {
        int borne = Math.min(Math.max(offset, 0), text.length());
        int debut = borne;
        while (debut > 0 && estNom(text.charAt(debut - 1))) {
            debut--;
        }
        return text.substring(debut, borne);
    }

    /** Le code dépouillé : chaînes et commentaires mis à blanc. */
    public StrippedCode stripped() {
        return StrippedCode.of(text);
    }

    private static boolean estNom(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    /**
     * Le texte dépouillé de son bruit : le contenu des chaînes et des
     * commentaires devient des espaces — la STRUCTURE garde ses
     * longueurs (un caractère remplacé par un espace), les accolades et
     * les points-virgules du code restent seuls à parler.
     *
     * <p>Les littéraux non fermés (une chaîne ouverte dont la ligne se
     * termine) se consignent : l'analyseur de diagnostics les sert.
     * Les blocs de commentaires et leur étendue se consignent aussi :
     * les plis s'en servent.</p>
     */
    public record StrippedCode(String text, List<Integer> unclosedLiterals,
            List<Comment> comments) {

        /** Un bloc de commentaire : décalages de début et de fin. */
        public record Comment(int start, int end) {
        }

        /**
         * Dépouille un texte : l'état de balayage suit le code, les
         * commentaires (de ligne et de bloc), les chaînes, les
         * caractères et les blocs de texte Java.
         *
         * @param source le texte entier du document
         * @return le code dépouillé et ses constats
         */
        public static StrippedCode of(String source) {
            StringBuilder sortie = new StringBuilder(source.length());
            List<Integer> nonFermes = new ArrayList<>();
            List<Comment> blocs = new ArrayList<>();
            int i = 0;
            while (i < source.length()) {
                char c = source.charAt(i);
                char suivant = i + 1 < source.length()
                        ? source.charAt(i + 1) : '\0';
                if (c == '/' && suivant == '/') {
                    i = blanchirJusquauSaut(source, sortie, i);
                } else if (c == '/' && suivant == '*') {
                    i = blanchirBloc(source, sortie, i, blocs);
                } else if (c == '"'
                        && suivant == '"' && i + 2 < source.length()
                        && source.charAt(i + 2) == '"') {
                    i = blanchirBlocDeTexte(source, sortie, i);
                } else if (c == '"' || c == '\'') {
                    i = blanchirLitteral(source, sortie, i, nonFermes);
                } else {
                    sortie.append(c);
                    i++;
                }
            }
            return new StrippedCode(sortie.toString(),
                    List.copyOf(nonFermes), List.copyOf(blocs));
        }

        /** Un commentaire de ligne : tout le reste de la ligne se
         * blanchit, le saut reste au code. */
        private static int blanchirJusquauSaut(String source,
                StringBuilder sortie, int i) {
            while (i < source.length() && source.charAt(i) != '\n') {
                sortie.append(' ');
                i++;
            }
            return i;
        }

        /** Un bloc de commentaire : blanchi, ses bornes se
         * consignent — un bloc non fermé court jusqu'au bout. */
        private static int blanchirBloc(String source, StringBuilder sortie,
                int i, List<Comment> blocs) {
            int debut = i;
            sortie.append("  ");
            i += 2;
            while (i < source.length()
                    && !(source.charAt(i) == '*'
                    && i + 1 < source.length()
                    && source.charAt(i + 1) == '/')) {
                sortie.append(source.charAt(i) == '\n' ? '\n' : ' ');
                i++;
            }
            if (i < source.length()) {
                sortie.append("  ");
                i += 2;
            }
            blocs.add(new Comment(debut, Math.min(i, source.length())));
            return i;
        }

        /** Un bloc de texte Java : les trois guillemets ouvrants,
         * le contenu et les trois fermants se blanchissent. */
        private static int blanchirBlocDeTexte(String source,
                StringBuilder sortie, int i) {
            i += 3;
            sortie.append("   ");
            while (i < source.length()) {
                boolean echappe = source.charAt(i) == '\\';
                sortie.append(' ');
                i++;
                if (echappe && i < source.length()) {
                    sortie.append(' ');
                    i++;
                } else if (i + 2 < source.length()
                        && source.charAt(i) == '"'
                        && source.charAt(i + 1) == '"'
                        && source.charAt(i + 2) == '"') {
                    sortie.append("   ");
                    i += 3;
                    break;
                }
            }
            return i;
        }

        /** Une chaîne ou un caractère : blanchis jusqu'à leur
         * fermeture ; une ouverture non fermée se consigne. */
        private static int blanchirLitteral(String source,
                StringBuilder sortie, int i, List<Integer> nonFermes) {
            char fermeture = source.charAt(i);
            int ouverture = i;
            sortie.append(' ');
            i++;
            boolean fermee = false;
            while (i < source.length() && source.charAt(i) != '\n') {
                boolean echappe = source.charAt(i) == '\\';
                sortie.append(' ');
                i++;
                if (echappe && i < source.length()) {
                    sortie.append(' ');
                    i++;
                } else if (source.charAt(i) == fermeture) {
                    sortie.append(' ');
                    i++;
                    fermee = true;
                    break;
                }
            }
            if (!fermee) {
                nonFermes.add(ouverture);
            }
            return i;
        }

        /**
         * Le décalage de la première accolade ouvrante non fermée, ou
         * -1 : le texte dépouillé garde la structure du code.
         *
         * @return le décalage de l'accolade orpheline, ou -1
         */
        public int firstUnclosedBrace() {
            return premierNonFerme('{', '}');
        }

        /**
         * Le décalage de la première parenthèse ouvrante non fermée,
         * ou -1.
         *
         * @return le décalage de la parenthèse orpheline, ou -1
         */
        public int firstUnclosedParen() {
            return premierNonFerme('(', ')');
        }

        private int premierNonFerme(char ouvrante, char fermante) {
            java.util.Deque<Integer> pile = new java.util.ArrayDeque<>();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == ouvrante) {
                    pile.push(i);
                } else if (c == fermante) {
                    if (pile.isEmpty()) {
                        return -1;
                    }
                    pile.pop();
                }
            }
            return pile.isEmpty() ? -1 : pile.peek();
        }
    }
}
