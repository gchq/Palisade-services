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
package uk.gov.gchq.palisade.component.data.model;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUTHORISED_DATA;

class AuthorisedDataRequestTest {

    /**
     * Creates an {@code AuthorisedDataRequest} for a successful request using the class's Builder and then
     * compares it to the original object.
     */
    @Test
    void testDataRequestReaderBuilder() {
        AuthorisedDataRequest authorisedDataObjectContentObject = AuthorisedDataRequest.Builder.create()
                .withResource(AUTHORISED_DATA.getResource())
                .withUser(AUTHORISED_DATA.getUser())
                .withContext(AUTHORISED_DATA.getContext())
                .withRules(AUTHORISED_DATA.getRules());

        assertAll("ObjectComparison",
                () -> assertThat(authorisedDataObjectContentObject)
                        .as("Comparison assertion using the AuthorisedData's equals")
                        .isEqualTo(AUTHORISED_DATA),

                () -> assertThat(authorisedDataObjectContentObject)
                        .as("Comparison assertion using all of the AuthorisedData's components recursively")
                        .usingRecursiveComparison()
                        .isEqualTo(AUTHORISED_DATA)
        );
    }

}
