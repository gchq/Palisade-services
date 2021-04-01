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

package uk.gov.gchq.palisade.service.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.reader.common.ConnectionDetail;
import uk.gov.gchq.palisade.reader.common.ResourceConfiguration;
import uk.gov.gchq.palisade.reader.common.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.reader.common.ResourceService;
import uk.gov.gchq.palisade.reader.common.Service;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.Resource;
import uk.gov.gchq.palisade.reader.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.common.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.resource.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.resource.repository.CompletenessRepository;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ResourceRepository;
import uk.gov.gchq.palisade.service.resource.repository.SerialisedFormatRepository;
import uk.gov.gchq.palisade.service.resource.repository.TypeRepository;
import uk.gov.gchq.palisade.service.resource.service.ConfiguredHadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableR2dbcRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.reactive"})
@EnableConfigurationProperties({ResourceServiceConfigProperties.class})
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    private static final int THREAD_POOL = 6;
    private final ResourceServiceConfigProperties resourceServiceConfigProperties;

    @Value("${web.client.data-service:data-service}")
    private String dataServiceName;

    /**
     * Spring dependency-injection for dependant configs
     *
     * @param resourceServiceConfigProperties service-specific config
     */
    public ApplicationConfiguration(final ResourceServiceConfigProperties resourceServiceConfigProperties) {
        this.resourceServiceConfigProperties = resourceServiceConfigProperties;
    }

    /**
     * A wrapper around a {@link ResourceConfiguration} that dynamically resolves the configured {@link ConnectionDetail}
     *
     * @param resourceConfig the {@link ResourceConfiguration} to use to build resource
     * @return a getter for a list of {@link Resource}s, each paired with an associated {@link LeafResource}, see {@link ResourceConfiguration} for more info
     */
    @Bean
    public Supplier<List<Entry<Resource, LeafResource>>> configuredResourceBuilder(final ResourceConfiguration resourceConfig) {
        Function<String, ConnectionDetail> connectionDetailMapper = serviceName -> new SimpleConnectionDetail()
                .serviceName(serviceName);
        return () -> resourceConfig.getResources().stream()
                .map(factory -> factory.build(connectionDetailMapper))
                .collect(Collectors.toList());
    }

    /**
     * A container for a number of {@link StdResourcePrepopulationFactory} builders used for creating {@link Resource}s
     * These resources will be used for prepopulating the {@link ResourceService}
     *
     * @return a standard {@link ResourceConfiguration} containing a list of {@link ResourcePrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdResourceConfiguration resourceConfiguration() {
        return new StdResourceConfiguration();
    }

    /**
     * A factory for {@link Resource} objects, wrapping the {@link ResourceBuilder} with a type and serialisedFormat
     * Note that this does not include resolving an appropriate {@link ConnectionDetail}, this is handled elsewhere
     *
     * @return a standard {@link ResourcePrepopulationFactory} capable of building a {@link Resource} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    public StdResourcePrepopulationFactory resourcePrepopulationFactory() {
        return new StdResourcePrepopulationFactory();
    }

    /**
     * An implementation of the {@link PersistenceLayer} interface to be used by the {@link ResourceService} as if it were a cache
     * See the {@link ReactivePersistenceLayer} for an in-depth description of how and why each part is used
     * While code introspection may suggest no beans found for these types, they will be created by Spring
     *
     * @param completenessRepository     the completeness repository to use, storing whether persistence will return a response for any given request
     * @param resourceRepository         the resource repository to use, a store of each available {@link LeafResource} and its parents
     * @param typeRepository             the type repository to use, a one-to-many relation of types to resource ids
     * @param serialisedFormatRepository the serialisedFormat repository to use, a one-to-many relation of serialisedFormats to resource ids
     * @return a {@link ReactivePersistenceLayer} object with the appropriate repositories configured for storing resource (meta)data
     */
    @Bean
    public ReactivePersistenceLayer persistenceLayer(
            final CompletenessRepository completenessRepository,
            final ResourceRepository resourceRepository,
            final TypeRepository typeRepository,
            final SerialisedFormatRepository serialisedFormatRepository) {
        return new ReactivePersistenceLayer(completenessRepository, resourceRepository, typeRepository, serialisedFormatRepository);
    }

    /**
     * A proxy-like object for a {@link ResourceService} using {@link akka.stream.javadsl.Source}s.
     * This includes providing cache like behaviour and wrapping stream elements in success/error objects
     *
     * @param persistenceLayer a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate         a 'real' {@link ResourceService} to delegate requests to when not found in the persistenceLayer
     *                         This must be marked 'impl' to designate that it is the backing implementation to use as there may be multiple proxies, services etc.
     * @return a {@link ResourceServicePersistenceProxy} to handle the streams produced by the persistenceLayer and delegate {@link ResourceService}
     */
    @Bean
    public ResourceServicePersistenceProxy resourceServicePersistenceProxy(
            final PersistenceLayer persistenceLayer,
            final ResourceService delegate) {
        return new ResourceServicePersistenceProxy(persistenceLayer, delegate);
    }

    /**
     * A bean for the implementation of the SimpleResourceService which is a simple implementation of
     * {@link ResourceService} which extends {@link Service}
     *
     * @return a new instance of SimpleResourceService with a string value dataServiceName retrieved from the relevant profiles yaml
     */
    @Bean("simpleResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "simple", matchIfMissing = true)
    public ResourceService simpleResourceService() {
        return new SimpleResourceService(dataServiceName, resourceServiceConfigProperties.getDefaultType());
    }


    /**
     * A bean for the implementation of the HadoopResourceService which implements {@link ResourceService} used for retrieving resources from Hadoop
     *
     * @param config hadoop configuration
     * @return a {@link ConfiguredHadoopResourceService} used for adding connection details to leaf resources
     * @throws IOException ioexception
     */
    @Bean("hadoopResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "hadoop")
    public HadoopResourceService hadoopResourceService(final org.apache.hadoop.conf.Configuration config) throws IOException {
        return new ConfiguredHadoopResourceService(config);
    }

    /**
     * A bean for the HadoopConfiguration used when creating the hadoopResourceService
     *
     * @return a {@link org.apache.hadoop.conf.Configuration}
     */
    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        return new org.apache.hadoop.conf.Configuration();
    }

    /**
     * Used so that you can create custom mapper by starting with the default and then modifying if needed
     *
     * @return a default ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Override
    @Bean("threadPoolTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(THREAD_POOL);
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
    }

}
