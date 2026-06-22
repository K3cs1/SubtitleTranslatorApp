package org.k3cs1.subtitletranslatorapp.controller;

import org.junit.jupiter.api.Test;
import org.k3cs1.subtitletranslatorapp.service.TranslationJobService;
import org.k3cs1.subtitletranslatorapp.service.TranslationJobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TranslationJobController.class)
class TranslationJobControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationJobService translationJobService;

    @MockitoBean
    private TranslationJobStore jobStore;

    @Test
    void postWithoutFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/translation-jobs")
                        .param("targetLanguage", "Hungarian"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result", is("ERROR")));
    }

    @Test
    void getUnknownJobId_returns404() throws Exception {
        when(jobStore.get("unknown-id")).thenReturn(null);

        mockMvc.perform(get("/api/translation-jobs/unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result", is("ERROR")));
    }
}
