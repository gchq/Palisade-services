/*
 * Copyright 2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.palisade.service.resource.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ResourceDetailsTest {

    public static final String PATH = "path/to/nowhere/";
    public static final String VALID1 = "employee_file0.avro";
    public static final String VALID2 = "emplo   yee_file0.avro";
    public static final String VALID3 = "employee_fi   le0.av   ro";

    public static final String INVALID1 = "file0.avro";
    public static final String INVALID2 = "employee_file0";
    public static final String INVALID3 = "_.";
    public static final String INVALID4 = "";
    public static final String INVALID5 = ".avro";

    @Test
    public void shouldExtractFileName() {
        //Given
        String fileName = PATH + VALID1;

        //When
        ResourceDetails details = ResourceDetails.getResourceDetailsFromFileName(fileName);

        //Then
        assertThat("employee", is(equalTo(details.getType())));
        assertThat(fileName, is(equalTo(details.getFileName())));
        assertThat("avro", is(equalTo(details.getFormat())));
    }

    @Test
    public void shouldCreateValidDetails() {
        //Given valid names above
        //When tested
        ResourceDetails valid1ob = ResourceDetails.getResourceDetailsFromFileName(VALID1);
        ResourceDetails valid2ob = ResourceDetails.getResourceDetailsFromFileName(VALID2);
        ResourceDetails valid3ob = ResourceDetails.getResourceDetailsFromFileName(VALID3);

        //Then - check components match
        assertThat("employee", is(equalTo(valid1ob.getType())));
        assertThat(VALID1, is(equalTo(valid1ob.getFileName())));
        assertThat("avro", is(equalTo(valid1ob.getFormat())));

        assertThat("emplo   yee", is(equalTo(valid2ob.getType())));
        assertThat(VALID2, is(equalTo(valid2ob.getFileName())));
        assertThat("avro", is(equalTo(valid2ob.getFormat())));

        assertThat("employee", is(equalTo(valid3ob.getType())));
        assertThat(VALID3, is(equalTo(valid3ob.getFileName())));
        assertThat("av   ro", is(equalTo(valid3ob.getFormat())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwOnInvalidName1() {
        ResourceDetails.getResourceDetailsFromFileName(INVALID1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwOnInvalidName2() {
        ResourceDetails.getResourceDetailsFromFileName(INVALID2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwOnInvalidName3() {
        ResourceDetails.getResourceDetailsFromFileName(INVALID3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwOnInvalidName4() {
        ResourceDetails.getResourceDetailsFromFileName(INVALID4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwOnInvalidName5() {
        ResourceDetails.getResourceDetailsFromFileName(INVALID5);
    }

    @Test
    public void shouldPassValidNames() {
        //All of these are valid so should return true
        assertTrue(ResourceDetails.isValidResourceName(VALID1));
        assertTrue(ResourceDetails.isValidResourceName(VALID2));
        assertTrue(ResourceDetails.isValidResourceName(VALID3));
    }

    @Test
    public void shouldFailInvalidNames() {
        //all of these should fail
        assertFalse(ResourceDetails.isValidResourceName(INVALID1));
        assertFalse(ResourceDetails.isValidResourceName(INVALID2));
        assertFalse(ResourceDetails.isValidResourceName(INVALID3));
        assertFalse(ResourceDetails.isValidResourceName(INVALID4));
        assertFalse(ResourceDetails.isValidResourceName(INVALID5));
    }
}
