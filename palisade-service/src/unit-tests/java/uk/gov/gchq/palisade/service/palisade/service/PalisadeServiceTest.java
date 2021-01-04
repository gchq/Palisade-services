/*
 * Copyright 2020 Crown Copyright
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
import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PalisadeServiceTest extends CommonTestData {

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = Materializer.createMaterializer(system);
    final PalisadeService service = new MockedPalisadeService(materializer);
    final Map<String, Object> attributes = new HashMap<>();
    LinkedList<TokenRequestPair> sinkCollection;

    @BeforeEach
    void setUp() {
        sinkCollection = new LinkedList<>();
        Sink<TokenRequestPair, CompletionStage<Done>> sink = Sink.foreach((TokenRequestPair x) -> sinkCollection.addLast(x));
        service.registerRequestSink(sink);
        attributes.put("messages", "10");
    }

    @Test
    void testRegisterDataRequest() {
        CompletableFuture<String> token = service.registerDataRequest(PALISADE_REQUEST);
        assertThat(token.join()).isEqualTo(COMMON_UUID.toString());
        assertThat(sinkCollection).usingRecursiveComparison().isEqualTo(List.of(new TokenRequestPair(token.join(), AUDITABLE_PALISADE_REQUEST)));
    }

    @Test
    void testErrorMessage() {
        CompletableFuture<String> token = service.registerDataRequest(PALISADE_REQUEST);
        service.errorMessage(PALISADE_REQUEST, token.join(), attributes, ERROR);
        assertThat(token.join()).isEqualTo(COMMON_UUID.toString());
        assertAll(
                () -> assertThat(sinkCollection.getLast().first()).isEqualTo(token.join()),
                () -> assertThat(sinkCollection.getLast().second().getAuditErrorMessage()).usingRecursiveComparison().ignoringFields("timestamp")
                        .isEqualTo(List.of(new TokenRequestPair(token.join(), AUDITABLE_PALISADE_ERROR)).get(0).second().getAuditErrorMessage())
        );
    }

    /**
     * MockPalisadeService used to test that the createToken method returns the correct token
     */
    static class MockedPalisadeService extends PalisadeService {

        /**
         * Instantiates a new Palisade service.
         *
         * @param materializer the materializer
         */
        MockedPalisadeService(final Materializer materializer) {
            super(materializer);
        }

        @Override
        public String createToken(final PalisadeRequest palisadeRequest) {
            return COMMON_UUID.toString();
        }

    }
}
