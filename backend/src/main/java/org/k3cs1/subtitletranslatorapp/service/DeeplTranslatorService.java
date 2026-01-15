package org.k3cs1.subtitletranslatorapp.service;

import java.util.List;

public interface DeeplTranslatorService {
    List<String> translateEnToHu(List<String> texts);
}
