package org.k3cs1.subtitletranslatorapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"NullAway", "nullness"})
class SrtTranslatorServiceImplTest {

    @Mock
    private ChatClient.Builder builder;

    @Mock
    private ChatClient chatClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private SrtTranslatorServiceImpl service;

    @BeforeEach
    @SuppressWarnings({"NullAway", "nullness"})
    void setUp() {
        service = new SrtTranslatorServiceImpl(builder);
        SrtTranslatorServiceImpl target = Objects.requireNonNull(service);
        byte[] systemBytes = "System: {{TARGET_LANGUAGE}}".getBytes(StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(
                target,
                "systemMessageResource",
                new ByteArrayResource(Objects.requireNonNull(systemBytes))
        );
        when(builder.build()).thenReturn(chatClient);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        service.init();
    }

    @Test
    @SuppressWarnings({"NullAway", "nullness"})
    void translateBatch_returnsParsedTranslations() throws IOException {
        List<SrtEntry> batch = List.of(
                new SrtEntry(1, "00:00:01,000 --> 00:00:02,000", List.of("Hello", "World")),
                new SrtEntry(2, "00:00:03,000 --> 00:00:04,000", List.of("Goodbye"))
        );
        String response = """
                <<<ENTRY 1>>>
                Hola
                Mundo
                <<<END>>>
                <<<ENTRY 2>>>
                Adios
                <<<END>>>
                """;
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(response);

        Map<Integer, List<String>> result = service.translateBatch(batch, "  HU  ");

        assertThat(result)
                .containsEntry(1, List.of("Hola", "Mundo"))
                .containsEntry(2, List.of("Adios"))
                .hasSize(2);

        verify(requestSpec).system("System: HU");
        String expectedUser = String.join("\n",
                "Translate this SRT text payload:",
                "",
                "<<<ENTRY 1>>>",
                "Hello",
                "World",
                "<<<END>>>",
                "<<<ENTRY 2>>>",
                "Goodbye",
                "<<<END>>>"
        );
        verify(requestSpec).user(Objects.requireNonNull(expectedUser));
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
        verifyNoInteractions(requestSpec, responseSpec);
    }

    @Test
    void translateBatch_throwsOnMalformedResponse() throws IOException {
        List<SrtEntry> batch = List.of(
                new SrtEntry(1, "00:00:01,000 --> 00:00:02,000", List.of("Hello"))
        );
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("<<<ENTRY X>>>\nBoom\n<<<END>>>");

        assertThrows(NumberFormatException.class, () -> service.translateBatch(batch, "EN"));
    }
}
