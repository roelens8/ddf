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

import java.util.Collections;
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
 * Service to convert county codes into various formats by performing a local lookup. In order to
 * convert country codes from FIPS 10-4 to ISO 3166-1 alpha 3 and vice versa, a properties file
 * containing the country code mappings is required and the file path of the file needs to be
 * configured via the feature's configuration. For more information on how the country code mappings
 * are formatted, refer to the Administrator's documentation.
 */
public class LocalCountryCodeConverter implements CountryCodeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCountryCodeConverter.class);

    private Multimap<String, String> fipsToIso3Map;

    private Multimap<String, String> iso3ToFipsMap;

    public List<String> convertFipsToIso3(@Nullable String fipsCountryCode) {
        if (StringUtils.isNotBlank(fipsCountryCode)) {
            List<String> convertedCountryCodes =
                    (List<String>) fipsToIso3Map.get(fipsCountryCode);
            if (!convertedCountryCodes.isEmpty()) {
                return convertedCountryCodes;
            }
        }
        LOGGER.debug("Could not find a corresponding ISO 3166-1 alpha 3 country code given the"
                + "FIPS 10-4 country code: {}", fipsCountryCode);
        return Collections.emptyList();
    }

    public List<String> convertIso3ToFips(@Nullable String iso3alphaCountryCode) {
        if (StringUtils.isNotBlank(iso3alphaCountryCode)) {
            List<String> convertedCountryCodes = (List<String>) iso3ToFipsMap.get(
                    iso3alphaCountryCode);
            if (!convertedCountryCodes.isEmpty()) {
                return convertedCountryCodes;
            }
        }
        LOGGER.debug("Could not find a corresponding FIPS 10-4 country code given the ISO 3166-1"
                + " alpha 3 country code: {}", iso3alphaCountryCode);
        return Collections.emptyList();
    }

    public void setCountryCodeMappingsFile(String countryCodeMappingFile) {
        if (StringUtils.isNotBlank(countryCodeMappingFile)) {
            fipsToIso3Map = ArrayListMultimap.create();
            iso3ToFipsMap = ArrayListMultimap.create();
            Map<String, String> countryCodesMap =
                    PropertiesLoader.toMap(PropertiesLoader.loadProperties(countryCodeMappingFile));
            for (Map.Entry<String, String> countryMappingEntry : countryCodesMap.entrySet()) {
                List<String> mappedCountries = Splitter.on(',')
                        .splitToList(countryMappingEntry.getValue());
                for (String isoCountry : mappedCountries) {
                    String newCountry = countryMappingEntry.getKey();
                    fipsToIso3Map.put(newCountry, isoCountry);
                    iso3ToFipsMap.put(isoCountry, newCountry);
                }
            }
        }
        if (fipsToIso3Map.isEmpty() || iso3ToFipsMap.isEmpty()) {
            LOGGER.warn("Country code maps are empty. Verify the 'FIPS 10-4 to ISO 3166-1 alpha 3 "
                    + "File Location' field in the feature's configuration is correctly pointing "
                    + "to the country mappings file.");
        }
    }
}
