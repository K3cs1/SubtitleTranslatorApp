package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.exception.TranslationFailedException;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.k3cs1.subtitletranslatorapp.parser.SrtIOParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationJobServiceImpl implements TranslationJobService {

    private final SrtTranslatorService translator;
    private final ExecutorService executor;

    @Value("${translation.batch-size}")
    private int batchSize;

    // Max parallel in-flight translation calls
    @Value("${translation.max-parallel}")
    private int maxParallel;

    public static final String SRT_EXTENSION = ".srt";
    public static final String HUN_SRT = "_hun.srt";

    @Override
    public CompletableFuture<Path> translateInBackground(TranslationJobRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Request input path: {}", request.inputPath());
                Path input = request.inputPath();
                List<SrtEntry> entries = SrtIOParser.parse(input);

                List<SrtEntry> translated = translateAll(entries);

                Path output = outputPath(request.inputPath());
                SrtIOParser.write(output, translated);
                return output;
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new TranslationFailedException("Translation failed: " + e.getMessage());
            }
        }, executor);
    }

    private List<SrtEntry> translateAll(List<SrtEntry> entries) {

        // Thread-safe result map
        final Map<Integer, List<String>> translatedTextByIndex = new ConcurrentHashMap<>();

        // For progress reporting
        final var done = new AtomicInteger(0);

        // Concurrency limiter (even with virtual threads)
        final var semaphore = new Semaphore(this.maxParallel);

        // Build batch list first to avoid subList surprises and to count batches
        final List<List<SrtEntry>> batches = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += this.batchSize) {
            batches.add(entries.subList(i, Math.min(i + this.batchSize, entries.size())));
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
                    log.error(ie.getMessage());
                    throw new TranslationFailedException(ie.getMessage());
                } catch (IOException ioe) {
                    log.error(ioe.getMessage());
                    throw new TranslationFailedException(ioe.getMessage());
                } finally {
                    semaphore.release();
                }
            }, executor));
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
        Path normalizedInput = input.toAbsolutePath().normalize();
        String name = normalizedInput.getFileName().toString();
        log.debug("Input name: {}", name);
        String base = name.endsWith(SRT_EXTENSION) ? name.substring(0, name.length() - 4) : name;
        String outName = base + HUN_SRT;
        Path userHome = Path.of(System.getProperty("user.home"));
        return userHome.resolve(outName);
    }
}
