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

            int index = Integer.parseInt(all.get(i).trim());
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
