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

package uk.gov.gchq.palisade.service.data.exception;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ReadExceptionTest {

    @Test
    void testReadExceptionCapturesDataReaderRequest() {
        // Given
        AuthorisedRequestEntity entity = new AuthorisedRequestEntity(
                "test-request-token",
                new User().userId("user-id"),
                new FileResource().id("/resource/id"),
                new Context(),
                new Rules<>()
        );
        DataReaderRequest readerRequest = new DataReaderRequest()
                .user(entity.getUser())
                .resource(entity.getLeafResource())
                .context(entity.getContext())
                .rules(entity.getRules());

        try {
            // When
            throw new ReadException(readerRequest, new IOException());

        } catch (ReadException ex) {
            // Then
            assertThat(ex.getReaderRequest())
                    .isEqualTo(readerRequest);
        }
    }

}
