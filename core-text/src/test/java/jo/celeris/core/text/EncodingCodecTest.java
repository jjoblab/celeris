package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests des encodages et des marques d'ordre des octets (parité A-24 :
 * UTF-8, UTF-16 et Latin-1, fins de ligne dominantes, marque préservée,
 * détection sans heuristique réseau).
 *
 * <p>Le décodage est strict : chaque erreur produit un remplacement
 * U+FFFD et une entrée portant son décalage d'octet — jamais d'exception
 * qui perdrait la fin du fichier.</p>
 */
class EncodingCodecTest {

    private static final String SAMPLE = "ligne un\nligne deux — accents éàü 🌍\n";

    @Test
    void detectsEveryBomFamily() {
        assertEquals(Encoding.UTF_8, EncodingCodec.detect(
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}).encoding());
        assertEquals(Encoding.UTF_16LE, EncodingCodec.detect(
                new byte[] {(byte) 0xFF, (byte) 0xFE}).encoding());
        assertEquals(Encoding.UTF_16BE, EncodingCodec.detect(
                new byte[] {(byte) 0xFE, (byte) 0xFF}).encoding());
        assertEquals(Encoding.UTF_32LE, EncodingCodec.detect(
                new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 0}).encoding());
        assertEquals(Encoding.UTF_32BE, EncodingCodec.detect(
                new byte[] {0, 0, (byte) 0xFE, (byte) 0xFF}).encoding());
    }

    @Test
    void utf32BomWinsOverSharedUtf16Prefix() {
        EncodingCodec.Detection detection = EncodingCodec.detect(
                new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 0});
        assertEquals(Encoding.UTF_32LE, detection.encoding());
        assertTrue(detection.hadBom());
        assertEquals(4, detection.bomLength());
    }

    @Test
    void roundTripsEveryEncodingWithAndWithoutBom() {
        for (Encoding encoding : Encoding.values()) {
            if (encoding == Encoding.LATIN_1) {
                continue;
            }
            byte[] withBom = EncodingCodec.encode(SAMPLE, encoding, true);
            EncodingCodec.Detection detection = EncodingCodec.detect(withBom);
            assertEquals(encoding, detection.encoding(), "détection pour " + encoding);
            assertTrue(detection.hadBom(), "marque pour " + encoding);
            assertEquals(SAMPLE, EncodingCodec.decode(withBom).text(),
                    "aller-retour avec marque pour " + encoding);

            byte[] withoutBom = EncodingCodec.encode(SAMPLE, encoding, false);
            assertEquals(SAMPLE, EncodingCodec.decode(withoutBom, encoding).text(),
                    "aller-retour sans marque pour " + encoding);
        }
    }

    /**
     * A-24 : le Latin-1 imposé décode et encode octet pour octet ; il
     * n'est jamais le fruit de la détection automatique (aucune marque
     * ne le désigne — l'hôte le force, la détection reste Unicode).
     */
    @Test
    void latin1RoundTripsAndIsNeverAutoDetected() {
        String latin = "Café simple, sans emoji";
        byte[] bytes = EncodingCodec.encode(latin, Encoding.LATIN_1, false);
        assertArrayEquals(latin.getBytes(StandardCharsets.ISO_8859_1), bytes);
        assertEquals(latin, EncodingCodec.decode(bytes, Encoding.LATIN_1).text());
        assertFalse(EncodingCodec.decode(bytes).hadBom());
        assertEquals(Encoding.UTF_8, EncodingCodec.detect(bytes).encoding());
        assertThrows(IllegalArgumentException.class,
                () -> EncodingCodec.encode("🌍", Encoding.LATIN_1, false));
    }

    @Test
    void bomlessUtf16RoundTripsOnLatinText() {
        String latin = "ABCDEFGHIJ";
        for (Encoding encoding : new Encoding[] {Encoding.UTF_16LE, Encoding.UTF_16BE}) {
            byte[] bytes = EncodingCodec.encode(latin, encoding, false);
            assertEquals(encoding, EncodingCodec.detect(bytes).encoding(),
                    "heuristique pour " + encoding);
            assertEquals(latin, EncodingCodec.decode(bytes).text());
        }
    }

    @Test
    void decodeStripsBomAndKeepsItOptional() {
        byte[] withBom = EncodingCodec.encode("é", Encoding.UTF_8, true);
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(withBom);
        assertEquals("é", decoded.text());
        assertTrue(decoded.hadBom());

        byte[] withoutBom = EncodingCodec.encode("é", Encoding.UTF_8, false);
        assertEquals("é", EncodingCodec.decode(withoutBom).text());
        assertFalse(EncodingCodec.decode(withoutBom).hadBom());
    }

    @Test
    void invalidUtf8ReportsOffsetsAndReplaces() {
        // 0x80 est un octet de continuation sans tête.
        EncodingCodec.DecodeResult broken = EncodingCodec.decode(
                new byte[] {'a', (byte) 0x80, '('});
        assertEquals("a\uFFFD(", broken.text());
        assertEquals(1, broken.errors().get(0).offset());
        assertTrue(broken.errors().get(0).message().contains("UTF-8"));

        // 0xC0 0x80 : survol (C0 interdit).
        EncodingCodec.DecodeResult overlong = EncodingCodec.decode(
                new byte[] {(byte) 0xC0, (byte) 0x80});
        assertFalse(overlong.errors().isEmpty());

        // 0xED 0xA0 0x80 : substitut encodé.
        EncodingCodec.DecodeResult surrogateHalf = EncodingCodec.decode(
                new byte[] {(byte) 0xED, (byte) 0xA0, (byte) 0x80});
        assertFalse(surrogateHalf.errors().isEmpty());

        // 0xF5 0x80... : au-delà d'U+10FFFF.
        EncodingCodec.DecodeResult outOfRange = EncodingCodec.decode(
                new byte[] {(byte) 0xF5, (byte) 0x80, (byte) 0x80, (byte) 0x80});
        assertFalse(outOfRange.errors().isEmpty());
    }

    @Test
    void truncatedUtf8SequenceIsReported() {
        EncodingCodec.DecodeResult truncated = EncodingCodec.decode(
                new byte[] {'a', (byte) 0xE2, (byte) 0x98});
        assertEquals(1, truncated.errors().get(0).offset());
        assertTrue(truncated.text().startsWith("a"));
    }

    @Test
    void utf16OddLengthReportsTrailingByte() {
        byte[] bytes = {(byte) 0xFF, (byte) 0xFE, 'a'};
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(bytes);
        assertEquals(Encoding.UTF_16LE, decoded.encoding());
        assertEquals("\uFFFD", decoded.text());
        assertEquals(1, decoded.errors().size());
        assertEquals(2, decoded.errors().get(0).offset());
    }

    @Test
    void utf16UnpairedSurrogatesAreReported() {
        byte[] bytes = {(byte) 0xD8, 0, (byte) 0xD8, 0};
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(bytes, Encoding.UTF_16BE);
        assertEquals("\uFFFD\uFFFD", decoded.text());
        assertEquals(2, decoded.errors().size());
    }

    @Test
    void utf32OutOfRangeCodePointIsReported() {
        byte[] bytes = {0, 0x11, 0, 1};
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(bytes, Encoding.UTF_32BE);
        assertEquals("\uFFFD", decoded.text());
        assertEquals(1, decoded.errors().size());
    }

    @Test
    void heuristicDetectsBomlessUtf16AndUtf32() {
        assertEquals(Encoding.UTF_16LE, EncodingCodec.detect(
                new byte[] {'A', 0, 'B', 0, 'C', 0}).encoding());
        assertEquals(Encoding.UTF_16BE, EncodingCodec.detect(
                new byte[] {0, 'A', 0, 'B', 0, 'C'}).encoding());
        assertEquals(Encoding.UTF_32LE, EncodingCodec.detect(
                new byte[] {'A', 0, 0, 0, 'B', 0, 0, 0}).encoding());
        assertEquals(Encoding.UTF_32BE, EncodingCodec.detect(
                new byte[] {0, 0, 0, 'A', 0, 0, 0, 'B'}).encoding());
    }

    @Test
    void plainAsciiDefaultsToUtf8() {
        byte[] bytes = "public class Main {}".getBytes(StandardCharsets.UTF_8);
        EncodingCodec.Detection detection = EncodingCodec.detect(bytes);
        assertEquals(Encoding.UTF_8, detection.encoding());
        assertFalse(detection.hadBom());
        assertEquals("public class Main {}", EncodingCodec.decode(bytes).text());
    }

    @Test
    void utf8HeuristicSurvivesPlainLatinText() {
        // « Café » en UTF-8 : octets non nuls variés, aucune raison de
        // voir des nuls réguliers.
        byte[] bytes = {'C', 'a', 'f', (byte) 0xC3, (byte) 0xA9};
        assertEquals(Encoding.UTF_8, EncodingCodec.detect(bytes).encoding());
    }

    @Test
    void encodeRejectsUnpairedSurrogates() {
        for (Encoding encoding : Encoding.values()) {
            assertThrows(IllegalArgumentException.class,
                    () -> EncodingCodec.encode("\uD800", encoding, false),
                    "substitut isolé accepté pour " + encoding);
            assertThrows(IllegalArgumentException.class,
                    () -> EncodingCodec.encode("a\uDC00b", encoding, true),
                    "substitut isolé bas accepté pour " + encoding);
        }
    }

    @Test
    void forcedEncodingDecodesForeignBomAsContent() {
        byte[] utf8Bom = EncodingCodec.encode("x", Encoding.UTF_8, true);
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(utf8Bom, Encoding.UTF_16LE);
        // EF BB BF 42 se lit comme deux unités UTF-16LE quelconques :
        // la marque étrangère est du contenu, pas une marque à retirer.
        assertEquals('\uBBEF', decoded.text().charAt(0));
        assertFalse(decoded.hadBom());
    }

    @Test
    void lineEndingDetectionReportsDominantMode() {
        assertEquals(EncodingCodec.LineEnding.LF,
                EncodingCodec.detectLineEndings("a\nb\nc\n"));
        assertEquals(EncodingCodec.LineEnding.CRLF,
                EncodingCodec.detectLineEndings("a\r\nb\r\n"));
        assertEquals(EncodingCodec.LineEnding.CR,
                EncodingCodec.detectLineEndings("a\rb"));
        assertEquals(EncodingCodec.LineEnding.MIXED,
                EncodingCodec.detectLineEndings("a\r\nb\nc\rd"));
        assertEquals(EncodingCodec.LineEnding.NONE,
                EncodingCodec.detectLineEndings("une seule ligne"));
    }

    @Test
    void decodeOfEmptyBytesIsAnEmptyText() {
        EncodingCodec.DecodeResult decoded = EncodingCodec.decode(new byte[0]);
        assertEquals("", decoded.text());
        assertEquals(Encoding.UTF_8, decoded.encoding());
        assertFalse(decoded.hadBom());
        assertArrayEquals(new byte[0], EncodingCodec.encode("", Encoding.UTF_8, false));
    }
}
