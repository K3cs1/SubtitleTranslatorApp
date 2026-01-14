package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.k3cs1.subtitletranslatorapp.parser.SrtIOParser;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationJobService {

    private final SrtTranslatorService translator;
    private final ExecutorService executor;

    public CompletableFuture<Path> translateInBackground(Path input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SrtEntry> entries = SrtIOParser.parse(input);

                List<SrtEntry> translated = translateAll(entries);

                Path output = outputPath(input);
                SrtIOParser.write(output, translated);
                return output;
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RuntimeException("Translation failed: " + e.getMessage(), e);
            }
        }, executor);
    }

    private List<SrtEntry> translateAll(List<SrtEntry> entries) {
        // Simple fixed-size batching; you can evolve to token-based batching later.
        int batchSize = 30;

        Map<Integer, List<String>> translatedTextByIndex = new HashMap<>();

        for (int i = 0; i < entries.size(); i += batchSize) {
            List<SrtEntry> batch = entries.subList(i, Math.min(i + batchSize, entries.size()));
            Map<Integer, List<String>> batchResult = translator.translateBatch(batch);

            // Map each translated string back to SrtEntry lines

            translatedTextByIndex.putAll(batchResult);

            log.info("Translated {}/{} entries", Math.min(i + batchSize, entries.size()), entries.size());
        }

        List<SrtEntry> out = new ArrayList<>(entries.size());
        for (SrtEntry e : entries) {
            List<String> lines = translatedTextByIndex.getOrDefault(e.index(), e.lines());
            out.add(new SrtEntry(e.index(), e.timeRange(), lines));
        }
        return out;
    }

    private Path outputPath(Path input) {
        String name = input.getFileName().toString();
        String base = name.endsWith(".srt") ? name.substring(0, name.length() - 4) : name;
        String outName = base + "_hun.srt"; // as requested
        return input.getParent().resolve(outName);
    }
}
