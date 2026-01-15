package org.k3cs1.subtitletranslatorapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorsConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService translationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
