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
package org.codice.ddf.spatial.country.converter.api;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Utility to convert country codes into various formats.
 */
public interface CountryCodeConverter {

    String ENGLISH_LANG = "en";

    /**
     * Converts FIPS 10-4 into ISO 3166-1 alpha 3. If a FIPS 10-4 code maps to more than one
     * ISO 3166-1 alpha 3 code, all of them will be returned in a list of strings.
     *
     * @param fipsCountryCode a FIPS 10-4 country code
     * @return an ISO 3166-1 alpha-3 country code in a List<String> or null if there isn't a
     * valid conversion.
     */
    List<String> convertFipsToIso3(@Nullable String fipsCountryCode);

    /**
     * Converts ISO 3166-1 alpha 3 into FIPS 10-4. If an ISO 3166-1 alpha 3 code maps to more than
     * one FIPS 10-4 code, all of them will be returned in a list of strings.
     *
     * @param iso3alphaCountryCode an ISO 3166 alpha 3 country code
     * @return an ISO 3166-1 country code in a List<String> or an empty list if there isn't a valid
     * conversion.
     */
    List<String> convertIso3ToFips(@Nullable String iso3alphaCountryCode);

    /**
     * Converts ISO 3166-1 alpha 2 into ISO 3166-1 alpha 3.
     *
     * @param language          the language to return the converted country code in
     * @param alpha2CountryCode an ISO 3166-1 alpha-2 country code
     * @return an ISO 3166-1 alpha-3 country code
     */
    String convertIso2ToIso3(String language, String alpha2CountryCode);
}
