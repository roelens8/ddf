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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import org.codice.ddf.country.converter.api.CountryCodeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ddf.security.PropertiesLoader;

/**
 * Service to convert county codes into various formats by performing a local lookup.  In order to
 * convert country codes from format X into format Y and vice versa, a properties file containing
 * country code mappings is required. Since ISO 3166-1 alpha 3 is the normalized format, all
 * country code mappings file should be formatted from format X into ISO 3166-1 alpha 3, and not
 * from format X directly into format Y. In order to convert from format X into format Y, two
 * properties files are required, one for format X and one for format Y. The file path of the file
 * also needs to be configured via the feature's configuration. The name of the properties file
 * (excluding the .extension) will be used as the value of the "sourceFormat" or "desiredFormat"
 * method parameters for the convert method. To specify the normalized format, use the predefined
 * string "ISO_3166_1_ALPHA_3".
 * <p>
 * Example:
 * When converting from FIPS 10-4 to ISO 3166-1 alpha 3, if the name of the country code mappings
 * file is: "fips_10_4.properties", the string "FIPS_10_4" (case-insensitive) is used as the value
 * of the source country code format and "ISO_3166_1_ALPHA3" as the value of the desired country
 * code format.
 * <p>
 * For more information on how the country code mappings are formatted, refer to the
 * Administrator's documentation.
 */
public class LocalCountryCodeConverter implements CountryCodeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCountryCodeConverter.class);

    /**
     * Variable representing the normalized country code format.
     */
    private static final String ISO_3166_1_ALPHA_3 = "ISO_3166_1_ALPHA3";

    private Map<String, Multimap<String, String>> convertToIso3Maps;

    private Map<String, Multimap<String, String>> convertFromIso3Maps;

    /**
     * The string value of the country code formats used for "sourceFormat" and "desiredFormat"
     * must be equal to the name of their corresponding country code mappings file, excluding the
     * file extension. EX. file name: "fips_10_4.properties", format used for conversion:
     * "fips_10_4".
     */
    public List<String> convert(@Nullable String countryCode, String sourceFormat,
            String desiredFormat) {
        List<String> convertedCountryCodes = new ArrayList<>();
        if (StringUtils.isNotBlank(countryCode) && !convertToIso3Maps.isEmpty()
                && !convertFromIso3Maps.isEmpty()) {
            sourceFormat = sourceFormat.toUpperCase();
            desiredFormat = desiredFormat.toUpperCase();
            if (validateConversionIsToIso3(sourceFormat, desiredFormat)) {
                convertedCountryCodes = (List<String>) convertToIso3Maps.get(sourceFormat)
                        .get(countryCode);
            } else if (validateConversionIsFromIso3(sourceFormat, desiredFormat)) {
                convertedCountryCodes = (List<String>) convertFromIso3Maps.get(desiredFormat)
                        .get(countryCode);
            } else if (validateConversionIsNonIso3(sourceFormat, desiredFormat)) {
                List<String> iso3CountryCodes = (List<String>) convertToIso3Maps.get(sourceFormat)
                        .get(countryCode);
                LinkedHashSet<String> countryCodeSet = new LinkedHashSet<>();
                for (String iso3countryCode : iso3CountryCodes) {
                    countryCodeSet.addAll(convertFromIso3Maps.get(desiredFormat)
                            .get(iso3countryCode));
                }
                convertedCountryCodes = new ArrayList<>(countryCodeSet);
            }
            if (convertedCountryCodes.isEmpty()) {
                LOGGER.debug("Could not find a corresponding {} country code given the {} country "
                        + "code: {}", desiredFormat, sourceFormat, countryCode);
            }
        }
        return convertedCountryCodes;
    }

    public void setMappingFileLocations(List<String> mappingFileLocations) {
        convertToIso3Maps = new HashMap<>();
        convertFromIso3Maps = new HashMap<>();
        for (String mappingFileLocation : mappingFileLocations) {
            if (StringUtils.isNotBlank(mappingFileLocation)) {
                String countryCodeFormat = convertFileNameToCountryFormat(mappingFileLocation);
                Multimap<String, String> convertToMap = ArrayListMultimap.create();
                Multimap<String, String> convertFromMap = ArrayListMultimap.create();
                Map<String, String> countryCodesMap =
                        PropertiesLoader.toMap(PropertiesLoader.loadProperties(mappingFileLocation));
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
                                    + "pointing to the country mappings file for that format.",
                            countryCodeFormat);
                } else {
                    convertToIso3Maps.put(countryCodeFormat.toUpperCase(), convertToMap);
                    convertFromIso3Maps.put(countryCodeFormat.toUpperCase(), convertFromMap);
                }
            }
        }
    }

    private boolean validateConversionIsToIso3(String sourceFormat, String desiredFormat) {
        if (!sourceFormat.equals(ISO_3166_1_ALPHA_3) &&
                desiredFormat.equals(ISO_3166_1_ALPHA_3) &&
                convertToIso3Maps.containsKey(sourceFormat) && !convertToIso3Maps.get(sourceFormat)
                .isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean validateConversionIsFromIso3(String sourceFormat, String desiredFormat) {
        if (!desiredFormat.equals(ISO_3166_1_ALPHA_3) &&
                sourceFormat.equals(ISO_3166_1_ALPHA_3) &&
                convertFromIso3Maps.containsKey(desiredFormat) && !convertFromIso3Maps.get(
                desiredFormat)
                .isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean validateConversionIsNonIso3(String sourceFormat, String desiredFormat) {
        if (!sourceFormat.equals(ISO_3166_1_ALPHA_3) &&
                !desiredFormat.equals(ISO_3166_1_ALPHA_3) &&
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

    private String convertFileNameToCountryFormat(String fileName) {
        return Splitter.on(".")
                .splitToList(new File(fileName).getName())
                .get(0);
    }
}
