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

package uk.gov.gchq.palisade.service.filteredresource.web.router;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.RequestContext;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.model.StatusCode;

import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.KafkaProducerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KafkaRestWriterRouter implements RouteSupplier {
    private final KafkaProducerService kafkaProducerService;

    public KafkaRestWriterRouter(final KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public Route get() {
        return Directives.pathPrefix("api", () ->
                Directives.concat(
                        Directives.pathPrefix("resource", this::resource),
                        Directives.pathPrefix("offset", this::offset),
                        Directives.pathPrefix("error", this::error)
                ));
    }

    private static Map<String, String> getHeadersMap(final RequestContext requestContext) {
        return StreamSupport.stream(requestContext.getRequest().getHeaders().spliterator(), false)
                .collect(Collectors.toMap(HttpHeader::name, HttpHeader::value));
    }

    private static <T> Route apply(final Class<T> domainClass, final BiFunction<Map<String, String>, List<T>, CompletableFuture<Void>> kafkaProduceMethod) {
        Class<List<T>> domainCollectionClass = (Class<List<T>>) List.<T>of().getClass();
        return Directives.concat(
                Directives.pathEndOrSingleSlash(() -> Directives.post(() ->
                        Directives.extract(KafkaRestWriterRouter::getHeadersMap, headers ->
                                Directives.entity(Jackson.unmarshaller(domainClass), (T request) -> {
                                    kafkaProduceMethod.apply(headers, Collections.singletonList(request)).join();
                                    return Directives.complete(StatusCode.int2StatusCode(202));
                                })))),
                Directives.pathPrefix("multi", () -> Directives.post(() ->
                        Directives.extract(KafkaRestWriterRouter::getHeadersMap, headers ->
                                Directives.entity(Jackson.unmarshaller(domainCollectionClass), (List<T> requests) -> {
                                    kafkaProduceMethod.apply(headers, requests).join();
                                    return Directives.complete(StatusCode.int2StatusCode(202));
                                }))))
        );
    }

    private Route resource() {
        return apply(FilteredResourceRequest.class, kafkaProducerService::filteredResourceMulti);
    }

    private Route offset() {
        return apply(TopicOffsetMessage.class, kafkaProducerService::topicOffsetMulti);
    }

    private Route error() {
        return apply(AuditErrorMessage.class, kafkaProducerService::auditErrorMulti);
    }
}
