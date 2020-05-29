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
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ResourceRepository;
import uk.gov.gchq.palisade.service.resource.repository.SerialisedFormatRepository;
import uk.gov.gchq.palisade.service.resource.repository.TypeRepository;
import uk.gov.gchq.palisade.service.resource.service.ConfiguredHadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;

import java.io.IOException;
import java.net.URI;
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
    private static final Integer RETRY_AFTER = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    /**
     * A generic resolver from service names to {@link URI}s
     * Uses Eureka if available, otherwise uses the Spring yaml configuration value directly as a URI (useful for k8s)
     *
     * @param discoveryClient an optional {@link DiscoveryClient} for resolving service names
     * @return a {@link ClientConfiguration} capable of resolving service names in multiple environments
     */
    @Bean
    @ConfigurationProperties(prefix = "web")
    public ClientConfiguration clientConfiguration(final ObjectProvider<DiscoveryClient> discoveryClient) {
        return new ClientConfiguration(discoveryClient.getIfAvailable(() -> null));
    }

    /**
     * A wrapper around a {@link ResourceConfiguration} that dynamically resolves the configured {@link ConnectionDetail} using the {@link ClientConfiguration}
     *
     * @param resourceConfig the {@link ResourceConfiguration} to use to build resource
     * @param clientConfig the {@link ClientConfiguration} to use to resolve the {@link java.net.URI} of a data-service at runtime
     * @return a getter for a list of {@link Resource}s, each paired with an associated {@link LeafResource}, see {@link ResourceConfiguration} for more info
     */
    @Bean
    public Supplier<List<Entry<Resource, LeafResource>>> configuredResourceBuilder(final ResourceConfiguration resourceConfig, final ClientConfiguration clientConfig) {
        Function<String, ConnectionDetail> connectionDetailMapper = serviceName -> new SimpleConnectionDetail()
                .uri(clientConfig.getClientUri(serviceName)
                        .or(() -> {
                            LOGGER.warn("No service found with name: {} - will retry once more in {}ms", serviceName, RETRY_AFTER);
                            try {
                                Thread.sleep(RETRY_AFTER);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            return clientConfig.getClientUri(serviceName);
                        })
                        .orElseThrow(() -> new IllegalArgumentException("No service found with name: " + serviceName))
                        .toString());
        return () -> resourceConfig.getResources().stream()
                .map(factory -> factory.build(connectionDetailMapper))
                .collect(Collectors.toList());
    }

    /**
     * A container for a number of {@link StdResourcePrepopulationFactory} builders used for creating {@link uk.gov.gchq.palisade.resource.Resource}s
     * These resources will be used for prepopulating the {@link ResourceService}
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.ResourceConfiguration} containing a list of {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdResourceConfiguration resourceConfiguration() {
        return new StdResourceConfiguration();
    }

    /**
     * A factory for {@link uk.gov.gchq.palisade.resource.Resource} objects, wrapping the {@link uk.gov.gchq.palisade.util.ResourceBuilder} with a type and serialisedFormat
     * Note that this does not include resolving an appropriate {@link ConnectionDetail}, this is handled elsewhere
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory} capable of building a {@link uk.gov.gchq.palisade.resource.Resource} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    public StdResourcePrepopulationFactory resourcePrepopulationFactory() {
        return new StdResourcePrepopulationFactory();
    }

    /**
     * An implementation of the {@link PersistenceLayer} interface to be used by the {@link ResourceService} as if it were a cache
     * See the {@link JpaPersistenceLayer} for an in-depth description of how and why each part is used
     * While code introspection may suggest no beans found for these types, they will be created by Spring
     *
     * @param completenessRepository the completeness repository to use, storing whether persistence will return a response for any given request
     * @param resourceRepository the resource repository to use, a store of each available {@link LeafResource} and its parents
     * @param typeRepository the type repository to use, a one-to-many relation of types to resource ids
     * @param serialisedFormatRepository the serialisedFormat repository to use, a one-to-many relation of serialisedFormats to resource ids
     * @return a {@link JpaPersistenceLayer} object with the appropriate repositories configured for storing resource (meta)data
     */
    @Bean(name = "jpa-persistence")
    public JpaPersistenceLayer persistenceLayer(
            final CompletenessRepository completenessRepository,
            final ResourceRepository resourceRepository,
            final TypeRepository typeRepository,
            final SerialisedFormatRepository serialisedFormatRepository) {
        return new JpaPersistenceLayer(completenessRepository, resourceRepository, typeRepository, serialisedFormatRepository);
    }

    @Bean
    public ResourceConverter resourceConverter() {
        return new ResourceConverter();
    }

    /**
     * A proxy-like object for a {@link ResourceService} using {@link java.util.stream.Stream}s to communicate with a {@link org.springframework.web.bind.annotation.RestController}
     * This includes writing the {@link java.util.stream.Stream} of {@link Resource}s to the {@link java.io.OutputStream} of a {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}
     *
     * @param persistenceLayer a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate a 'real' {@link ResourceService} to delegate requests to when not found in the persistenceLayer
     *                 This must be marked 'impl' to designate that it is the backing implementation to use as there may be multiple proxies, services etc.
     * @param objectMapper a {@link ObjectMapper} used for serialisation when writing each {@link Resource} to the {@link java.io.OutputStream}
     * @param resourceBuilder a {@link Supplier} of resources as built by a {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory}, but with a connection detail attached
     * @return a {@link StreamingResourceServiceProxy} to handle the streams produced by the persistenceLayer and delegate {@link ResourceService}
     */
    @Bean
    public StreamingResourceServiceProxy resourceServiceProxy(
            final PersistenceLayer persistenceLayer,
            final @Qualifier("impl") ResourceService delegate,
            final ObjectMapper objectMapper,
            final Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder) {
        return new StreamingResourceServiceProxy(persistenceLayer, delegate, objectMapper, resourceBuilder);
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
