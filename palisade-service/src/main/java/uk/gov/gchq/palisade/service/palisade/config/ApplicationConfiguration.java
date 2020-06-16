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

package uk.gov.gchq.palisade.service.palisade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
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

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph.
 */
@Configuration
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

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

    /**
     * User service bean created with an userClient which uses Feign to send rest requests to the User Service
     * Feign will either resolve hostnames from eureka or values in the relevant profiles yaml
     *
     * @param userClient the user client
     * @return the user service
     */
    @Bean
    public UserService userService(final UserClient userClient) {
        return new UserService(userClient, getAsyncExecutor());
    }

    /**
     * Audit service bean created with an auditClient which uses Feign to send rest requests to the Audit Service
     * Feign will either resolve hostnames from eureka or values in the relevant profiles yaml
     *
     * @param auditClient the audit client
     * @return the audit service
     */
    @Bean
    public AuditService auditService(final AuditClient auditClient) {
        return new AuditService(auditClient);
    }

    /**
     * Resource service bean created with an resourceClient which uses Feign to send rest requests to the Resource Service
     * Feign will either resolve hostnames from eureka or values in the relevant profiles yaml
     *
     * @param resourceClient the resource client
     * @param objectMapper   the object mapper
     * @return the resource service
     */
    @Bean
    public ResourceService resourceService(final ResourceClient resourceClient, final ObjectMapper objectMapper) {
        return new ResourceService(resourceClient, objectMapper, getAsyncExecutor());
    }

    /**
     * Policy service bean created with an policyClient which uses Feign to send rest requests to the Policy Service
     * Feign will either resolve hostnames from eureka or values in the relevant profiles yaml
     *
     * @param policyClient the policy client
     * @return the policy service
     */
    @Bean
    public PolicyService policyService(final PolicyClient policyClient) {
        return new PolicyService(policyClient, getAsyncExecutor());
    }

    @Bean
    public ResultAggregationService resultAggregationService(final PersistenceLayer persistenceLayer) {
        return new ResultAggregationService(persistenceLayer);
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
