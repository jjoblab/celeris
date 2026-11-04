package jo.celeris.core.text;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Codecs UTF-16 et UTF-32 stricts, dans leurs deux orientations —
 * visibles du paquet seulement, la surface publique étant
 * {@link EncodingCodec}.
 *
 * <p>Chaque erreur (unité orpheline, octet traînant, point de code hors
 * plage ou dans les substituts) produit un remplacement U+FFFD et une
 * entrée d'erreur portant son décalage d'octet ; l'encodage refuse les
 * substituts isolés.</p>
 */
final class WideCodecs {

    private WideCodecs() {
    }

    /** Décode strictement l'UTF-16 des octets {@code [start, end)}. */
    static String decodeUtf16(byte[] bytes, int start, int end, boolean bigEndian,
            List<EncodingCodec.DecodeError> errors) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < end) {
            if (end - i < 2) {
                errors.add(new EncodingCodec.DecodeError(i, "odd trailing byte"));
                out.append('\uFFFD');
                break;
            }
            char unit = unit16(bytes, i, bigEndian);
            if (Character.isHighSurrogate(unit)) {
                if (end - i < 4) {
                    errors.add(new EncodingCodec.DecodeError(i, "unpaired high surrogate"));
                    out.append('\uFFFD');
                    i += 2;
                    continue;
                }
                char next = unit16(bytes, i + 2, bigEndian);
                if (!Character.isLowSurrogate(next)) {
                    errors.add(new EncodingCodec.DecodeError(i, "unpaired high surrogate"));
                    out.append('\uFFFD');
                    i += 2;
                    continue;
                }
                out.append(unit).append(next);
                i += 4;
            } else if (Character.isLowSurrogate(unit)) {
                errors.add(new EncodingCodec.DecodeError(i, "unpaired low surrogate"));
                out.append('\uFFFD');
                i += 2;
            } else {
                out.append(unit);
                i += 2;
            }
        }
        return out.toString();
    }

    /** Décode strictement l'UTF-32 des octets {@code [start, end)}. */
    static String decodeUtf32(byte[] bytes, int start, int end, boolean bigEndian,
            List<EncodingCodec.DecodeError> errors) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < end) {
            if (end - i < 4) {
                errors.add(new EncodingCodec.DecodeError(i,
                        "trailing bytes without full UTF-32 unit"));
                break;
            }
            int codePoint = bigEndian
                    ? ((bytes[i] & 0xFF) << 24) | ((bytes[i + 1] & 0xFF) << 16)
                    | ((bytes[i + 2] & 0xFF) << 8) | (bytes[i + 3] & 0xFF)
                    : ((bytes[i + 3] & 0xFF) << 24) | ((bytes[i + 2] & 0xFF) << 16)
                    | ((bytes[i + 1] & 0xFF) << 8) | (bytes[i] & 0xFF);
            if (codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
                errors.add(new EncodingCodec.DecodeError(i, "invalid UTF-32 code point"));
                out.append('\uFFFD');
            } else {
                out.appendCodePoint(codePoint);
            }
            i += 4;
        }
        return out.toString();
    }

    /** Encode le texte en UTF-16, marque en tête si demandée. */
    static byte[] encodeUtf16(String text, boolean bigEndian, boolean withBom) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (withBom) {
            writeUnit(out, 0xFEFF, bigEndian);
        }
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (Character.isHighSurrogate(unit)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
                }
                writeUnit(out, unit, bigEndian);
                writeUnit(out, text.charAt(i + 1), bigEndian);
                i++;
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
            } else {
                writeUnit(out, unit, bigEndian);
            }
        }
        return out.toByteArray();
    }

    /** Encode le texte en UTF-32, marque en tête si demandée. */
    static byte[] encodeUtf32(String text, boolean bigEndian, boolean withBom) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (withBom) {
            writeUnit32(out, 0xFEFF, bigEndian);
        }
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                throw new IllegalArgumentException("unpaired surrogate cannot be encoded");
            }
            i += Character.charCount(codePoint) - 1;
            writeUnit32(out, codePoint, bigEndian);
        }
        return out.toByteArray();
    }

    private static char unit16(byte[] bytes, int index, boolean bigEndian) {
        return bigEndian
                ? (char) (((bytes[index] & 0xFF) << 8) | (bytes[index + 1] & 0xFF))
                : (char) (((bytes[index + 1] & 0xFF) << 8) | (bytes[index] & 0xFF));
    }

    private static void writeUnit(ByteArrayOutputStream out, int unit, boolean bigEndian) {
        if (bigEndian) {
            out.write((unit >> 8) & 0xFF);
            out.write(unit & 0xFF);
        } else {
            out.write(unit & 0xFF);
            out.write((unit >> 8) & 0xFF);
        }
    }

    private static void writeUnit32(ByteArrayOutputStream out, int codePoint, boolean bigEndian) {
        if (bigEndian) {
            out.write((codePoint >> 24) & 0xFF);
            out.write((codePoint >> 16) & 0xFF);
            out.write((codePoint >> 8) & 0xFF);
            out.write(codePoint & 0xFF);
        } else {
            out.write(codePoint & 0xFF);
            out.write((codePoint >> 8) & 0xFF);
            out.write((codePoint >> 16) & 0xFF);
            out.write((codePoint >> 24) & 0xFF);
        }
    }
}
