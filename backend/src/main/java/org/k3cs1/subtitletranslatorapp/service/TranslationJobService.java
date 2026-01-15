package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.dto.TranslationJobRequest;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface TranslationJobService {
    CompletableFuture<Path> translateInBackground(TranslationJobRequest request);
}
