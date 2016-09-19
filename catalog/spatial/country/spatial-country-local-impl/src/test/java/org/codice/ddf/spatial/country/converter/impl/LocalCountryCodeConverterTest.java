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

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.MissingResourceException;

import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

public class LocalCountryCodeConverterTest {

    private LocalCountryCodeConverter localCountryCodeConverter = new LocalCountryCodeConverter();

    @Before
    public void setup() {
        localCountryCodeConverter.setFipsToIso3MappingFile("fipsToIso.properties");
    }

    @Test
    public void testFipstoIso3Conversion() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("RS");
        assertThat(iso3.get(0), Is.is("RUS"));
    }

    @Test
    public void testFipsToIso3ConversionNullCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3(null);
        assertThat(iso3.isEmpty(), Is.is(true));
    }

    @Test
    public void testFipsToIso3ConversionInvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("11");
        assertThat(iso3.isEmpty(), Is.is(true));
    }

    @Test
    public void testIso3ToFipsConversion() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips("RUS");
        assertThat(fipsCountryCodes, Matchers.containsInAnyOrder("RS"));
    }

    @Test
    public void testIso3ToFipsSpecialCaseConversion() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips("PSE");
        assertThat(fipsCountryCodes, Matchers.containsInAnyOrder("WE", "GZ"));
    }

    @Test
    public void testIso3ToFipsConversionInvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertIso3ToFips("111");
        assertThat(iso3.isEmpty(), Is.is(true));
    }

    @Test
    public void testIso3toFipsConversionNullCountryCode() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips(null);
        assertThat(fipsCountryCodes.isEmpty(), Is.is(true));
    }

    @Test
    public void testIsoIso2ToIso3Conversion() {
        String alpha3CountryCode = localCountryCodeConverter.convertIso2ToIso3(
                LocalCountryCodeConverter.ENGLISH_LANG,
                "NO");
        assertThat(alpha3CountryCode, Is.is("NOR"));
    }

    @Test(expected = MissingResourceException.class)
    public void testIso2toIso3ConversionInvalidCountryCode() {
        localCountryCodeConverter.convertIso2ToIso3(LocalCountryCodeConverter.ENGLISH_LANG,
                "invalid country code");
    }
}
