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
package uk.gov.gchq.palisade.service.data.service;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;

import java.util.concurrent.CompletableFuture;

/**
 * Service for sending messages to the Audit Service. Messages can be either success or error message type.
 */
public class AuditMessageService {

    private final Materializer materializer;
    private final CompletableFuture<Sink<TokenMessagePair, ?>> futureSink;

    /**
     * Autowires the {@link Materializer}
     *
     * @param materializer the Akka {@link Materializer} configured to be used
     */
    public AuditMessageService(final Materializer materializer) {
        this.futureSink = new CompletableFuture<>();
        this.materializer = materializer;
    }

    /**
     * Sends messages to the Audit Service using a Kafka stream.
     *
     * @param tokenMessagePair the constructed message detailing the resource read, the rules applied and other metadata
     * @implNote Any implementation of this should ensure it has confirmation that the message has been persisted downstream.
     * This provides assurances that the audit logs won't go missing due to processing failures.
     * This is probably implemented as blocking until the persistence-write (kafka/redis/etc.) completes and throwing a
     * {@link RuntimeException} if processing fails.
     */
    public void auditMessage(final TokenMessagePair tokenMessagePair) {
        futureSink.thenAccept(sink -> Source.single(tokenMessagePair).runWith(sink, materializer)).join();
    }

    /**
     * Register request sink.
     *
     * @param sink the sink
     */
    public void registerRequestSink(final Sink<TokenMessagePair, ?> sink) {
        this.futureSink.complete(sink);
    }

}
