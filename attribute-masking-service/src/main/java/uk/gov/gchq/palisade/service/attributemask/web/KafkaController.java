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

package uk.gov.gchq.palisade.service.attributemask.web;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Controller;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Kafka interface to the service, used by an Akka RunnableGraph.
 */
@Controller
public class KafkaController extends MarkedStreamController {
    private final ErrorHandlingService errorHandler;

    public KafkaController(
            final AttributeMaskingService attributeMaskingService,
            final ErrorHandlingService errorHandler) {
        super(attributeMaskingService);
        this.errorHandler = errorHandler;
    }

    public <K> ProducerRecord<K, AttributeMaskingResponse> maskAttributes(
            final ConsumerRecord<K, AttributeMaskingRequest> requestRecord,
            final Topic producerTopic) {
        String token = new String(requestRecord.headers().lastHeader(Token.HEADER).value());
        StreamMarker streamMarker = Optional.ofNullable(requestRecord.headers().lastHeader(StreamMarker.HEADER))
                .map(header -> StreamMarker.valueOf(new String(header.value())))
                .orElse(null);
        AttributeMaskingRequest request = requestRecord.value();

        Optional<AttributeMaskingResponse> optionalResponse = Optional.empty();
        try {
            // Try to store with service
            optionalResponse = this.processRequestOrStreamMarker(token, streamMarker, request);
        } catch (IOException ex) {
            // Audit error appropriately (Kafka)
            // If we failed with a StreamMarker message, there's no real way to audit it, so throw NoSuchElementException
            errorHandler.reportError(token, requireNonNull(request), ex);
        }

        // Prepare partition keying
        String partitionKey = "";
        int partition = Math.floorMod(partitionKey.hashCode(), producerTopic.getPartitions());

        // Return result
        return new ProducerRecord<>(producerTopic.getName(), partition, requestRecord.key(), optionalResponse.orElse(null), requestRecord.headers());
    }
}
