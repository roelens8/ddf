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
package org.codice.ddf.libs.location;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

import ddf.security.PropertiesLoader;

/**
 * Utility to convert country codes into various formats.
 */
public class CountryCodeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountryCodeConverter.class);

    private static final String FIPS_TO_ISO3A3 = "FIPS10-4toISO3a3.properties";

    public static final String ENGLISH_LANG = "en";

    private static Multimap<String, String> countryCodesMap;

    static {
        countryCodesMap =
                PropertiesLoader.toMultiMap(PropertiesLoader.loadProperties(FIPS_TO_ISO3A3));
    }

    /**
     * Converts FIPS 10-4 into ISO 3166-1 alpha 3. If a FIPS 10-4 code maps to more than one
     * ISO 3166-1 alpha 3 code, all of them will be returned in a list of strings.
     *
     * @param fipsCountryCode a FIPS 10-4 country code
     * @return an ISO 3166-1 alpha-3 country code in a List<String> or null if there isn't a
     * valid conversion.
     */
    public static List<String> convertFipsToIso3(@Nullable String fipsCountryCode) {
        return convert(fipsCountryCode, true);
    }

    /**
     * Converts ISO 3166-1 alpha 3 into FIPS 10-4. If an ISO 3166-1 alpha 3 code maps to more than
     * one FIPS 10-4 code, all of them will be returned in a list of strings.
     *
     * @param iso3alphaCountryCode an ISO 3166 alpha 3 country code
     * @return an ISO 3166-1 country code in a List<String> or an empty list if there isn't a valid
     * conversion.
     */
    public static List<String> convertIso3ToFips(@Nullable String iso3alphaCountryCode) {
        return convert(iso3alphaCountryCode, false);
    }

    /**
     * Converts ISO 3166-1 alpha 2 into ISO 3166-1 alpha 3.
     *
     * @param language          the language to return the converted country code in
     * @param alpha2CountryCode an ISO 3166-1 alpha-2 country code
     * @return an ISO 3166-1 alpha-3 country code
     */
    public static String convertIso2ToIso3(String language, String alpha2CountryCode) {
        Locale locale = new Locale(language, alpha2CountryCode);
        return locale.getISO3Country();
    }

    private static List<String> convert(String countryCode, boolean convertFipsToIso) {
        if (StringUtils.isBlank(countryCode)) {
            return Collections.emptyList();
        }
        List<String> convertedCountryCodes;
        if (convertFipsToIso) {
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
}
