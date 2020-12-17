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

package uk.gov.gchq.palisade.service.filteredresource.config;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.repository.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetRepository;
import uk.gov.gchq.palisade.service.filteredresource.service.AuditEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    TokenOffsetPersistenceLayer jpaTokenOffsetPersistenceLayer(
            final TokenOffsetRepository repository,
            final @Qualifier("applicationTaskExecutor") Executor executor) {
        return new JpaTokenOffsetPersistenceLayer(repository, executor);
    }

    // Replace this with a proper error reporting service (akka actors etc.)
    @Bean
    ErrorEventService loggingErrorReporterService() {
        LOGGER.warn("Using a Logging-only error reporter, this should be replaced by a proper implementation!");
        return (String token, Throwable exception) -> LOGGER.error("An error was reported for token {}:", token, exception);
    }

    @Bean
    AuditEventService auditEventService() {
        return new AuditEventService(Collections.emptyMap());
    }

    @Bean
    OffsetEventService topicOffsetService(final TokenOffsetPersistenceLayer persistenceLayer) {
        return new OffsetEventService(persistenceLayer);
    }

    // Replace this with a proper error handling mechanism (kafka queues etc.)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (String token, FilteredResourceRequest request, Throwable error) -> LOGGER.error("Token {} and request {} threw exception", token, request, error);
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

    @Primary
    @Bean("jsonSerialiser")
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
