package org.k3cs1.subtitletranslatorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.k3cs1.subtitletranslatorapp.dto.CountryOptionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorldBankReferenceServiceImpl implements WorldBankReferenceService {

    private final RestClient.Builder builder;
    private final ObjectMapper objectMapper;

    @Value("${worldbank.base-url:https://api.worldbank.org}")
    private String baseUrl;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = builder
                .baseUrl(Objects.requireNonNull(baseUrl, "worldbank.base-url is required"))
                .build();
    }

    @Override
    public List<CountryOptionDto> listCountries() {
        try {
            String body = Objects.requireNonNull(restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/country")
                            .queryParam("format", "json")
                            .queryParam("per_page", "400")
                            .build())
                    .retrieve()
                    .body(String.class), "World Bank response body is null");

            JsonNode root = Objects.requireNonNull(objectMapper.readTree(body), "World Bank response JSON is null");
            if (!root.isArray() || root.size() < 2 || !root.get(1).isArray()) {
                throw new IllegalStateException("Unexpected World Bank response format.");
            }

            List<CountryOptionDto> out = new ArrayList<>();
            for (JsonNode node : root.get(1)) {
                String iso2 = node.path("iso2Code").asText("");
                String name = node.path("name").asText("");
                String regionValue = node.path("region").path("value").asText("");

                // Filter out aggregates and invalid entries
                if (iso2.isBlank() || name.isBlank()) {
                    continue;
                }
                if ("Aggregates".equalsIgnoreCase(regionValue)) {
                    continue;
                }

                out.add(new CountryOptionDto(iso2, name));
            }

            out.sort(Comparator.comparing(CountryOptionDto::name, String.CASE_INSENSITIVE_ORDER));
            return out;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch countries from World Bank.", ex);
        }
    }
}

