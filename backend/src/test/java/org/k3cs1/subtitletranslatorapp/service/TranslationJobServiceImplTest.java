package org.k3cs1.subtitletranslatorapp.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;
import org.k3cs1.subtitletranslatorapp.exception.TranslationFailedException;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationJobServiceImplTest {

    @Mock
    private SrtTranslatorService translator;

    @Mock
    private TranslationJobStore jobStore;

    private ExecutorService executor;
    private TranslationJobServiceImpl service;

    @BeforeEach
    @SuppressWarnings({"NullAway", "nullness"})
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        service = new TranslationJobServiceImpl(translator, executor, jobStore);
        TranslationJobServiceImpl target = Objects.requireNonNull(service);
        ReflectionTestUtils.setField(target, "batchSize", 1);
        ReflectionTestUtils.setField(target, "maxParallel", 2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void translateInBackground_writesTranslatedFile(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("movie.srt");
        Files.writeString(input, """
                1
                00:00:01,000 --> 00:00:02,000
                Hello

                2
                00:00:03,000 --> 00:00:04,000
                World

                """);

        when(translator.translateBatch(anyList(), eq("HU")))
                .thenAnswer(invocation -> {
                    List<SrtEntry> batch = invocation.getArgument(0);
                    Map<Integer, List<String>> result = new HashMap<>();
                    for (SrtEntry entry : batch) {
                        String translated = "Translated: " + String.join(" ", entry.lines());
                        result.put(entry.index(), List.of(translated));
                    }
                    return result;
                });

        Path output = service.translateInBackground(new TranslationJobRequest(input, "HU")).join();

        assertThat(Files.exists(output)).isTrue();
        List<String> lines = Files.readAllLines(output);
        List<String> expected = List.of(
                "1",
                "00:00:01,000 --> 00:00:02,000",
                "Translated: Hello",
                "",
                "2",
                "00:00:03,000 --> 00:00:04,000",
                "Translated: World",
                ""
        );
        assertThat(lines).containsExactlyElementsOf(expected);
        verify(translator, times(2)).translateBatch(anyList(), eq("HU"));

        Files.deleteIfExists(output);
    }

    @Test
    void translateInBackground_wrapsTranslatorFailure(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("movie.srt");
        Files.writeString(input, """
                1
                00:00:01,000 --> 00:00:02,000
                Hello

                """);

        when(translator.translateBatch(anyList(), eq("EN")))
                .thenThrow(new IOException("boom"));

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> service.translateInBackground(new TranslationJobRequest(input, "EN")).join()
        );

        assertThat(thrown.getCause()).isInstanceOf(TranslationFailedException.class);
        assertThat(thrown.getCause().getMessage())
                .contains("Translation failed:")
                .contains("Parallel translation failed: boom");
    }

    @Test
    void translateInBackground_wrapsParseFailure(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("bad.srt");
        Files.writeString(input, "not a valid srt");

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> service.translateInBackground(new TranslationJobRequest(input, "EN")).join()
        );

        assertThat(thrown.getCause()).isInstanceOf(TranslationFailedException.class);
        assertThat(thrown.getCause().getMessage())
                .contains("Translation failed:")
                .contains("Invalid SRT index line");
        verifyNoInteractions(translator);
    }
}
