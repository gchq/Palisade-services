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

package uk.gov.gchq.palisade.component.user.web;

import akka.Done;
import akka.stream.javadsl.Sink;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.util.concurrent.CompletionStage;

/**
 * Config for {@link RestControllerWebMvcTest} class.
 * Rather than actually connecting to kafka, set-up an akka sink to consume messages and do nothing
 * with them. For stricter testing configuration, sink to a static Collection and inspect the contents
 * after each test.
 */
@Configuration
public class AkkaSinkTestConfiguration {
    @Primary
    @Bean
    Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> ignoringSink() {
        return Sink.foreach(ignored -> {
        });
    }
}
