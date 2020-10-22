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
package uk.gov.gchq.palisade.service.user.config;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.user.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.AsyncUserServiceProxy;
import uk.gov.gchq.palisade.service.user.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.user.service.NullUserService;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.service.CacheableUserServiceProxy;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    /**
     * A container for a number of {@link StdUserPrepopulationFactory} builders used for creating {@link uk.gov.gchq.palisade.User}s
     * These users will be used for prepopulating the {@link UserService}
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.UserConfiguration} containing a list of {@link uk.gov.gchq.palisade.service.UserPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdUserConfiguration userConfiguration() {
        return new StdUserConfiguration();
    }

    /**
     * A factory for {@link uk.gov.gchq.palisade.User} objects, using a userId, a list of authorisations and a list of roles
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.UserPrepopulationFactory} capable of building a {@link uk.gov.gchq.palisade.User} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std")
    public StdUserPrepopulationFactory userPrepopulationFactory() {
        return new StdUserPrepopulationFactory();
    }

    @Bean("userService")
    public CacheableUserServiceProxy cacheableUserServiceProxy(final Set<UserService> userServices) {
        CacheableUserServiceProxy cacheableUserServiceProxy = new CacheableUserServiceProxy(userServices.stream().findFirst().orElse(null));
        LOGGER.info("Instantiated CacheableUserServiceProxy with {}", (userServices.stream().findFirst().orElse(null)));
        return cacheableUserServiceProxy;
    }

    @Bean
    public AsyncUserServiceProxy asyncUserServiceProxy(@Qualifier("plainRequestSink") final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> sink,
                                                       final ConsumerTopicConfiguration upstreamConfig,
                                                       final Materializer materializer,
                                                       @Qualifier("userService") final UserService service,
                                                       @Qualifier("applicationTaskExecutor") final Executor executor) {
        LOGGER.info("Instantiated AsyncUserServiceProxy with {}", service.getClassName());
        return new AsyncUserServiceProxy(sink, upstreamConfig, materializer, service, executor);
    }

    @Bean(name = "null-user-service")
    @ConditionalOnProperty(prefix = "user-service", name = "service", havingValue = "null-user-service", matchIfMissing = true)
    public NullUserService nullUserService() {
        LOGGER.info("Instantiated NullUserService");
        return new NullUserService();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Override
    @Bean("applicationTaskExecutor")
    public Executor getAsyncExecutor() {
        return Optional.of(new ThreadPoolTaskExecutor()).stream().peek(ex -> {
            ex.setThreadNamePrefix("AppThreadPool-");
            ex.setCorePoolSize(6);
            LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        }).findFirst().orElse(null);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
    }

    // Replace this with a proper error handling mechanism (kafka queues etc.)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (String token, AuditErrorMessage message) -> LOGGER.error("Token {} and userId {} threw exception {}", token, message.getUserId(), message.getAttributes(), message.getError());
    }
}
