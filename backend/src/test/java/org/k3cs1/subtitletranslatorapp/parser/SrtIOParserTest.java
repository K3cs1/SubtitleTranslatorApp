package org.k3cs1.subtitletranslatorapp.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SrtIOParserTest {

    @Test
    void parse_acceptsIso88591EncodedFile(@TempDir Path dir) throws Exception {
        // Byte 0xE9 is invalid as standalone UTF-8 but is "é" in ISO-8859-1 / Windows-1252
        byte[] latin1 = (
                "1\n"
                        + "00:00:01,000 --> 00:00:02,000\n"
                        + "caf\u00E9 scene\n"
                        + "\n"
        ).getBytes(StandardCharsets.ISO_8859_1);

        Path file = dir.resolve("test.srt");
        Files.write(file, latin1);

        SrtIOParser.validateSrtContent(file);
        List<?> entries = SrtIOParser.parse(file);
        assertEquals(1, entries.size());
    }

    @Test
    void parse_stillAcceptsUtf8(@TempDir Path dir) throws Exception {
        String utf8 = "1\n00:00:01,000 --> 00:00:02,000\nHello 世界\n\n";
        Path file = dir.resolve("utf8.srt");
        Files.writeString(file, utf8, StandardCharsets.UTF_8);

        SrtIOParser.validateSrtContent(file);
        assertFalse(SrtIOParser.parse(file).isEmpty());
    }
}
