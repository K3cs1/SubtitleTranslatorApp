package org.k3cs1.subtitletranslatorapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class SubtitleTranslatorApp {

    public static void main(String[] args) {
        SpringApplication.run(SubtitleTranslatorApp.class, args);
    }

//    @Bean
//    ApplicationRunner runner(TranslationJobService jobs) {
//        return args -> {
//            System.out.println("Enter path to .srt file: ");
//            try (var scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
//                String raw = scanner.nextLine().trim();
//                Path input = Path.of(raw);
//
//                if (!Files.exists(input) || !Files.isRegularFile(input) || !raw.toLowerCase().endsWith(".srt")) {
//                    log.error("Invalid input. Provide an existing .srt file path.");
//                    return;
//                }
//
//                var future = jobs.translateInBackground(new TranslationJobRequest(input));
//
//                // Don’t block the main thread; just attach callbacks
//                future.whenComplete((out, ex) -> {
//                    if (ex != null) {
//                        log.error("Translation failed: {}", ex.getMessage());
//                    } else {
//                        log.info("Done! Saved to: {}", out.toAbsolutePath());
//                    }
//                });
//
//                log.info("Translation started in background.");
//                log.info("You can keep this process running until completion.");
//            }
//        };
//    }

}
