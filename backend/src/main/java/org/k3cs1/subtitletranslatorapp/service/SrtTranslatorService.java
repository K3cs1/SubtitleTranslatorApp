package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.model.SrtEntry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SrtTranslatorService {
    Map<Integer, List<String>> translateBatch(List<SrtEntry> batch, String targetLanguage) throws IOException;
}
