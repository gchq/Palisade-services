/*
 * Copyright 2018-2021 Crown Copyright
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
package uk.gov.gchq.palisade.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Abstract class providing a serialisation test for a class
 */
@SuppressWarnings("java:S2187") // no tests in this class by design
public class AbstractSerialisationTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setupAll() {
        mapper = new ObjectMapper().registerModules(new Jdk8Module());
    }

    @AfterAll
    static void afterAll() {
        mapper = null;
    }

    /**
     * Tests the serialisation and deserialisation of each object provided by the
     * method source
     * <p>
     * The tests asserted by this method:
     * <ul>
     * <li><em>Equals</em> - Test equality using each instances equals method</li>
     * <li><em>Recursive Equals</em> - Tests equality by testing field by field in the instance</li>
     * </ul>
     *
     * @param type             The type class
     * @param <O>              The type of the object itself
     * @param <T>              The type of the object
     * @param expectedInstance The expected instance
     * @return The object being tested
     * @throws Exception if an error occurs
     */
    protected <O, T extends O> T assertSerialisation(final Class<T> type, final O expectedInstance) throws Exception {
        return assertSerialisation(type, expectedInstance, null);
    }

    /**
     * Tests the serialisation and deserialisation of each object provided by the
     * method source
     * <p>
     * The tests asserted by this method:
     * <ul>
     * <li><em>Equals</em> - Test equality using each instances equals method</li>
     * <li><em>Recursive Equals</em> - Tests equality by testing field by field in
     * the instance</li>
     * <li><em>JSON Attributes</em> - uses JSONAssert to test whether the serialise
     * instance matches the provided JSON string</li>
     * </ul>
     *
     * @param type             The type class
     * @param <O>              The type of the object itself
     * @param <T>              The type
     * @param expectedInstance The expected instance
     * @param expectedJson     The expected JSON of the provided instance. This JSON
     *                         string is asserted of ignored is {@code expectedJson}
     *                         is null.
     * @return The object being tested
     * @throws Exception if an error occurs
     */
    protected <O, T extends O> T assertSerialisation(final Class<T> type, final O expectedInstance, final String expectedJson)
            throws Exception {

        var objectMapper = getObjectMapper();
        var typeName = type.getSimpleName();

        // WHEN the expected instance is serialised to JSON and then deserialised back
        //      into an actual instance

        var actualJson = objectMapper.writeValueAsString(expectedInstance);
        var actualInstance = objectMapper.readValue(actualJson, type);

        // THEN

        assertThat(actualInstance)
                .as("check %s using toString()", typeName)
                .isEqualTo(expectedInstance);

        assertThat(actualInstance)
                .as("check %s using recursive toString()", typeName)
                .usingRecursiveComparison()
                .isEqualTo(expectedInstance);

        // if an expected JSON string has been provided, then we will check it against
        // the actual JSON string.
        // Note that this uses JSONAssert which will check that the actual json tree of
        // each JSON string.

        if (expectedJson != null && !expectedJson.isBlank()) {
            assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
        }

        // return the actual instance in case the sub-class requires it, e.g. logging.
        return actualInstance;

    }

    /**
     * Returns the object mapper. Override if needed.
     *
     * @return the object mapper
     */
    protected ObjectMapper getObjectMapper() {
        return mapper;
    }

}
