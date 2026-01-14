package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.model.SrtEntry;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SrtTranslatorService {

    private final DeeplTranslatorService deepl;

    public SrtTranslatorService(DeeplTranslatorService deepl) {
        this.deepl = deepl;
    }

    public Map<Integer, List<String>> translateBatch(List<SrtEntry> batch) {
        List<String> texts = batch.stream()
                .map(SrtEntry::originalText)
                .toList();

        List<String> translated = deepl.translateEnToHu(texts);

        Map<Integer, List<String>> out = new LinkedHashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            out.put(
                    batch.get(i).index(),
                    List.of(translated.get(i).split("\\R"))
            );
        }
        return out;
    }
}
