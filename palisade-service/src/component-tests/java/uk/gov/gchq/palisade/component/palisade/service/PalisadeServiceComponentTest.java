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

package uk.gov.gchq.palisade.component.palisade.service;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = {PalisadeServiceComponentTest.class, ApplicationConfiguration.class})
class PalisadeServiceComponentTest extends CommonTestData {

    @Autowired
    PalisadeService palisadeService;

    LinkedList<TokenRequestPair> sinkCollection;

    @Bean
    Materializer materializer() {
        return Materializer.createMaterializer(ActorSystem.create("test-actor-system"));
    }

    @BeforeEach
    void setUp() {
        sinkCollection = new LinkedList<>();
        Sink<TokenRequestPair, CompletionStage<Done>> sink = Sink.foreach((TokenRequestPair x) -> sinkCollection.addLast(x));
        palisadeService.registerRequestSink(sink);
    }

    @Test
    void testContextLoads() {
        assertThat(palisadeService).isNotNull();
    }

    @Test
    void testRegisterRequestReturnsAValidTokenRequestPair() {
        CompletableFuture<String> token = palisadeService.registerDataRequest(PALISADE_REQUEST);
        //UUID.fromString contains its own uuid validation and will throw an error if an incorrect UUID is returned
        UUID uuid = UUID.fromString(token.join());
        assertAll("testRegisterRequest",
                () -> assertThat(uuid).isNotNull()
                        .isInstanceOf(UUID.class),
                () -> assertThat(sinkCollection)
                        .hasSize(1)
                        .usingRecursiveComparison()
                        .isEqualTo(List.of(new TokenRequestPair(uuid.toString(), AUDITABLE_WITH_REQUEST)))
        );
    }
}
