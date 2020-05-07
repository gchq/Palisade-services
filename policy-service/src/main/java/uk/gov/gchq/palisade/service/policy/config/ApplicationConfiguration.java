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
package uk.gov.gchq.palisade.service.policy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.gchq.palisade.service.policy.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.policy.service.NullPolicyService;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceHierarchyProxy;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdPolicyConfiguration policyConfiguration() {
        return new StdPolicyConfiguration();
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    public StdPolicyPrepopulationFactory policyPrepopulationFactory() {
        return new StdPolicyPrepopulationFactory();
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdUserConfiguration userConfiguration() {
        return new StdUserConfiguration();
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std", matchIfMissing = true)
    public StdUserPrepopulationFactory userPrepopulationFactory() {
        return new StdUserPrepopulationFactory();
    }

    @Bean("nullService")
    @Qualifier("impl")
    public PolicyService nullPolicyService() {
        return new NullPolicyService();
    }

    @Bean("cachingProxy")
    public PolicyServiceCachingProxy cachedPolicyService(final @Qualifier("impl") PolicyService service) {
        PolicyServiceCachingProxy policyServiceCachingProxy = new PolicyServiceCachingProxy(service);
        LOGGER.debug("Instantiated CachedPolicyService");
        return policyServiceCachingProxy;
    }

    @Bean("hierarchicalProxy")
    @Qualifier("controller")
    public PolicyServiceHierarchyProxy hierarchicalPolicyService(final PolicyServiceCachingProxy cache) {
        PolicyServiceHierarchyProxy policyServiceHierarchyProxy = new PolicyServiceHierarchyProxy(cache);
        LOGGER.debug("Instantiated HierarchicalPolicyService");
        return policyServiceHierarchyProxy;
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
