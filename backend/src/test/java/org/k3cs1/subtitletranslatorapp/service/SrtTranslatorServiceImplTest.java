package org.k3cs1.subtitletranslatorapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"NullAway", "nullness"})
class SrtTranslatorServiceImplTest {

    @Mock
    private DeeplTranslatorService deeplTranslatorService;

    private SrtTranslatorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SrtTranslatorServiceImpl(deeplTranslatorService);
    }

    @Test
    void translateBatch_returnsParsedTranslations() {
        List<SrtEntry> batch = List.of(
                new SrtEntry(1, "00:00:01,000 --> 00:00:02,000", List.of("Hello", "World")),
                new SrtEntry(2, "00:00:03,000 --> 00:00:04,000", List.of("Goodbye"))
        );
        when(deeplTranslatorService.translate(
                List.of("Hello\nWorld", "Goodbye"),
                "HU"
        )).thenReturn(List.of("Hola\nMundo", "Adios"));

        Map<Integer, List<String>> result = service.translateBatch(batch, "  HU  ");

        assertThat(result)
                .containsEntry(1, List.of("Hola", "Mundo"))
                .containsEntry(2, List.of("Adios"))
                .hasSize(2);
        verify(deeplTranslatorService).translate(List.of("Hello\nWorld", "Goodbye"), "HU");
    }

    @Test
    void translateBatch_throwsWhenTargetLanguageBlank() {
        List<SrtEntry> batch = List.of(
                new SrtEntry(1, "00:00:01,000 --> 00:00:02,000", List.of("Hello"))
        );

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> service.translateBatch(batch, " ")
        );

        assertThat(thrown.getMessage()).isEqualTo("Target language is required.");
        verifyNoInteractions(deeplTranslatorService);
    }
}
