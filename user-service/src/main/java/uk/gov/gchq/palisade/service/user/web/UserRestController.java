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
package uk.gov.gchq.palisade.service.user.web;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.UserConfiguration;
import uk.gov.gchq.palisade.service.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.user.model.AddUserRequest;
import uk.gov.gchq.palisade.service.user.model.GetUserRequest;
import uk.gov.gchq.palisade.service.user.model.Token;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/")
public class UserRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestController.class);
    private final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> upstreamSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;
    private UserService service;
    private UserConfiguration userConfig;

    public UserRestController(
            @Qualifier("userService") final UserService service,
            @Qualifier("userConfiguration") final UserConfiguration configuration,
            final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> upstreamSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        this.upstreamSink = upstreamSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
        this.service = service;
        this.userConfig = configuration;
    }

    @PostMapping(value = "/getUser", consumes = "application/json", produces = "application/json")
    public User getUserRequest(@RequestBody final GetUserRequest request) {
        LOGGER.info("Invoking GetUserRequest: {}", request);
        return service.getUser(request.userId);
    }

    @PostMapping(value = "/addUser", consumes = "application/json", produces = "application/json")
    public Boolean addUserRequest(@RequestBody final AddUserRequest request) {
        LOGGER.info("Invoking AddUserRequest: {}", request);
        return service.addUser(request.user).equals(request.user);
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/request", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> userRequest(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody(required = false) UserRequest request) {

        // Process the request and return results
        return this.processRequest(headers, Collections.singletonList(request));
    }

    private ResponseEntity<Void> processRequest(
            final Map<String, String> headers,
            final Collection<UserRequest> requests) {
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

    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        LOGGER.info("Prepopulating using user config: {}", userConfig.getClass());
        // Add example users to the user-service cache
        userConfig.getUsers().stream()
                .map(UserPrepopulationFactory::build)
                .peek(user -> LOGGER.debug(user.toString()))
                .forEach(user -> service.addUser(user));
    }
}
