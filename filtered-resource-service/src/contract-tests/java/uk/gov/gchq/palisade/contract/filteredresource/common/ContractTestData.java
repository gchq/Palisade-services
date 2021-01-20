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

package uk.gov.gchq.palisade.contract.filteredresource.common;

import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.headers.RawHeader;
import akka.japi.Pair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Strictly control content of messages by specifying exact JSON string contents, then converting to appropriate object types.
 * This enforces the contract between services through specifying the string values for messages, and any changes to the contract
 * require changes to the string values.
 */
public final class ContractTestData {

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final String REQUEST_JSON = "{\"userId\":\"test-user\",\"resourceId\":\"\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"purpose\"}},\"resource\":null}";
    public static final JsonNode REQUEST_NODE;
    public static final JsonNode TOPIC_OFFSET_MSG_JSON_NODE;
    public static final TopicOffsetMessage TOPIC_OFFSET_MESSAGE;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TOPIC_OFFSET_MESSAGE_JSON = "{\"queuePointer\":1}";

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
            TOPIC_OFFSET_MSG_JSON_NODE = MAPPER.readTree(TOPIC_OFFSET_MESSAGE_JSON);
            TOPIC_OFFSET_MESSAGE = ContractTestData.MAPPER.treeToValue(ContractTestData.TOPIC_OFFSET_MSG_JSON_NODE, TopicOffsetMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to setup contract test data", e);
        }
    }

    private ContractTestData() {
    }

    public static Function<String, WebSocketMessage> getCompleteMsgBuilder() {
        return (token) -> WebSocketMessage.Builder.create()
                .withType(MessageType.COMPLETE)
                .withHeader(Token.HEADER, token)
                .noHeaders()
                .noBody();
    }

    public static BiFunction<String, LeafResource, WebSocketMessage> getResponseBuilder() {
        return (token, leafResource) -> WebSocketMessage.Builder.create()
                .withType(MessageType.RESOURCE)
                .withHeader(Token.HEADER, token)
                .noHeaders()
                .withBody(leafResource);
    }

    public static Function<String, FilteredResourceRequest> getResourceBuilder() {
        return resourceId -> FilteredResourceRequest.Builder.create()
                .withUserId("userId")
                .withResourceId("file:/file/")
                .withContext(new Context().purpose("purpose"))
                .withResource(new FileResource()
                        .id("file:/file/" + resourceId)
                        .serialisedFormat("fmt")
                        .type("type")
                        .connectionDetail(new SimpleConnectionDetail()
                                .serviceName("data-service"))
                        .parent(new SystemResource().id("file:/file/")));
    }

    public static class ParameterizedArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) throws Exception {
            // Builders
            Function<String, FilteredResourceRequest> resourceBuilder = resourceId -> FilteredResourceRequest.Builder.create()
                    .withUserId("userId")
                    .withResourceId("file:/file/")
                    .withContext(new Context().purpose("purpose"))
                    .withResource(new FileResource()
                            .id("file:/file/" + resourceId)
                            .serialisedFormat("fmt")
                            .type("type")
                            .connectionDetail(new SimpleConnectionDetail()
                                    .serviceName("data-service"))
                            .parent(new SystemResource().id("file:/file/")));
            Function<Long, TopicOffsetMessage> offsetBuilder = TopicOffsetMessage.Builder.create()
                    ::withQueuePointer;
            BiFunction<String, LeafResource, WebSocketMessage> responseBuilder = (token, leafResource) -> WebSocketMessage.Builder.create()
                    .withType(MessageType.RESOURCE)
                    .withHeader(Token.HEADER, token)
                    .noHeaders()
                    .withBody(leafResource);
            // Special instances
            HttpHeader startHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.START));
            HttpHeader endHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.END));
            WebSocketMessage ctsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
            Function<String, WebSocketMessage> completeMsgBuilder = (token) -> WebSocketMessage.Builder.create()
                    .withType(MessageType.COMPLETE)
                    .withHeader(Token.HEADER, token)
                    .noHeaders()
                    .noBody();
            return Stream.of(
                    // Test for 'early' client - topic offset message has offset
                    Arguments.of(
                            "test-token-1",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), endHeader), null)
                            ),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), offsetBuilder.apply(0L))
                            ),
                            Map.of(),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-1")
                            ),
                            List.of()
                    ),
                    // Test for 'late' client - persistence has offset
                    Arguments.of(
                            "test-token-2",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), endHeader), null)
                            ),
                            List.of(),
                            Map.of(
                                    "test-token-2", 0L
                            ),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-2")
                            ),
                            List.of()
                    ),
                    //Test no start of stream marker
                    Arguments.of(
                            "test-token-3",
                            List.of(
                                    //No Start Marker
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3"), endHeader), null)
                            ),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), offsetBuilder.apply(0L))
                            ),
                            Map.of(),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-3")
                            ),
                            List.of(
                                    AuditErrorMessage.Builder.create().withUserId("userId")
                                            .withResourceId("file:/file")
                                            .withContext(new Context().purpose("purpose"))
                                            .withAttributes(Collections.emptyMap())
                                            .withError(new Throwable("No Start Marker was observed for token: " + "test-token-3"))
                            )
                    )
            );
        }
    }
}
