package jo.celeris.core.text;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Codec UTF-8 strict — visible du paquet seulement, la surface publique
 * étant {@link EncodingCodec}.
 *
 * <p>Chaque erreur (octet de tête interdit, séquence tronquée, survol,
 * substitut encodé) produit un remplacement U+FFFD et une entrée
 * d'erreur portant son décalage d'octet ; l'encodage refuse les
 * substituts isolés.</p>
 */
final class Utf8Codec {

    private Utf8Codec() {
    }

    /** Décode strictement les octets {@code [start, end)}. */
    static String decode(byte[] bytes, int start, int end, List<EncodingCodec.DecodeError> errors) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < end) {
            int lead = bytes[i] & 0xFF;
            if (lead <= 0x7F) {
                out.append((char) lead);
                i++;
                continue;
            }
            Sequence seq = sequence(bytes, end, i, lead, errors);
            if (seq.codePoint() >= 0) {
                out.appendCodePoint(seq.codePoint());
            } else {
                out.append('\uFFFD');
            }
            i += seq.advance();
        }
        return out.toString();
    }

    /** Encode le texte, marque en tête si demandée. */
    static byte[] encode(String text, boolean withBom) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        if (withBom) {
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
        }
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (Character.isHighSurrogate(unit)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
                }
                encodeCodePoint(out, text.codePointAt(i));
                i++;
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
            } else {
                encodeCodePoint(out, unit);
            }
        }
        return out.toByteArray();
    }

    /**
     * La séquence UTF-8 commençant à {@code i} : soit un point de code
     * valide (avance de la longueur), soit une erreur (avance d'un
     * octet, remplacement) — jamais d'exception, le reste du fichier
     * doit survivre.
     */
    private static Sequence sequence(byte[] bytes, int end, int i, int lead,
            List<EncodingCodec.DecodeError> errors) {
        LengthBounds bornes = lengthBounds(lead);
        if (bornes == null) {
            errors.add(new EncodingCodec.DecodeError(i, "invalid UTF-8 lead byte"));
            return Sequence.INVALID;
        }
        if (i + bornes.length() > end) {
            errors.add(new EncodingCodec.DecodeError(i, "truncated UTF-8 sequence"));
            return Sequence.INVALID;
        }
        if (!continuationsValid(bytes, i, bornes)) {
            errors.add(new EncodingCodec.DecodeError(i, "invalid UTF-8 sequence"));
            return Sequence.INVALID;
        }
        int codePoint = lead & (bornes.length() == 2 ? 0x1F : bornes.length() == 3 ? 0x0F : 0x07);
        for (int k = 1; k < bornes.length(); k++) {
            codePoint = (codePoint << 6) | (bytes[i + k] & 0x3F);
        }
        return new Sequence(codePoint, bornes.length());
    }

    /**
     * La longueur attendue d'une séquence et les bornes de son premier
     * octet de continuation, ou {@code null} si la tête est interdite
     * (survol, substitut, plage hors Unicode).
     */
    private static @Nullable LengthBounds lengthBounds(int lead) {
        if (lead >= 0xC2 && lead <= 0xDF) {
            return new LengthBounds(2, 0x80, 0xBF);
        }
        if (lead >= 0xE0 && lead <= 0xEF) {
            return new LengthBounds(3, lead == 0xE0 ? 0xA0 : 0x80, lead == 0xED ? 0x9F : 0xBF);
        }
        if (lead >= 0xF0 && lead <= 0xF4) {
            return new LengthBounds(4, lead == 0xF0 ? 0x90 : 0x80, lead == 0xF4 ? 0x8F : 0xBF);
        }
        return null;
    }

    /** Les octets de continuation d'une séquence sont-ils dans les bornes. */
    private static boolean continuationsValid(byte[] bytes, int i, LengthBounds bornes) {
        int first = bytes[i + 1] & 0xFF;
        if (first < bornes.lower() || first > bornes.upper()) {
            return false;
        }
        for (int k = 2; k < bornes.length(); k++) {
            int continuation = bytes[i + k] & 0xFF;
            if (continuation < 0x80 || continuation > 0xBF) {
                return false;
            }
        }
        return true;
    }

    private static void encodeCodePoint(java.io.ByteArrayOutputStream out, int codePoint) {
        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
            throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
        }
        if (codePoint <= 0x7F) {
            out.write(codePoint);
        } else if (codePoint <= 0x7FF) {
            out.write(0xC0 | (codePoint >> 6));
            out.write(0x80 | (codePoint & 0x3F));
        } else if (codePoint <= 0xFFFF) {
            out.write(0xE0 | (codePoint >> 12));
            out.write(0x80 | ((codePoint >> 6) & 0x3F));
            out.write(0x80 | (codePoint & 0x3F));
        } else {
            out.write(0xF0 | (codePoint >> 18));
            out.write(0x80 | ((codePoint >> 12) & 0x3F));
            out.write(0x80 | ((codePoint >> 6) & 0x3F));
            out.write(0x80 | (codePoint & 0x3F));
        }
    }

    /** Une séquence UTF-8 : point de code valide ou erreur avançant d'un octet. */
    private record Sequence(int codePoint, int advance) {

        private static final Sequence INVALID = new Sequence(-1, 1);
    }

    /** Longueur attendue et bornes du premier octet de continuation. */
    private record LengthBounds(int length, int lower, int upper) {
    }
}
