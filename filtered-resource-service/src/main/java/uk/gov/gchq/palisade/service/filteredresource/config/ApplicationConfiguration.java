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

package uk.gov.gchq.palisade.service.filteredresource.config;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.service.filteredresource.common.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.JpaTokenErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageController;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageController.TokenErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageRepository;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetRepository;
import uk.gov.gchq.palisade.service.filteredresource.service.AuditEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorMessageEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class ApplicationConfiguration {

    @Bean
    TokenOffsetPersistenceLayer jpaTokenOffsetPersistenceLayer(
            final TokenOffsetRepository repository,
            final @Qualifier("applicationTaskExecutor") Executor executor) {
        return new JpaTokenOffsetPersistenceLayer(repository, executor);
    }

    /**
     * Bean for the {@link TokenErrorMessagePersistenceLayer}.
     * Connect the Redis repository to the persistence layer, providing an executor for any async requests
     *
     * @param repository an instance of the tokenErrorMessage repository, backed by redis
     * @param executor   an async executor, preferably a {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
     * @return a {@link TokenErrorMessagePersistenceLayer} wrapping the repository instance, providing async methods for getting error messages from persistence
     */
    @Bean
    TokenErrorMessagePersistenceLayer jpaTokenErrorMessagePersistenceLayer(final TokenErrorMessageRepository repository,
                                                                           final @Qualifier("applicationTaskExecutor") Executor executor) {
        return new JpaTokenErrorMessagePersistenceLayer(repository, executor);
    }

    @Bean
    AuditEventService auditEventService() {
        return new AuditEventService(Collections.emptyMap());
    }

    @Bean
    OffsetEventService topicOffsetService(final TokenOffsetPersistenceLayer persistenceLayer) {
        return new OffsetEventService(persistenceLayer);
    }

    /**
     * A Bean for the handling of AuditErrorMessages from other Palisade Services. Taking a link to the persistence Layer, the service will monitor
     * kafka to recieve and persist AuditErrorMessages to the backing store
     *
     * @param persistenceLayer A link to the backing store technology, for the storage of tokens and exceptions
     * @return a new ErrorMessageEventService, instantiated with a link to the backing store.
     */
    @Bean
    ErrorMessageEventService errorMessageEventService(final TokenErrorMessagePersistenceLayer persistenceLayer) {
        return new ErrorMessageEventService(persistenceLayer);
    }

    /**
     * Create the TokenOffsetController, controlling get access to the persistence layer and asynchronously waiting until offsets
     * are available. That is, a Source of one element "token" through the {@link TokenOffsetController#asGetterFlow(ActorRef)}
     * Flow will only emit an element to downstream once an offset is available from either persistence or "masked-resource-offset"
     * kafka topic.
     *
     * @param persistenceLayer an instance of the TokenOffsetPersistenceLayer (this must still be written to explicitly)
     * @return a (running) ActorSystem for {@link TokenOffsetCommand}s
     * @implNote it is most likely this object will be used in conjunction with {@link TokenOffsetController#asGetterFlow(ActorRef)}
     * and {@link TokenOffsetController#asSetterSink(ActorRef)}, supplying this object as the argument, producing an
     * {@link akka.stream.javadsl.Flow} or {@link akka.stream.javadsl.Sink} respectively.
     */
    @Bean
    ActorRef<TokenOffsetCommand> tokenOffsetController(final TokenOffsetPersistenceLayer persistenceLayer) {
        return TokenOffsetController.create(persistenceLayer);
    }

    /**
     * Create the TokenErrorMessageController, controlling access to the persistence layer and asynchronously waiting until error messages
     * are available. That is, a Source of one element "token" through the {@link TokenErrorMessageController#asGetterFlow(ActorRef)}
     * Flow will only emit an element to downstream once an excption is available from either persistence via the "error" kafka topic.
     *
     * @param persistenceLayer an instance of the TokenErrorMessagePersistenceLayer
     * @return a (running) ActorSystem for {@link TokenErrorMessageCommand}s
     */
    @Bean
    ActorRef<TokenErrorMessageCommand> tokenErrorController(final TokenErrorMessagePersistenceLayer persistenceLayer) {
        return TokenErrorMessageController.create(persistenceLayer);
    }

    @Primary
    @Bean("jsonSerialiser")
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
