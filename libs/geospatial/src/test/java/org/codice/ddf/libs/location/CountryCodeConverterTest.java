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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

import java.util.List;
import java.util.MissingResourceException;

import org.junit.Test;

public class CountryCodeConverterTest {

    @Test
    public void testFipstoIso3Conversion() {
        List<String> iso3 = CountryCodeConverter.convertFipsToIso3("RS");
        assertThat(iso3.get(0), is("RUS"));
    }

    @Test
    public void testFipsToIso3ConversionNullCountryCode() {
        List<String> iso3 = CountryCodeConverter.convertFipsToIso3(null);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testFipsToIso3ConversionNoCountryCode() {
        List<String> iso3 = CountryCodeConverter.convertFipsToIso3("11");
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testIso3ToFipsConversion() {
        List<String> fipsCountryCodes = CountryCodeConverter.convertIso3ToFips("RUS");
        assertThat(fipsCountryCodes, containsInAnyOrder("RS"));
    }

    @Test
    public void testIso3ToFipsSpecialCaseConversion() {
        List<String> fipsCountryCodes = CountryCodeConverter.convertIso3ToFips("PSE");
        assertThat(fipsCountryCodes, containsInAnyOrder("WE", "GZ"));
    }

    @Test
    public void testIso3ToFipsConversionNoCountryCode() {
        List<String> iso3 = CountryCodeConverter.convertIso3ToFips("111");
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testIso3toFipsConversionNullCountryCode() {
        List<String> fipsCountryCodes = CountryCodeConverter.convertIso3ToFips(null);
        assertThat(fipsCountryCodes.isEmpty(), is(true));
    }

    @Test
    public void testIsoIso2ToIso3Conversion() {
        String alpha3CountryCode =
                CountryCodeConverter.convertIso2ToIso3(CountryCodeConverter.ENGLISH_LANG, "NO");
        assertThat(alpha3CountryCode, is("NOR"));
    }

    @Test(expected = MissingResourceException.class)
    public void testIso2toIso3ConversionInvalidCountryCode() {
        CountryCodeConverter.convertIso2ToIso3(CountryCodeConverter.ENGLISH_LANG,
                "invalid country code");
    }
}
