package org.k3cs1.subtitletranslatorapp.model;

import java.util.List;

public record SrtEntry(int index, String timeRange, List<String> lines) {
    public String originalText() {
        return String.join("\n", lines);
    }
}
