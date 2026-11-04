package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Détection, décodage strict et encodage des fichiers texte (parité
 * A-24 : UTF-8, UTF-16, UTF-32 et Latin-1, marques d'ordre, fins de
 * ligne dominantes).
 *
 * <p>La détection reconnaît les cinq marques d'ordre des octets, en
 * testant les marques UTF-32 avant celles d'UTF-16 qui partagent leur
 * préfixe ; sans marque, une heuristique de nuls réguliers tranche
 * entre UTF-16, UTF-32 et UTF-8, puis UTF-8 valide est préféré —
 * aucune heuristique réseau, jamais. Le Latin-1 n'est jamais détecté
 * automatiquement : l'hôte l'impose. Le décodage est strict : chaque
 * erreur produit un caractère de remplacement U+FFFD et une entrée
 * d'erreur portant son décalage d'octet — jamais d'exception qui
 * perdrait le reste du fichier. L'encodage est inversement sans
 * perte : un substitut isolé dans le texte est refusé, de même qu'un
 * caractère hors de la page Latin-1. Les codecs par famille vivent
 * dans {@link Utf8Codec} et {@link WideCodecs}, visibles du paquet.</p>
 *
 * <p>Fil d'exécution : utilitaire pur.</p>
 */
public final class EncodingCodec {

    private EncodingCodec() {
    }

    /** Les marques d'ordre connues, la plus longue d'abord. */
    private static final byte[][] MARQUES = {
            {0, 0, (byte) 0xFE, (byte) 0xFF},
            {(byte) 0xFF, (byte) 0xFE, 0, 0},
            {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
            {(byte) 0xFE, (byte) 0xFF},
            {(byte) 0xFF, (byte) 0xFE},
    };

    private static final Encoding[] ENCODINGS_DE_MARQUE = {
            Encoding.UTF_32BE, Encoding.UTF_32LE, Encoding.UTF_8,
            Encoding.UTF_16BE, Encoding.UTF_16LE,
    };

    /**
     * Détection d'encodage, par marque d'ordre puis heuristique.
     *
     * @param bytes octets du fichier
     * @return encodage détecté et présence de la marque
     */
    public static Detection detect(byte[] bytes) {
        for (int i = 0; i < MARQUES.length; i++) {
            if (startsWith(bytes, MARQUES[i])) {
                return new Detection(ENCODINGS_DE_MARQUE[i], true, MARQUES[i].length, true);
            }
        }
        return new Detection(heuristicEncoding(bytes), false, 0, false);
    }

    /**
     * Décode les octets avec l'encodage détecté automatiquement.
     *
     * @param bytes octets du fichier
     * @return texte décodé, encodage constaté et erreurs éventuelles
     */
    public static DecodeResult decode(byte[] bytes) {
        return decode(bytes, detect(bytes).encoding());
    }

    /**
     * Décode les octets avec l'encodage imposé.
     *
     * <p>Une marque d'ordre cohérente avec l'encodage imposé est
     * retirée du texte ; une marque étrangère est décodée comme
     * contenu (le caractère U+FEFF apparaît alors en tête).</p>
     *
     * @param bytes octets du fichier
     * @param encoding encodage à appliquer
     * @return texte décodé et erreurs éventuelles
     */
    public static DecodeResult decode(byte[] bytes, Encoding encoding) {
        int bomLength = bomLength(bytes, encoding);
        List<DecodeError> errors = new ArrayList<>();
        String text = switch (encoding) {
            case UTF_8 -> Utf8Codec.decode(bytes, bomLength, bytes.length, errors);
            case UTF_16LE -> WideCodecs.decodeUtf16(bytes, bomLength, bytes.length, false, errors);
            case UTF_16BE -> WideCodecs.decodeUtf16(bytes, bomLength, bytes.length, true, errors);
            case UTF_32LE -> WideCodecs.decodeUtf32(bytes, bomLength, bytes.length, false, errors);
            case UTF_32BE -> WideCodecs.decodeUtf32(bytes, bomLength, bytes.length, true, errors);
            case LATIN_1 -> decodeLatin1(bytes, bomLength);
        };
        return new DecodeResult(text, encoding, bomLength > 0, List.copyOf(errors));
    }

    /**
     * Encode le texte dans l'encodage voulu, avec ou sans marque.
     *
     * @param text texte à encoder
     * @param encoding encodage cible
     * @param withBom écrit la marque d'ordre en tête si {@code true}
     *         (sans effet pour le Latin-1, qui n'a pas de marque)
     * @return octets encodés
     * @throws IllegalArgumentException si le texte contient un
     *         substitut isolé ou un caractère hors de la page cible,
     *         inencodable sans perte
     */
    public static byte[] encode(String text, Encoding encoding, boolean withBom) {
        return switch (encoding) {
            case UTF_8 -> Utf8Codec.encode(text, withBom);
            case UTF_16LE -> WideCodecs.encodeUtf16(text, false, withBom);
            case UTF_16BE -> WideCodecs.encodeUtf16(text, true, withBom);
            case UTF_32LE -> WideCodecs.encodeUtf32(text, false, withBom);
            case UTF_32BE -> WideCodecs.encodeUtf32(text, true, withBom);
            case LATIN_1 -> encodeLatin1(text);
        };
    }

    /**
     * Détecte le mode dominant de fins de ligne d'un texte.
     *
     * @param text texte à inspecter
     * @return mode dominant, ou {@code MIXED} si plusieurs coexistent
     */
    public static LineEnding detectLineEndings(String text) {
        int lf = 0;
        int crlf = 0;
        int cr = 0;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    crlf++;
                    i++;
                } else {
                    cr++;
                }
            } else if (current == '\n') {
                lf++;
            }
        }
        return dominantLineEnding(lf, crlf, cr);
    }

    private static LineEnding dominantLineEnding(int lf, int crlf, int cr) {
        int kinds = (lf > 0 ? 1 : 0) + (crlf > 0 ? 1 : 0) + (cr > 0 ? 1 : 0);
        if (kinds == 0) {
            return LineEnding.NONE;
        }
        if (kinds > 1) {
            return LineEnding.MIXED;
        }
        if (lf > 0) {
            return LineEnding.LF;
        }
        return crlf > 0 ? LineEnding.CRLF : LineEnding.CR;
    }

    private static Encoding heuristicEncoding(byte[] bytes) {
        int sample = Math.min(bytes.length, 64);
        if (sample >= 4) {
            boolean utf32be = bytes[0] == 0 && bytes[1] == 0 && bytes[3] != 0;
            boolean utf32le = bytes[0] != 0 && bytes[2] == 0 && bytes[3] == 0;
            boolean utf16be = bytes[0] == 0 && bytes[1] != 0 && bytes[2] == 0 && bytes[3] != 0;
            boolean utf16le = bytes[0] != 0 && bytes[1] == 0 && bytes[2] != 0 && bytes[3] == 0;
            if (utf32be && stableZeros(bytes, sample, 4, 0, 2)) {
                return Encoding.UTF_32BE;
            }
            if (utf32le && stableZeros(bytes, sample, 4, 2, 4)) {
                return Encoding.UTF_32LE;
            }
            if (utf16be && stableZeros(bytes, sample, 2, 0, 1)) {
                return Encoding.UTF_16BE;
            }
            if (utf16le && stableZeros(bytes, sample, 2, 1, 2)) {
                return Encoding.UTF_16LE;
            }
        }
        return Encoding.UTF_8;
    }

    private static boolean stableZeros(byte[] bytes, int sample, int unit, int from, int to) {
        int groups = 0;
        int zeros = 0;
        for (int i = 0; i + unit <= sample; i += unit) {
            groups++;
            for (int k = from; k < to; k++) {
                if (bytes[i + k] == 0) {
                    zeros++;
                }
            }
        }
        return groups >= 2 && zeros == groups * (to - from);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int bomLength(byte[] bytes, Encoding encoding) {
        for (int i = 0; i < MARQUES.length; i++) {
            if (ENCODINGS_DE_MARQUE[i] == encoding && startsWith(bytes, MARQUES[i])) {
                return MARQUES[i].length;
            }
        }
        return 0;
    }

    /** Latin-1 : chaque octet est un caractère, aucun échec possible. */
    private static String decodeLatin1(byte[] bytes, int start) {
        StringBuilder out = new StringBuilder(bytes.length - start);
        for (int i = start; i < bytes.length; i++) {
            out.append((char) (bytes[i] & 0xFF));
        }
        return out.toString();
    }

    private static byte[] encodeLatin1(String text) {
        byte[] out = new byte[text.length()];
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (unit > 0xFF) {
                throw new IllegalArgumentException(
                    "character U+" + Integer.toHexString(unit).toUpperCase(Locale.ROOT)
                        + " cannot be encoded in Latin-1 at offset " + i);
            }
            out[i] = (byte) unit;
        }
        return out;
    }

    /**
     * Encodage détecté pour un fichier.
     *
     * @param encoding encodage retenu
     * @param hadBom présence d'une marque d'ordre des octets
     * @param bomLength longueur de la marque, en octets
     * @param certain {@code true} si la décision vient d'une marque,
     *        {@code false} si elle vient de l'heuristique
     */
    public record Detection(Encoding encoding, boolean hadBom, int bomLength, boolean certain) {
    }

    /**
     * Erreur de décodage, avec son décalage d'octet d'origine.
     *
     * @param offset décalage de l'octet fautif dans le fichier
     * @param message description concise et greppable
     */
    public record DecodeError(int offset, String message) {
    }

    /**
     * Résultat d'un décodage strict.
     *
     * @param text texte décodé, avec U+FFFD aux points d'erreur
     * @param encoding encodage effectivement appliqué
     * @param hadBom marque d'ordre retirée du texte
     * @param errors erreurs rencontrées, éventuellement vide
     */
    public record DecodeResult(
            String text, Encoding encoding, boolean hadBom, List<DecodeError> errors) {
    }

    /**
     * Mode de fin de ligne dominant d'un texte.
     */
    public enum LineEnding {

        /** Aucune fin de ligne dans le texte. */
        NONE,

        /** Saut de ligne Unix, LF. */
        LF,

        /** Fin de ligne Windows, CRLF. */
        CRLF,

        /** Retour chariot isolé, CR. */
        CR,

        /** Plusieurs modes coexistent. */
        MIXED
    }
}
