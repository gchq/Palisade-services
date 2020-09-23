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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import uk.gov.gchq.palisade.service.attributemask.exception.AuditableException;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;

/**
 * Kafka interface to the service, used by an Akka RunnableGraph.
 */
@Controller
public class KafkaController extends MarkedStreamController {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaController.class);

    /**
     * Autowired constructor for Kafka Controller, supplying the underlying service implementation
     *
     * @param attributeMaskingService an implementation of the {@link AttributeMaskingService}
     */
    public KafkaController(final AttributeMaskingService attributeMaskingService) {
        super(attributeMaskingService);
    }

    /**
     * Given an (Auditable)Exception, produce an AuditErrorMessage ProducerRecord for writing to Kafka
     *
     * @param exception an exception wrapping the cause and original request to be audited
     * @param errorTopic the topic to write to
     * @param <K> the type of the kafka error-topic key
     * @return a Kafka ProducerRecord with appropriate topic, partition, value and headers
     */
    public <K> ProducerRecord<K, AuditErrorMessage> auditError(
            final AuditableException exception,
            final Topic errorTopic) {
        LOGGER.error("An error occurred, supervising it now:", exception);
        ConsumerRecord<?, AttributeMaskingRequest> request = exception.getRequest();
        AuditErrorMessage errorMessage = AuditErrorMessage.Builder.create(request.value(), Collections.emptyMap())
                .withError(exception.getCause());

        // Prepare partition keying
        String token = new String(request.headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset());
        int partition = Math.floorMod(token.hashCode(), errorTopic.getPartitions());

        return new ProducerRecord<>(errorTopic.getName(), partition, (K) null, errorMessage, request.headers());
    }

    /**
     * Connect the MarkedStreamController to Kafka, converting between Kafka Records and Headers to POJOs
     *
     * @param requestRecord a record containing the request to the service, as well as headers
     * @param producerTopic the output topic name for constructing producer records
     * @param <K>           the type of the Kafka record key
     * @return a record containing the response from the service, as well as headers
     * @throws AuditableException if an exception occurred, binding the {@link ConsumerRecord} to the thrown {@link Exception}
     */
    public <K> ProducerRecord<K, AttributeMaskingResponse> maskAttributes(
            final ConsumerRecord<K, AttributeMaskingRequest> requestRecord,
            final Topic producerTopic) {

        try {
            // Get request from body
            AttributeMaskingRequest request = requestRecord.value();

            // Get Token and StreamMarker from headers
            String token = new String(requestRecord.headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset());
            StreamMarker streamMarker = Optional.ofNullable(requestRecord.headers().lastHeader(StreamMarker.HEADER))
                    .map(header -> StreamMarker.valueOf(new String(header.value(), Charset.defaultCharset())))
                    .orElse(null);

            // Try to store with service
            Optional<AttributeMaskingResponse> optionalResponse = this.processRequestOrStreamMarker(token, streamMarker, request);

            // Prepare partition keying
            int partition = Math.floorMod(token.hashCode(), producerTopic.getPartitions());

            // Return result
            return new ProducerRecord<>(producerTopic.getName(), partition, requestRecord.key(), optionalResponse.orElse(null), requestRecord.headers());

        } catch (RuntimeException | IOException ex) {
            // Bind request to exception object, to be caught and audited appropriately
            throw new AuditableException(requestRecord, ex);
        }
    }
}
