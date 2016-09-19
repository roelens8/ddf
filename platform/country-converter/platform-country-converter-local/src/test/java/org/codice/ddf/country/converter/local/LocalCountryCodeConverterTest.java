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

import org.codice.ddf.country.converter.api.CountryCodeConverter;
import org.junit.Test;

public class LocalCountryCodeConverterTest {

    private static final String ISO_3166_1_ALPHA_3 = "ISO_3166_1_ALPHA3";

    private static final String FIPS_10_4 = "FIPS_10_4";

    private static final String TEST_FORMAT = "TEST_FORMAT";

    private LocalCountryCodeConverter localCountryCodeConverter = new LocalCountryCodeConverter();

    public LocalCountryCodeConverterTest() {
        localCountryCodeConverter.setMappingFileLocations(Arrays.asList("fips_10_4.properties",
                "test_format.properties"));
    }

    @Test
    public void testConvertFipsToIso3() {
        List<String> iso3 = localCountryCodeConverter.convert("RS", FIPS_10_4, ISO_3166_1_ALPHA_3);
        assertThat(iso3, containsInAnyOrder("RUS"));
    }

    @Test
    public void testConvertFipsToIso3MultipleCountryMappings() {
        List<String> iso3 = localCountryCodeConverter.convert("ZZ", FIPS_10_4, ISO_3166_1_ALPHA_3);
        assertThat(iso3, containsInAnyOrder("ZZZ", "QQQ"));
    }

    @Test
    public void testConvertFipsToIso3InvalidFilePath() {
        localCountryCodeConverter.setMappingFileLocations(Arrays.asList("invalid file path"));
        List<String> iso3 = localCountryCodeConverter.convert("ZZ", FIPS_10_4, ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipsToIso3InvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convert("11", FIPS_10_4, ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipsToIso3NullCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convert(null, FIPS_10_4, ISO_3166_1_ALPHA_3);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testConvertIso3ToFips() {
        List<String> fips = localCountryCodeConverter.convert("RUS", ISO_3166_1_ALPHA_3, FIPS_10_4);
        assertThat(fips, containsInAnyOrder("RS"));
    }

    @Test
    public void testConvertIso3ToFipsMultipleCountryMappings() {
        List<String> fips = localCountryCodeConverter.convert("PSE", ISO_3166_1_ALPHA_3, FIPS_10_4);
        assertThat(fips, containsInAnyOrder("WE", "GZ"));
    }

    @Test
    public void testConvertIso3ToFipsInvalidCountryCode() {
        List<String> fips = localCountryCodeConverter.convert("111", ISO_3166_1_ALPHA_3, FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertIso3toFipsNullCountryCode() {
        List<String> fips = localCountryCodeConverter.convert(null, ISO_3166_1_ALPHA_3, FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertTestFormatToIso3() {
        List<String> fips = localCountryCodeConverter.convert("AA",
                TEST_FORMAT,
                ISO_3166_1_ALPHA_3);
        assertThat(fips, containsInAnyOrder("USA"));
    }

    @Test
    public void testConvertTestFormatToIso3MultipleCountryMappings() {
        List<String> fips = localCountryCodeConverter.convert("BB",
                TEST_FORMAT,
                ISO_3166_1_ALPHA_3);
        assertThat(fips, containsInAnyOrder("ZZZ", "QQQ"));
    }

    @Test
    public void testConvertIsoToTestFormat() {
        List<String> fips = localCountryCodeConverter.convert("USA",
                ISO_3166_1_ALPHA_3,
                TEST_FORMAT);
        assertThat(fips, containsInAnyOrder("AA"));
    }

    @Test
    public void testConvertFipstoTestFormat() {
        List<String> fips = localCountryCodeConverter.convert("US", FIPS_10_4, TEST_FORMAT);
        assertThat(fips, containsInAnyOrder("AA"));
    }

    @Test
    public void testConvertFipstoTestFormatMultipleCountryMappings() {
        List<String> fips = localCountryCodeConverter.convert("RS", FIPS_10_4, TEST_FORMAT);
        assertThat(fips, containsInAnyOrder("RR", "LL"));
    }

    @Test
    public void testConvertTestFormatToFips() {
        List<String> fips = localCountryCodeConverter.convert("BB", TEST_FORMAT, FIPS_10_4);
        assertThat(fips, containsInAnyOrder("ZZ"));
    }

    @Test
    public void testConvertTestFormtToFipsInvalidCountryCode() {
        List<String> fips = localCountryCodeConverter.convert("QQQ", TEST_FORMAT, FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertTestFormtToFipsNullCountryCode() {
        List<String> fips = localCountryCodeConverter.convert(null, TEST_FORMAT, FIPS_10_4);
        assertThat(fips.isEmpty(), is(true));
    }

    @Test
    public void testConvertFipstoFips() {
        List<String> fips = localCountryCodeConverter.convert("US", FIPS_10_4, FIPS_10_4);
        assertThat(fips, containsInAnyOrder("US"));
    }

    @Test
    public void testConvertIsotoIso() {
        List<String> fips = localCountryCodeConverter.convert("USA",
                ISO_3166_1_ALPHA_3,
                ISO_3166_1_ALPHA_3);
        assertThat(fips.isEmpty(), is(true));
    }

}
