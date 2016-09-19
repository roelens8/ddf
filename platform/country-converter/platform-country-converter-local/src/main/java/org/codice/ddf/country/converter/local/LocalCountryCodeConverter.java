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
package org.codice.ddf.country.converter.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import org.codice.ddf.country.converter.api.CountryCodeConverter;
import org.codice.ddf.country.converter.api.CountryCodeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ddf.security.PropertiesLoader;

/**
 * Service to convert county codes into various formats by performing a local lookup. In order to
 * convert country codes from one format into another, a properties file containing country code
 * mappings is required and the file path of the file needs to be configured via the feature's
 * configuration. The name of the properties file is also required to equal the name of the source's
 * country code format enum.
 * <p>
 * Example:
 * When converting from FIPS 10-4 to ISO 3166-1 alpha 3, the name of the mapping file must be
 * "fips_10_4.properties" and the country code format enum has to be named "FIPS_10_4".
 * <p>
 * For more information on how the country code mappings are formatted,
 * refer to the Administrator's documentation.
 */
public class LocalCountryCodeConverter implements CountryCodeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCountryCodeConverter.class);

    Map<String, Multimap<String, String>> convertToIso3Maps;

    Map<String, Multimap<String, String>> convertFromIso3Maps;

    public List<String> convert(@Nullable String countryCode, CountryCodeFormat sourceFormat,
            CountryCodeFormat desiredFormat) {
        List<String> convertedCountryCodes = new ArrayList<>();
        if (StringUtils.isNotBlank(countryCode) && !convertToIso3Maps.isEmpty()
                && !convertFromIso3Maps.isEmpty()) {
            if (validateConversionIsToIso3(sourceFormat.name(), desiredFormat.name())) {
                convertedCountryCodes = (List<String>) convertToIso3Maps.get(sourceFormat.name())
                        .get(countryCode);
            } else if (validateConversionIsFromIso3(sourceFormat.name(), desiredFormat.name())) {
                convertedCountryCodes = (List<String>) convertFromIso3Maps.get(desiredFormat.name())
                        .get(countryCode);
            } else if (validateConversionIsNonIso3(sourceFormat.name(), desiredFormat.name())) {
                List<String> iso3CountryCodes =
                        (List<String>) convertToIso3Maps.get(sourceFormat.name())
                                .get(countryCode);
                for (String iso3countryCode : iso3CountryCodes) {
                    convertedCountryCodes.addAll(convertFromIso3Maps.get(desiredFormat.name())
                            .get(iso3countryCode));
                }
            }
            if (convertedCountryCodes.isEmpty()) {
                LOGGER.debug("Could not find a corresponding {} country code given the {} country "
                        + "code: {}", sourceFormat.name(), desiredFormat.name(), countryCode);
            }
        }
        return convertedCountryCodes;
    }

    public void setMappingFileLocations(List<String> mappingFileLocations) {
        convertToIso3Maps = new HashMap<>();
        convertFromIso3Maps = new HashMap<>();
        for (String mappingFileLocation : mappingFileLocations) {
            for (CountryCodeFormat countryCodeFormat : CountryCodeFormat.values()) {
                if (StringUtils.isNotBlank(mappingFileLocation) && mappingFileLocation.toLowerCase()
                        .endsWith(countryCodeFormat.name()
                                .toLowerCase() + ".properties") && !countryCodeFormat.name()
                        .equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name())) {
                    Multimap<String, String> convertToMap = ArrayListMultimap.create();
                    Multimap<String, String> convertFromMap = ArrayListMultimap.create();
                    Map<String, String> countryCodesMap =
                            PropertiesLoader.toMap(PropertiesLoader.loadProperties(
                                    mappingFileLocation));
                    for (Map.Entry<String, String> countryMappingEntry : countryCodesMap.entrySet()) {
                        List<String> mappedCountries = Splitter.on(',')
                                .splitToList(countryMappingEntry.getValue());
                        for (String mappedCountry : mappedCountries) {
                            String originalCountry = countryMappingEntry.getKey();
                            convertToMap.put(originalCountry, mappedCountry);
                            convertFromMap.put(mappedCountry, originalCountry);
                        }
                    }
                    if (convertToMap.isEmpty() || convertFromMap.isEmpty()) {
                        LOGGER.warn("Country code map for the country code format {} is empty. "
                                        + "Verify the \"Country Mappings File Locations\" field in "
                                        + "the feature's configuration has a value that is correctly "
                                        + "pointing to the country mapping file for that format.",
                                countryCodeFormat.name());
                    } else {
                        convertToIso3Maps.put(countryCodeFormat.name(), convertToMap);
                        convertFromIso3Maps.put(countryCodeFormat.name(), convertFromMap);
                    }
                    break;
                }
            }
        }
    }

    private boolean validateConversionIsToIso3(String sourceFormat, String desiredFormat) {
        if (!sourceFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                desiredFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                convertToIso3Maps.containsKey(sourceFormat) && !convertToIso3Maps.get(sourceFormat)
                .isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean validateConversionIsFromIso3(String sourceFormat, String desiredFormat) {
        if (!desiredFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                sourceFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                convertFromIso3Maps.containsKey(desiredFormat) && !convertFromIso3Maps.get(
                desiredFormat)
                .isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean validateConversionIsNonIso3(String sourceFormat, String desiredFormat) {
        if (!sourceFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                !desiredFormat.equals(CountryCodeFormat.ISO_3166_1_ALPHA_3.name()) &&
                convertToIso3Maps.containsKey(sourceFormat) &&
                !convertToIso3Maps.get(sourceFormat)
                        .isEmpty() &&
                convertFromIso3Maps.containsKey(desiredFormat) &&
                !convertFromIso3Maps.get(desiredFormat)
                        .isEmpty()) {
            return true;
        }
        return false;
    }
}
