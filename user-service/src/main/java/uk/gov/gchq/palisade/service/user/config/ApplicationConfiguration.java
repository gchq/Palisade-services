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
package uk.gov.gchq.palisade.service.user.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.service.user.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.common.user.UserConfiguration;
import uk.gov.gchq.palisade.service.user.common.user.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.user.common.user.UserService;
import uk.gov.gchq.palisade.service.user.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.user.service.NullUserService;
import uk.gov.gchq.palisade.service.user.service.UserServiceAsyncProxy;
import uk.gov.gchq.palisade.service.user.service.UserServiceCachingProxy;

import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
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
     * A container for a number of {@link StdUserPrepopulationFactory} builders used for creating {@link User}s
     * These users will be used for pre-populating the {@link UserService}
     *
     * @return a standard {@link UserConfiguration} containing a list of {@link UserPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdUserConfiguration userConfiguration() {
        return new StdUserConfiguration();
    }

    /**
     * A factory for {@link User} objects, using a userId, a list of authorizations and a list of roles
     *
     * @return a standard {@link UserPrepopulationFactory} capable of building a {@link User} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "userProvider", havingValue = "std")
    public StdUserPrepopulationFactory userPrepopulationFactory() {
        return new StdUserPrepopulationFactory();
    }

    /**
     * A bean to instantiate a {@link UserService} implementation
     *
     * @param userService a {@link UserService}
     * @return an instance of the {@link UserServiceAsyncProxy}
     */
    @Bean
    public UserServiceCachingProxy cacheableUserServiceProxy(final UserService userService) {
        LOGGER.info("Instantiated UserServiceCachingProxy with {}", userService.getClass().getName());
        return new UserServiceCachingProxy(userService);
    }

    /**
     * A bean for the creation of the {@link UserServiceAsyncProxy}
     *
     * @param service  the {@link UserService} implementation
     * @param executor an async {@link Executor}
     * @return an instance of the {@link UserServiceCachingProxy}
     */
    @Bean
    public UserServiceAsyncProxy asyncUserServiceProxy(final UserServiceCachingProxy service,
                                                       final @Qualifier("threadPoolTaskExecutor") Executor executor) {
        LOGGER.info("Instantiated AsyncUserServiceProxy with {}", service.getClass());
        return new UserServiceAsyncProxy(service, executor);
    }

    /**
     * The simplest implementation of a user service, allows unit tests and small services to use a lightweight user service
     *
     * @return an instance of the {@link NullUserService}
     */
    @Bean
    @ConditionalOnProperty(prefix = "user-service", name = "service", havingValue = "null-user-service", matchIfMissing = true)
    public UserService nullUserService() {
        LOGGER.info("Instantiated NullUserService");
        return new NullUserService();
    }

    /**
     * ObjectMapper used in serialising and deserialising
     *
     * @return an instance of the ObjectMapper
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
