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
package org.codice.ddf.country.converter.api;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Service to convert country codes into various formats.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface CountryCodeConverter {

    /**
     * Converts a country code from one format into another. If the country code maps to more than
     * one country code from the desired format, all of them will be returned in a list of strings.
     *
     * @param countryCode   a country code.
     * @param sourceFormat  source format of the country code.
     * @param desiredFormat desired format of the country code to convert into.
     * @return country code(s) in a List of Strings.
     */
    List<String> convert(@Nullable String countryCode, String sourceFormat, String desiredFormat);
}
