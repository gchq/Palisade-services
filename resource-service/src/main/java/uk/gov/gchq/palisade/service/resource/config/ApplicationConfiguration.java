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

package uk.gov.gchq.palisade.service.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceConfigurationService;
import uk.gov.gchq.palisade.service.resource.service.NullResourceService;
import uk.gov.gchq.palisade.service.resource.service.ResourceServiceCachingProxy;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean(name = "null")
    @ConditionalOnProperty(prefix = "resource-service", name = "implementation", havingValue = "null", matchIfMissing = true)
    public NullResourceService nullResourceService() {
        LOGGER.info("Instantiated NullResourceService");
        return new NullResourceService();
    }

    @Bean(name = "hadoop")
    @ConditionalOnProperty(prefix = "resource-service", name = "implementation", havingValue = "hadoop")
    public HadoopResourceConfigurationService hadoopResourceConfigurationService() throws IOException {
        LOGGER.info("Instantiated HadoopConfigurationService");
        return new HadoopResourceConfigurationService(hadoopConfiguration());
    }

    @Bean(name = "cacheProxy")
    public ResourceServiceCachingProxy resourceService(final ResourceService service) {
        ResourceServiceCachingProxy cachingProxy = new ResourceServiceCachingProxy(service);
        LOGGER.info("Instantiated ResourceServiceCachingProxy");
        return cachingProxy;
    }

    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        return new org.apache.hadoop.conf.Configuration();
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
}
