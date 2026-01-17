package org.k3cs1.subtitletranslatorapp.dto;

public record CountryOptionDto(String code, String name) {
    public CountryOptionDto {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Country code is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Country name is required.");
        }
    }
}

