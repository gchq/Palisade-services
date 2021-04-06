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

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PalisadeServiceTest extends CommonTestData {

    final Materializer materialiser = Materializer.createMaterializer(ActorSystem.create());
    final PalisadeService service = new MockedPalisadeService(materialiser);
    LinkedList<TokenRequestPair> sinkCollection;

    @BeforeEach
    void setUp() {
        sinkCollection = new LinkedList<>();
        Sink<TokenRequestPair, CompletionStage<Done>> sink = Sink.foreach((TokenRequestPair x) -> sinkCollection.addLast(x));
        service.registerRequestSink(sink);
    }

    @Test
    void testRegisterDataRequest() {
        // When we make a request to the service
        CompletableFuture<String> token = service.registerDataRequest(PALISADE_REQUEST);

        // Then the response is valid and contains all the information we require
        assertAll(
                () -> assertThat(token.join())
                        .as("Check the value of the returned Token")
                        .isEqualTo(COMMON_UUID.toString()),

                () -> assertThat(sinkCollection)
                        .as("Check that the sinkCollection has only 1 TokenRequestPair, containing the token and an AuditablePalisadeSystemResponse")
                        .usingRecursiveComparison()
                        .asList()
                        .containsOnly(new TokenRequestPair(token.join(), AUDITABLE_PALISADE_REQUEST))
        );
    }

    @Test
    void testErrorMessage() {
        // When we make a request to the client
        CompletableFuture<String> token = service.registerDataRequest(PALISADE_REQUEST);
        // And then force an error to be returned, signifying a rejected request
        service.errorMessage(PALISADE_REQUEST, token.join(), Map.of("messages", "10"), ERROR);

        // Then the response is valid and contains all the information we require
        assertAll(
                () -> assertThat(token.join())
                        .as("Check the value of the returned Token is the one we expect")
                        .isEqualTo(COMMON_UUID.toString()),

                () -> assertThat(sinkCollection.getLast().first())
                        .as("Check the Token value from the error-topic")
                        .isEqualTo(token.join()),

                () -> assertThat(sinkCollection.getLast().second().getAuditErrorMessage())
                        .as("Recursively check the value of the AuditErrorMessage is what we expect and has not been modified")
                        .usingRecursiveComparison()
                        .ignoringFields("timestamp")
                        .isEqualTo(AUDITABLE_PALISADE_ERROR.getAuditErrorMessage())
        );
    }

    /**
     * MockPalisadeService used to test that the createToken method returns the correct token
     */
    static class MockedPalisadeService extends PalisadeService {

        /**
         * Instantiates a new mocked Palisade Service for testing.
         *
         * @param materialiser the materialiser
         */
        MockedPalisadeService(final Materializer materialiser) {
            super(materialiser);
        }

        /**
         * To ensure our test passes, instead of using a unique, random UUID, we return a specific token to ensure it is not modified in the service.
         *
         * @param palisadeClientRequest the request from the client
         * @return a specific token, used only for testing
         */
        @Override
        public String createToken(final PalisadeClientRequest palisadeClientRequest) {
            return COMMON_UUID.toString();
        }
    }
}
