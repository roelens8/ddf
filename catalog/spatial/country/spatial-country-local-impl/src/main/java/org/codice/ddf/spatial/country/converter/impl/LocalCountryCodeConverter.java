/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.country.converter.impl;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.country.converter.api.CountryCodeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

import ddf.security.PropertiesLoader;

public class LocalCountryCodeConverter implements CountryCodeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCountryCodeConverter.class);

    private Multimap<String, String> countryCodesMap;

    public List<String> convertFipsToIso3(@Nullable String fipsCountryCode) {
        return convert(fipsCountryCode, true);
    }

    public List<String> convertIso3ToFips(@Nullable String iso3alphaCountryCode) {
        return convert(iso3alphaCountryCode, false);
    }

    public String convertIso2ToIso3(String language, String alpha2CountryCode) {
        Locale locale = new Locale(language, alpha2CountryCode);
        return locale.getISO3Country();
    }

    private List<String> convert(String countryCode, boolean fipsToIso) {
        if (StringUtils.isBlank(countryCode)) {
            return Collections.emptyList();
        }
        if (countryCodesMap == null) {
            LOGGER.debug(
                    "Could not convert country code as the FIPS 10-4 to ISO 3166-1 alpha 3 mapping "
                            + "file is not set.");
            return Collections.emptyList();
        }
        List<String> convertedCountryCodes;
        if (fipsToIso) {
            convertedCountryCodes = (List<String>) countryCodesMap.get(countryCode);
            if (convertedCountryCodes.isEmpty()) {
                LOGGER.debug(
                        "Could not find a corresponding ISO 3166-1 alpha 3 country code given the"
                                + "FIPS 10-4 country code: {}",
                        countryCode);
                return Collections.emptyList();
            }
        } else {
            convertedCountryCodes = countryCodesMap.entries()
                    .stream()
                    .filter(entry -> Objects.equals(entry.getValue(), countryCode))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (convertedCountryCodes.isEmpty()) {
                LOGGER.debug(
                        "Could not find a corresponding FIPS 10-4 country code given the ISO 3166-1"
                                + " alpha 3 country code: {}",
                        countryCode);
                return Collections.emptyList();
            }
        }
        return convertedCountryCodes;
    }

    public void setFipsToIso3MappingFile(String fipsToIso3MappingFile) {
        countryCodesMap = PropertiesLoader.toMultiMap(PropertiesLoader.loadProperties(
                fipsToIso3MappingFile));
    }
}
