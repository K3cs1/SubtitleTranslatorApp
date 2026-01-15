package org.k3cs1.subtitletranslatorapp.parser;

import org.k3cs1.subtitletranslatorapp.model.SrtEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SrtIOParser {

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
