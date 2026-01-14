package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.k3cs1.subtitletranslatorapp.exception.TranslationFailedException;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.k3cs1.subtitletranslatorapp.parser.SrtIOParser;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
                throw new TranslationFailedException("Translation failed: " + e.getMessage());
            }
        }, executor);
    }

    private List<SrtEntry> translateAll(List<SrtEntry> entries) {
        final int batchSize = 30;

        // Max parallel in-flight translation calls
        final int maxParallel = 5;

        // Thread-safe result map
        final Map<Integer, List<String>> translatedTextByIndex = new ConcurrentHashMap<>();

        // For progress reporting
        final var done = new AtomicInteger(0);

        // Concurrency limiter (even with virtual threads)
        final var semaphore = new Semaphore(maxParallel);

        // Build batch list first to avoid subList surprises and to count batches
        final List<List<SrtEntry>> batches = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += batchSize) {
            batches.add(entries.subList(i, Math.min(i + batchSize, entries.size())));
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(batches.size());

        for (List<SrtEntry> batch : batches) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();

                    Map<Integer, List<String>> batchResult = translator.translateBatch(batch);
                    translatedTextByIndex.putAll(batchResult);

                    int finished = done.addAndGet(batch.size());
                    log.info("Translated {}/{} entries", finished, entries.size());

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TranslationFailedException(ie.getMessage());
                } finally {
                    semaphore.release();
                }
            }, executor)); // <-- your virtual-thread ExecutorService / Executor
        }

        // Wait for all batches to finish (propagate errors)
        try {
            CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            ).join();
        } catch (CompletionException ce) {
            // Unwrap to keep logs readable
            Throwable root = ce.getCause() != null ? ce.getCause() : ce;
            throw new TranslationFailedException("Parallel translation failed: " + root.getMessage());
        }

        // Reassemble in original order
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
