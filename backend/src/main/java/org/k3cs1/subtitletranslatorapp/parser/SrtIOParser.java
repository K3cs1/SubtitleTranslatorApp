package org.k3cs1.subtitletranslatorapp.parser;

import org.k3cs1.subtitletranslatorapp.exception.InvalidArgumentException;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class SrtIOParser {

    private static final Pattern SRT_TIME_RANGE = Pattern.compile(
            "^\\s*\\d{2}:\\d{2}:\\d{2},\\d{3}\\s*-->\\s*\\d{2}:\\d{2}:\\d{2},\\d{3}(?:\\s+.*)?\\s*$"
    );

    /**
     * Lightweight content validation to reject non-SRT uploads (content-based, not extension-based).
     * Accepts typical SRT format:
     * - numeric index line
     * - time range line: "00:00:00,000 --> 00:00:01,000"
     */
    public static void validateSrtContent(Path path) throws IOException {
        if (path == null) {
            throw new InvalidArgumentException("Subtitle file path is required.");
        }

        int linesScanned = 0;
        int maxLinesToScan = 300;
        List<String> allLines = readSubtitleLines(path);

        for (int lineIdx = 0; lineIdx < allLines.size() && linesScanned < maxLinesToScan; ) {
            String line = allLines.get(lineIdx++);
            linesScanned++;
            String trimmed = line.replace("\uFEFF", "").trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String idx = trimmed.replaceAll("[^0-9]", "");
            if (idx.isEmpty()) {
                throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (missing numeric index line).");
            }

            while (lineIdx < allLines.size() && linesScanned < maxLinesToScan) {
                String timeLine = allLines.get(lineIdx++);
                linesScanned++;
                String t = timeLine.trim();
                if (t.isEmpty()) {
                    continue;
                }
                if (!SRT_TIME_RANGE.matcher(t).matches()) {
                    throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (invalid time range line).");
                }
                return;
            }

            throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (incomplete header).");
        }

        throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file.");
    }

    public static List<SrtEntry> parse(Path path) throws IOException {
        List<String> all = readSubtitleLines(path);
        List<SrtEntry> entries = new ArrayList<>();

        int i = 0;
        while (i < all.size()) {
            // skip empty lines
            while (i < all.size() && all.get(i).trim().isEmpty()) {
                i++;
            }
            if (i >= all.size()) {
                break;
            }

            String idxLine = all.get(i);

            // Remove UTF-8 BOM if present and trim
            idxLine = idxLine.replace("\uFEFF", "").trim();

            // Some files include odd whitespace; keep only digits for the index line
            idxLine = idxLine.replaceAll("[^0-9]", "");

            if (idxLine.isEmpty()) {
                throw new InvalidArgumentException("Invalid SRT index line at input line " + (i + 1) + ": '" + all.get(i) + "'");
            }

            int index = Integer.parseInt(idxLine);

            i++;

            if (i >= all.size()) {
                throw new InvalidArgumentException(
                        "Invalid SRT file: missing time range line after cue " + index + ".");
            }

            String timeRange = all.get(i).replace("\uFEFF", "").trim();
            if (!SRT_TIME_RANGE.matcher(timeRange).matches()) {
                throw new InvalidArgumentException(
                        "Invalid SRT file: bad time range after cue " + index + ".");
            }
            i++;

            List<String> lines = new ArrayList<>();
            while (i < all.size() && !all.get(i).trim().isEmpty()) {
                lines.add(all.get(i));
                i++;
            }

            entries.add(new SrtEntry(index, timeRange, lines));
        }
        return entries;
    }

    public static void write(Path out, List<SrtEntry> entries) throws IOException {
        List<String> lines = new ArrayList<>(entries.size() * 4);
        for (SrtEntry entry : entries) {
            lines.add(Integer.toString(entry.index()));
            lines.add(entry.timeRange());
            lines.addAll(entry.lines());
            lines.add(""); // blank line
        }
        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    /**
     * Many .srt files are not UTF-8 (e.g. Windows-1252 / Latin-1). Strict UTF-8 decoding throws
     * {@link CharacterCodingException}, which surfaces to clients as a generic read failure.
     */
    private static List<String> readSubtitleLines(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        String text;
        try {
            CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            text = utf8.decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException ex) {
            text = StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(raw)).toString();
        }
        return text.lines().toList();
    }

    private SrtIOParser() {
    }
}
