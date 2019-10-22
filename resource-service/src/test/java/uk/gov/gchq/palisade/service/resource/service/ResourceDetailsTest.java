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

import org.apache.commons.math3.util.Pair;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class ResourceDetailsTest {

    public static final String PATH = "path/to/nowhere/";

    @DataPoints
    public static final List<Pair<String, Pair<String, String>>> filenameDataPoints = new ArrayList<>();
    @DataPoints
    public static final List<ResourceDetails> resourceDataPoints = new ArrayList<>();

    ;

    static {
        // Valid file names
        filenameDataPoints.add(new Pair<>("employee_file0.avro", new Pair<>("employee", "avro")));
        filenameDataPoints.add(new Pair<>("emplo   yee_file0.avro", new Pair<>("emplo   yee", "avro")));
        filenameDataPoints.add(new Pair<>("employee_fi   le0.av   ro", new Pair<>("employee", "av   ro")));
        // Invalid file names
        filenameDataPoints.add(new Pair<>("file0.avro", null));
        filenameDataPoints.add(new Pair<>("employee_file0", null));
        filenameDataPoints.add(new Pair<>("_.", null));
        filenameDataPoints.add(new Pair<>("", null));
        filenameDataPoints.add(new Pair<>(".avro", null));
    }

    static {
        for (Pair<String, ?> filename : filenameDataPoints) {
            if (filename.getValue() != null) {
                resourceDataPoints.add(ResourceDetails.getResourceDetailsFromFileName(filename.getKey()));
            }
        }
    }


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Theory
    public void throwIfInvalidFileName(Pair<String, Pair<String, String>> dataPoint) {
        // Given - invalid filename
        String fileName = dataPoint.getKey();
        Pair<String, String> expected = dataPoint.getValue();
        assumeThat(expected, nullValue());

        // Then (expected)
        thrown.expect(IllegalArgumentException.class);

        // When
        ResourceDetails.getResourceDetailsFromFileName(fileName);
    }

    @Theory
    public void shouldExtractFileName(Pair<String, Pair<String, String>> dataPoint) {
        // Given - valid filename
        String fileName = dataPoint.getKey();
        Pair<String, String> expected = dataPoint.getValue();
        assumeThat(expected, notNullValue());

        String type = expected.getKey();
        String format = expected.getValue();

        //When
        ResourceDetails details = ResourceDetails.getResourceDetailsFromFileName(fileName);

        //Then
        assertThat(type, is(equalTo(details.getType())));
        assertThat(fileName, is(equalTo(details.getFileName())));
        assertThat(format, is(equalTo(details.getFormat())));
    }

    @Theory
    public void shouldPassValidNames(Pair<String, Pair<String, String>> dataPoint) {
        // Given
        String fileName = dataPoint.getKey();
        Pair<String, String> expected = dataPoint.getValue();

        assertThat(ResourceDetails.isValidResourceName(fileName), is(expected != null));
    }

    @Theory
    public void reflexiveEquals(ResourceDetails x) {
        // Then
        assertThat(x, equalTo(x));
    }

    @Theory
    public void nullNotEquals(ResourceDetails x) {
        // Then
        assertThat(x, not(equalTo(nullValue())));
    }

    @Theory
    public void symmetricEquals(ResourceDetails x, ResourceDetails y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(y, equalTo(x));
    }

    @Theory
    public void transitiveEquals(ResourceDetails x, ResourceDetails y, ResourceDetails z) {
        // Given
        assumeThat(x, equalTo(y));
        assumeThat(y, equalTo(z));
        // Then
        assertThat(x, equalTo(z));
    }

    @Theory
    public void consistentHashCode(ResourceDetails x) {
        // Then
        assertThat(x.hashCode(), equalTo(x.hashCode()));
    }

    @Theory
    public void equalHashCodeWhenEqual(ResourceDetails x, ResourceDetails y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(x.hashCode(), equalTo(y.hashCode()));
    }
}
