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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.service.resource.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.resource.common.resource.ConnectionDetail;
import uk.gov.gchq.palisade.service.resource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourceConfiguration;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourceService;
import uk.gov.gchq.palisade.service.resource.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.resource.repository.CompletenessRepository;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ResourceRepository;
import uk.gov.gchq.palisade.service.resource.repository.SerialisedFormatRepository;
import uk.gov.gchq.palisade.service.resource.repository.TypeRepository;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;

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
// Suppress dynamic class loading smell as it's needed for json serialisation
@SuppressWarnings("java:S2658")
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    private static final int THREAD_POOL = 6;
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        // Reflect and add annotated classes as subtypes
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegisterJsonSubType.class));
        scanner.findCandidateComponents("uk.gov.gchq.palisade")
                .forEach((BeanDefinition beanDef) -> {
                    try {
                        Class<?> type = Class.forName(beanDef.getBeanClassName());
                        Class<?> supertype = type.getAnnotation(RegisterJsonSubType.class).value();
                        LOGGER.debug("Registered {} as json subtype of {}", type, supertype);
                        MAPPER.registerSubtypes(type);
                    } catch (ClassNotFoundException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                });
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
     * Used so that you can create custom mapper by starting with the default and then modifying if needed
     *
     * @return a configured object mapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return MAPPER;
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
