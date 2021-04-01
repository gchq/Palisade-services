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
package uk.gov.gchq.palisade.service.policy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.service.policy.common.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.policy.common.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.policy.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.policy.service.NullPolicyService;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceAsyncProxy;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceHierarchyProxy;

import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    /**
     * A container for a number of {@link StdPolicyPrepopulationFactory} builders used for creating Policies
     * These wil be populated further using a UserConfiguration and ResourceConfiguration
     * These policies will be used for pre-populating the {@link PolicyService}
     *
     * @return a standard {@link PolicyConfiguration} containing a list of {@link PolicyPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdPolicyConfiguration policyConfiguration() {
        return new StdPolicyConfiguration();
    }

    /**
     * A factory for a map of {@link Rules} to a resourceId, using:
     * - a {@link String} value of the resourceId
     * - a list of {@link Rule} resource-level rules operating on a {@link Resource}
     * - a list of {@link Rule} record-level rules operating on the type of a {@link LeafResource}
     *
     * @return a standard {@link PolicyPrepopulationFactory} capable of building a Policy from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    public StdPolicyPrepopulationFactory policyPrepopulationFactory() {
        return new StdPolicyPrepopulationFactory();
    }

    /**
     * The simplest implementation of a policy service, allows unit tests and small services to use a lightweight policy service
     *
     * @return a new instance of the nullPolicyService
     */
    @Bean
    public PolicyService nullPolicyService() {
        LOGGER.debug("Instantiated nullPolicyService");
        return new NullPolicyService();
    }

    /**
     * An implementation of the policy service that allows caching to take place, either using Redis or Caffeine
     *
     * @param service the service that will be implemented
     * @return a new instance of the PolicyServiceCachingProxy
     */
    @Bean
    public PolicyServiceCachingProxy cachedPolicyService(final PolicyService service) {
        PolicyServiceCachingProxy policyServiceCachingProxy = new PolicyServiceCachingProxy(service);
        LOGGER.debug("Instantiated CachedPolicyService");
        return policyServiceCachingProxy;
    }

    /**
     * An implementation of the PolicyService that contains java code used to apply, get and set rules against resources
     *
     * @param cache the cache layer that this service will implement
     * @return a new instance of the PolicyServiceHierarchyProxy
     */
    @Bean
    public PolicyServiceHierarchyProxy hierarchicalPolicyService(final PolicyServiceCachingProxy cache) {
        PolicyServiceHierarchyProxy policyServiceHierarchyProxy = new PolicyServiceHierarchyProxy(cache);
        LOGGER.debug("Instantiated HierarchicalPolicyService");
        return policyServiceHierarchyProxy;
    }

    /**
     * AysncPolicyServiceProxy sits between akka and a caching layer,
     * allowing akka to make async calls to a service and not have to wait for a response
     *
     * @param hierarchy {@link PolicyServiceHierarchyProxy} as the service performing the majority of the code manipulation
     * @param executor  {@link Executor} This interface provides a way of decoupling task submission from the mechanics of how each task will be run, including details of thread use, scheduling, etc.
     * @return a new instance of a PolicyServiceAsyncProxy
     */
    @Bean
    public PolicyServiceAsyncProxy asyncPolicyServiceProxy(
            final PolicyServiceHierarchyProxy hierarchy,
            final @Qualifier("threadPoolTaskExecutor") Executor executor) {
        LOGGER.debug("Instantiated asyncUserServiceProxy");
        return new PolicyServiceAsyncProxy(hierarchy, executor);
    }

    /**
     * ObjectMapper used in serializing and deserializing
     *
     * @return a new instance of the ObjectMapper
     */
    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Override
    @Bean("threadPoolTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(6);
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
    }
}
