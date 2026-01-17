package org.k3cs1.subtitletranslatorapp.service;

import org.k3cs1.subtitletranslatorapp.dto.CountryOptionDto;

import java.util.List;

public interface WorldBankReferenceService {
    List<CountryOptionDto> listCountries();
}

