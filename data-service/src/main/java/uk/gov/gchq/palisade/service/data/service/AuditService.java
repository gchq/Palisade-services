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
package uk.gov.gchq.palisade.service.data.service;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;

import java.util.concurrent.CompletableFuture;

public class AuditService {

    private final Materializer materializer;
    private final CompletableFuture<Sink<AuditableDataReaderResponse, ?>> successFutureSink;

    AuditService(final Materializer materializer) {
        this.successFutureSink = new CompletableFuture<>();
        this.materializer = materializer;
    }

    /**
     * Audit a successful read to the audit-service
     *
     * @param auditableDataReaderResponse the constructed message detailing the resource read, the rules applied and other metadata
     * @implNote Any implementation of this should ensure it has confirmation that the message has been persisted downstream.
     * This provides assurances that the audit logs won't go missing due to processing failures.
     * This is probably implemented as blocking until the persistence-write (kafka/redis/etc.) completes and throwing a
     * {@link RuntimeException} if processing fails.
     */
    public void auditMessage(final AuditableDataReaderResponse auditableDataReaderResponse) {
        //how do it integrate the token value into header for this message?
        successFutureSink.thenApply(sink -> Source.single(auditableDataReaderResponse).runWith(sink, materializer));
    }
}
