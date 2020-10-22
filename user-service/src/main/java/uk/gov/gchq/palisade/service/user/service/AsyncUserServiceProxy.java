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

package uk.gov.gchq.palisade.service.user.service;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.model.Token;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * An asynchronous service for processing a cacheable method.
 */
public class AsyncUserServiceProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUserServiceProxy.class);
    private final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> upstreamSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;
    private final Executor executor;
    private final UserService service;

    /**
     * Constructor for the {@link AsyncUserServiceProxy}
     *
     * @param upstreamSink       a sink to the upstream topic
     * @param upstreamConfig     the config for the topic (name, partitions, ...)
     * @param materializer       the akka system materializer
     * @param service            the {@link UserService} implementation
     * @param executor           the {@link Executor} for the service
     */
    public AsyncUserServiceProxy(final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> upstreamSink,
                                 final ConsumerTopicConfiguration upstreamConfig,
                                 final Materializer materializer,
                                 final UserService service,
                                 final Executor executor) {
        this.upstreamSink = upstreamSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
        this.service = service;
        this.executor = executor;
    }

    public ResponseEntity<Void> processRequest(final Map<String, String> headers,
                                               Collection<UserRequest> requests) {
        // Get token from headers
        String token = Optional.ofNullable(headers.get(Token.HEADER))
                .orElseThrow(() -> new NoSuchElementException("No token specified in headers"));

        // Get topic and calculate partition, unless this service has been assigned a partition
        Topic topic = this.upstreamConfig.getTopics().get("input-topic");
        int partition = Optional.ofNullable(topic.getAssignment())
                .orElseGet(() -> Token.toPartition(token, topic.getPartitions()));

        // Convert headers to kafka style
        List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset())))
                .collect(Collectors.toList());

        // Process requests
        // Akka reactive streams can't have null elements, so map to and from optional
        Source.fromJavaStream(() -> requests.stream().map(Optional::ofNullable))
                .map(request -> new ProducerRecord<String, UserRequest>(topic.getName(), partition, null, request.orElse(null), kafkaHeaders))
                .toMat(this.upstreamSink, Keep.right())
                .run(this.materializer)
                .toCompletableFuture()
                .join();

        // Return results
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    public CompletableFuture<User> getUser(final UserRequest userRequest) {
        LOGGER.debug("Getting user '{}' from cache", userRequest.getUserId());
        return CompletableFuture.supplyAsync(() -> service.getUser(userRequest.userId), executor);
    }

    public User addUser(final User user) {
        LOGGER.debug("Adding user '{}' to cache", user.getUserId().getId());
        return service.addUser(user);
    }
}
