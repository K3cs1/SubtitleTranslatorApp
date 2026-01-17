package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.dto.TranslationJobStatusResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TranslationJobStore {

    private final Map<String, TranslationJobStatusResponse> jobs = new ConcurrentHashMap<>();

    public void store(String jobId, TranslationJobStatusResponse status) {
        jobs.put(jobId, status);
    }

    public TranslationJobStatusResponse get(String jobId) {
        return jobs.get(jobId);
    }

    public void remove(String jobId) {
        jobs.remove(jobId);
    }
}
