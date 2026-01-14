package org.k3cs1.subtitletranslatorapp;

import org.k3cs1.subtitletranslatorapp.service.TranslationJobService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

@SpringBootApplication
public class SubtitleTranslatorApp {

    public static void main(String[] args) {
        SpringApplication.run(SubtitleTranslatorApp.class, args);
    }

    @Bean
    ApplicationRunner runner(TranslationJobService jobs) {
        return args -> {
            System.out.print("Enter path to .srt file: ");
            try (var scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
                String raw = scanner.nextLine().trim();
                Path input = Path.of(raw);

                if (!Files.exists(input) || !Files.isRegularFile(input) || !raw.toLowerCase().endsWith(".srt")) {
                    System.err.println("Invalid input. Provide an existing .srt file path.");
                    return;
                }

                var future = jobs.translateInBackground(input);

                // Don’t block the main thread; just attach callbacks
                future.whenComplete((out, ex) -> {
                    if (ex != null) {
                        System.err.println("Translation failed: " + ex.getMessage());
                    } else {
                        System.out.println("Done! Saved to: " + out.toAbsolutePath());
                    }
                });

                System.out.println("Translation started in background.");
                System.out.println("You can keep this process running until completion.");
            }
        };
    }

}
