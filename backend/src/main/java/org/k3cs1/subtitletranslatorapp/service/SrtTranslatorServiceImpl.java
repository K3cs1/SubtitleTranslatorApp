package org.k3cs1.subtitletranslatorapp.service;

import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SrtTranslatorServiceImpl implements SrtTranslatorService {

    private final DeeplTranslatorService deeplTranslatorService;

    @Override
    public Map<Integer, List<String>> translateBatch(List<SrtEntry> batch, String targetLanguage) {
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Target language is required.");
        }
        if (batch == null || batch.isEmpty()) {
            return Map.of();
        }

        List<String> texts = batch.stream().map(SrtEntry::originalText).toList();
        List<String> translated = deeplTranslatorService.translate(texts, targetLanguage.trim());

        Map<Integer, List<String>> out = new LinkedHashMap<>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            SrtEntry entry = batch.get(i);
            String translatedText = translated.get(i) == null ? "" : translated.get(i);
            List<String> lines = Arrays.asList(translatedText.split("\\R", -1));
            out.put(entry.index(), lines);
        }
        return out;
    }
}
