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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import java.util.List;

import org.codice.ddf.country.converter.api.CountryCodeFormat;
import org.junit.Test;

public class LocalCountryCodeConverterTest {

    private LocalCountryCodeConverter localCountryCodeConverter = new LocalCountryCodeConverter();

    public LocalCountryCodeConverterTest() {
        localCountryCodeConverter.setMappingFileLocations(Arrays.asList("fips_10_4.properties"));
    }

    @Test
    public void testConvertFipsToIso3() {
        List<String> iso3 = localCountryCodeConverter.convert("RS",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.ISO_3166_1_ALPHA_3);
        assertThat(iso3.get(0), is("RUS"));
    }

    @Test
    public void testConvertFipsToIso3MultipleCountryMappings() {
        List<String> iso3 = localCountryCodeConverter.convert("ZZ",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.ISO_3166_1_ALPHA_3);
        assertThat(iso3, containsInAnyOrder("ZZZ", "QQQ"));
    }

    @Test
    public void testConvertFipsToIso3InvalidFilePath() {
        localCountryCodeConverter.setMappingFileLocations(Arrays.asList("invalid file path"));
        List<String> iso3 = localCountryCodeConverter.convert("ZZ",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipsToIso3InvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convert("11",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipsToIso3NullCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convert(null,
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertIso3ToFips() {
        List<String> fips = localCountryCodeConverter.convert("RUS",
                CountryCodeFormat.ISO_3166_1_ALPHA_3,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips, containsInAnyOrder("RS"));
    }

    @Test
    public void testConvertIso3ToFipsMultipleCountryMappings() {
        List<String> fips = localCountryCodeConverter.convert("PSE",
                CountryCodeFormat.ISO_3166_1_ALPHA_3,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips, containsInAnyOrder("WE", "GZ"));
    }

    @Test
    public void testConvertIso3ToFipsInvalidCountryCode() {
        List<String> fips = localCountryCodeConverter.convert("111",
                CountryCodeFormat.ISO_3166_1_ALPHA_3,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertIso3toFipsNullCountryCode() {
        List<String> fips = localCountryCodeConverter.convert(null,
                CountryCodeFormat.ISO_3166_1_ALPHA_3,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipstoFips() {
        List<String> fips = localCountryCodeConverter.convert("US",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips, containsInAnyOrder("US"));
    }

    @Test
    public void testConvertFipstoFipsInvalidCountryCode() {
        List<String> fips = localCountryCodeConverter.convert("QQQ",
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipstoFipsNullCountryCode() {
        List<String> fips = localCountryCodeConverter.convert(null,
                CountryCodeFormat.FIPS_10_4,
                CountryCodeFormat.FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

}
