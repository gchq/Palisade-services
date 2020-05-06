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
import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.domain.ResourceConverter;
import uk.gov.gchq.palisade.service.resource.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.resource.repository.CompletenessRepository;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ResourceRepository;
import uk.gov.gchq.palisade.service.resource.repository.SerialisedFormatRepository;
import uk.gov.gchq.palisade.service.resource.repository.TypeRepository;
import uk.gov.gchq.palisade.service.resource.service.ConfiguredHadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties
@EnableAutoConfiguration
@EnableAsync
@EnableScheduling
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "web")
    public ClientConfiguration clientConfiguration(final Optional<EurekaClient> eurekaClient) {
        return new ClientConfiguration(eurekaClient);
    }

    @Bean
    public Supplier<List<Entry<Resource, LeafResource>>> configuredResourceBuilder(final ResourceConfiguration resourceConfiguration, final ClientConfiguration clientConfig) {
        Function<String, ConnectionDetail> connectionDetailMapper = serviceName -> new SimpleConnectionDetail()
                .uri(clientConfig.getClientUri(serviceName)
                        .orElseThrow(() -> new IllegalArgumentException("No service found with name: " + serviceName))
                        .toString());
        return () -> resourceConfiguration.getResources().stream()
                .map(factory -> factory.build(connectionDetailMapper))
                .collect(Collectors.toList());
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resource", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdResourceConfiguration resourceConfiguration() {
        return new StdResourceConfiguration();
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resource", havingValue = "std", matchIfMissing = true)
    public StdResourcePrepopulationFactory resourcePrepopulationFactory() {
        return new StdResourcePrepopulationFactory();
    }

    @Bean(name = "jpa-persistence")
    public JpaPersistenceLayer persistenceLayer(final CompletenessRepository completenessRepository, final ResourceRepository resourceRepository, final TypeRepository typeRepository, final SerialisedFormatRepository serialisedFormatRepository) {
        return new JpaPersistenceLayer(completenessRepository, resourceRepository, typeRepository, serialisedFormatRepository);
    }

    @Bean
    public ResourceConverter resourceConverter() {
        return new ResourceConverter();
    }

    @Bean
    public StreamingResourceServiceProxy resourceServiceProxy(final JpaPersistenceLayer persistenceLayer, final @Qualifier("impl") ResourceService delegate, final ObjectMapper objectMapper) {
        return new StreamingResourceServiceProxy(persistenceLayer, delegate, objectMapper);
    }

    @Bean("simpleResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "simple")
    @Qualifier("impl")
    public ResourceService simpleResourceService(final ClientConfiguration clientConfiguration) {
        return new SimpleResourceService(clientConfiguration);
    }

    @Bean("hadoopResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "hadoop")
    @Qualifier("impl")
    public HadoopResourceService hadoopResourceService(final org.apache.hadoop.conf.Configuration config) throws IOException {
        return new ConfiguredHadoopResourceService(config);
    }

    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        return new org.apache.hadoop.conf.Configuration();
    }

    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper() {
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

    @Bean(name = "concurrentTaskExecutor")
    public ConcurrentTaskExecutor getTaskExecutor() {
        return new ConcurrentTaskExecutor(this.getAsyncExecutor());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
    }

}
