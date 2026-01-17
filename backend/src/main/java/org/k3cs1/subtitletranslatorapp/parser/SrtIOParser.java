package org.k3cs1.subtitletranslatorapp.parser;

import org.k3cs1.subtitletranslatorapp.exception.InvalidArgumentException;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;

import java.io.BufferedReader;
import java.io.IOException;
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

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;

            // Find the first non-empty line (index line), then the next non-empty line must be a time range.
            while ((line = reader.readLine()) != null && linesScanned < maxLinesToScan) {
                linesScanned++;
                String trimmed = line.replace("\uFEFF", "").trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // index line must be digits (some files include whitespace; allow that)
                String idx = trimmed.replaceAll("[^0-9]", "");
                if (idx.isEmpty()) {
                    throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (missing numeric index line).");
                }

                // now find the next non-empty line and validate time range format
                String timeLine;
                while ((timeLine = reader.readLine()) != null && linesScanned < maxLinesToScan) {
                    linesScanned++;
                    String t = timeLine.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    if (!SRT_TIME_RANGE.matcher(t).matches()) {
                        throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (invalid time range line).");
                    }
                    return; // looks like SRT
                }

                // file ended before a time range line appeared
                throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file (incomplete header).");
            }
        }

        throw new InvalidArgumentException("Uploaded file is not a valid .srt subtitle file.");
    }

    public static List<SrtEntry> parse(Path path) throws IOException {
        List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
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
                throw new IllegalArgumentException("Invalid SRT index line at input line " + (i + 1) + ": '" + all.get(i) + "'");
            }

            int index = Integer.parseInt(idxLine);

            i++;

            String timeRange = all.get(i);
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

    private SrtIOParser() {
    }
}
