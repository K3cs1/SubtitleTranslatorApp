package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobStatusResponse;
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
    private final TranslationJobStore jobStore;

    @Value("${translation.batch-size}")
    private int batchSize;

    /**
     * Safety cap to avoid creating oversized prompts for the LLM.
     * This is an approximate character budget of the user payload (markers + entry text).
     */
    @Value("${translation.max-batch-chars:12000}")
    private int maxBatchChars;

    // Max parallel in-flight translation calls
    @Value("${translation.max-parallel}")
    private int maxParallel;

    @Override
    public CompletableFuture<Path> translateInBackground(TranslationJobRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Request input path: {}", request.inputPath());
                Path input = request.inputPath();
                List<SrtEntry> entries = SrtIOParser.parse(input);

                List<SrtEntry> translated = translateAll(entries, request.targetLanguage(), request.jobId());

                Path output = outputPath(request.inputPath(), request.targetLanguage());
                SrtIOParser.write(output, translated);
                return output;
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new TranslationFailedException("Translation failed: " + e.getMessage());
            }
        }, executor);
    }

    private List<SrtEntry> translateAll(List<SrtEntry> entries, String targetLanguage, String jobId) {

        // Thread-safe result map
        final Map<Integer, List<String>> translatedTextByIndex = new ConcurrentHashMap<>();

        // For progress reporting
        final var done = new AtomicInteger(0);
        final int totalEntries = entries.size();

        // Concurrency limiter (even with virtual threads)
        final var semaphore = new Semaphore(this.maxParallel);

        // Build batches using an entry-count limit + a payload-size safety cap
        final List<List<SrtEntry>> batches = buildBatches(entries, this.batchSize, this.maxBatchChars);

        List<CompletableFuture<Void>> futures = new ArrayList<>(batches.size());

        for (List<SrtEntry> batch : batches) {
            futures.add(CompletableFuture.runAsync(() -> {
                boolean acquired = false;
                try {
                    semaphore.acquire();
                    acquired = true;

                    Map<Integer, List<String>> batchResult = translator.translateBatch(batch, targetLanguage);
                    translatedTextByIndex.putAll(batchResult);

                    int finished = done.addAndGet(batch.size());
                    log.info("Translated {}/{} entries", finished, entries.size());

                    // Update progress in job store if jobId is provided
                    if (jobId != null && !jobId.isBlank() && jobStore != null) {
                        TranslationJobStatusResponse currentStatus = jobStore.get(jobId);
                        if (currentStatus != null) {
                            String inputFileName = currentStatus.inputFileName();
                            jobStore.store(jobId, TranslationJobStatusResponse.processing(
                                    jobId, inputFileName, finished, totalEntries));
                        }
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error(ie.getMessage());
                    throw new TranslationFailedException(ie.getMessage());
                } catch (IOException ioe) {
                    log.error(ioe.getMessage());
                    throw new TranslationFailedException(ioe.getMessage());
                } finally {
                    if (acquired) {
                        semaphore.release();
                    }
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

    private static List<List<SrtEntry>> buildBatches(List<SrtEntry> entries, int batchSize, int maxBatchChars) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        int safeBatchSize = Math.max(1, batchSize);
        int safeMaxChars = Math.max(512, maxBatchChars);

        final List<List<SrtEntry>> batches = new ArrayList<>();
        List<SrtEntry> current = new ArrayList<>(Math.min(safeBatchSize, entries.size()));
        int currentChars = 0;

        for (SrtEntry e : entries) {
            // Approximate per-entry payload length: markers + entry text + newlines
            int entryChars = 40 + (e == null ? 0 : e.originalText().length());

            boolean wouldExceedCount = current.size() >= safeBatchSize;
            boolean wouldExceedChars = !current.isEmpty() && (currentChars + 1 + entryChars) > safeMaxChars;

            if (wouldExceedCount || wouldExceedChars) {
                batches.add(current);
                current = new ArrayList<>(Math.min(safeBatchSize, entries.size()));
                currentChars = 0;
            }

            current.add(e);
            currentChars += entryChars + 1;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    private Path outputPath(Path input, String targetLanguage) {
        Path normalizedInput = input.toAbsolutePath().normalize();
        String name = normalizedInput.getFileName().toString();
        log.debug("Input name: {}", name);
        String base = name.toLowerCase().endsWith(".srt") ? name.substring(0, name.length() - 4) : name;
        String suffix = targetLanguage == null ? "" : targetLanguage.toLowerCase();
        suffix = suffix.replaceAll("[^a-z0-9]+", "-");
        suffix = suffix.replaceAll("(^-+|-+$)", "");
        if (suffix.isBlank()) {
            suffix = "translated";
        }
        if (suffix.length() > 24) {
            suffix = suffix.substring(0, 24);
        }
        String outName = base + "_" + suffix + ".srt";
        Path userHome = Path.of(System.getProperty("user.home"));
        return userHome.resolve(outName);
    }
}
