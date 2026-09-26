package org.k3cs1.subtitletranslatorapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "deepl.auth-key=smoke-test-key")
class SubtitleTranslatorAppSmokeTest {

    @Test
    void contextLoads() {
        // Verifies the application context starts cleanly without OpenAI / Spring AI
    }
}
