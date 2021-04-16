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
package uk.gov.gchq.palisade.service.palisade.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.palisade.CommonTestData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class UUIDPalisadeServiceTest extends CommonTestData {
    final PalisadeService uuidPalisadeService = Mockito.mock(UUIDPalisadeService.class);

    @Test
    void testUUIDCreation() {
        // When we hardcode the token to be returned from the service
        Mockito.doReturn(COMMON_UUID.toString()).when(uuidPalisadeService).createToken(any());

        // When the service is called to create the token
        String token = uuidPalisadeService.createToken(PALISADE_REQUEST);

        //Then the token is a valid uuid
        //UUID.fromString has its own error handling, such as throw new IllegalArgumentException so no extra validation checking is required here
        assertThat(UUID.fromString(token))
                .as("Check the class of the token")
                .isInstanceOf(UUID.class)
                .as("Check the value of the token")
                .isEqualTo(COMMON_UUID);
    }
}
