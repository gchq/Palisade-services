/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.palisade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.palisade.domain.ContextConverter;
import uk.gov.gchq.palisade.service.palisade.domain.LeafResourceConverter;
import uk.gov.gchq.palisade.service.palisade.domain.RulesConverter;
import uk.gov.gchq.palisade.service.palisade.domain.UserConverter;
import uk.gov.gchq.palisade.service.palisade.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.palisade.repository.DataRequestRepository;
import uk.gov.gchq.palisade.service.palisade.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.repository.LeafResourceRulesRepository;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.service.AuditService;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.PolicyService;
import uk.gov.gchq.palisade.service.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.palisade.service.ResultAggregationService;
import uk.gov.gchq.palisade.service.palisade.service.SimplePalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.UserService;
import uk.gov.gchq.palisade.service.palisade.web.AuditClient;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;
import uk.gov.gchq.palisade.service.palisade.web.UserClient;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Bean configuration and dependency injection graph.
 */
@Configuration
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "web")
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration();
    }

    @Bean(name = "jpa-persistence")
    public JpaPersistenceLayer persistenceLayer(final DataRequestRepository dataRequestRepository, final LeafResourceRulesRepository leafResourceRulesRepository) {
        return new JpaPersistenceLayer(dataRequestRepository, leafResourceRulesRepository, getAsyncExecutor());
    }

    @Bean
    public ContextConverter contextConverter(final ObjectMapper objectMapper) {
        return new ContextConverter(objectMapper);
    }

    @Bean
    public LeafResourceConverter leafResourceConverter(final ObjectMapper objectMapper) {
        return new LeafResourceConverter(objectMapper);
    }

    @Bean
    public RulesConverter rulesConverter(final ObjectMapper objectMapper) {
        return new RulesConverter(objectMapper);
    }

    @Bean
    public UserConverter userConverter(final ObjectMapper objectMapper) {
        return new UserConverter(objectMapper);
    }

    @Bean
    public PalisadeService palisadeService(final PersistenceLayer persistenceLayer,
                                           final AuditService auditService,
                                           final UserService userService,
                                           final PolicyService policyService,
                                           final ResourceService resourceService,
                                           final ResultAggregationService resultAggregationService) {
        return new SimplePalisadeService(auditService,
                userService,
                policyService,
                resourceService,
                persistenceLayer,
                getAsyncExecutor(),
                resultAggregationService);
    }

    @Bean
    public UserService userService(final UserClient userClient, final ClientConfiguration clientConfig) {
        Supplier<URI> userUriSupplier = () -> clientConfig
                .getClientUri("user-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'user-service' - see 'web.client' properties or discovery service registration"));
        return new UserService(userClient, userUriSupplier, getAsyncExecutor());
    }

    @Bean
    public AuditService auditService(final AuditClient auditClient, final ClientConfiguration clientConfig) {
        Supplier<URI> auditUriSupplier = () -> clientConfig
                .getClientUri("audit-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'audit-service' - see 'web.client' properties or discovery service registration"));
        return new AuditService(auditClient, auditUriSupplier, getAsyncExecutor());
    }

    @Bean
    public ResourceService resourceService(final ResourceClient resourceClient, final ClientConfiguration clientConfig, final ObjectMapper objectMapper) {
        Supplier<URI> resourceUriSupplier = () -> clientConfig
                .getClientUri("resource-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'resource-service' - see 'web.client' properties or discovery service registration"));
        return new ResourceService(resourceClient, resourceUriSupplier, objectMapper, getAsyncExecutor());
    }

    @Bean
    public PolicyService policyService(final PolicyClient policyClient, final ClientConfiguration clientConfig) {
        Supplier<URI> policyUriSupplier = () -> clientConfig
                .getClientUri("policy-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'policy-service' - see 'web.client' properties or discovery service registration"));
        return new PolicyService(policyClient, policyUriSupplier, getAsyncExecutor());
    }

    @Bean
    public ResultAggregationService resultAggregationService(final AuditService auditService, final PersistenceLayer persistenceLayer) {
        return new ResultAggregationService(auditService, persistenceLayer);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Override
    @Bean("threadPoolTaskExecutor")
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

}
