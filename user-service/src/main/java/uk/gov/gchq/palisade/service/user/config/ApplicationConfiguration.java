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
package uk.gov.gchq.palisade.service.user.config;

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
import uk.gov.gchq.palisade.service.user.repository.BackingStore;
import uk.gov.gchq.palisade.service.user.repository.EtcdBackingStore;
import uk.gov.gchq.palisade.service.user.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.user.repository.K8sBackingStore;
import uk.gov.gchq.palisade.service.user.repository.PropertiesBackingStore;
import uk.gov.gchq.palisade.service.user.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.user.service.CacheService;
import uk.gov.gchq.palisade.service.user.service.SimpleUserService;
import uk.gov.gchq.palisade.service.user.web.ServiceInstanceRestController;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static java.util.stream.Collectors.toList;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties(CacheConfiguration.class)
public class ApplicationConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    public CacheConfiguration cacheConfiguration() {
        return new CacheConfiguration();
    }

    @Bean
    public SimpleUserService userService(final CacheService cacheService) {
        SimpleUserService simpleUserService = new SimpleUserService(cacheService);
        LOGGER.info("Instantiated SimpleUserService");
        return simpleUserService;
    }

    @Bean(name = "hashmap")
    @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "hashmap", matchIfMissing = true)
    public HashMapBackingStore hashMapBackingStore() {
        return new HashMapBackingStore();
    }

    @Bean(name = "k8s")
    @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "k8s")
    public K8sBackingStore k8sBackingStore() {
        return new K8sBackingStore();
    }

    @Bean(name = "props")
    @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "props")
    public PropertiesBackingStore propertiesBackingStore() {
        return new PropertiesBackingStore(Optional.ofNullable(cacheConfiguration().getProps()).orElse("cache.properties"));
    }

    @Bean(name = "etcd")
    @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "etcd")
    public EtcdBackingStore etcdBackingStore() {
        return new EtcdBackingStore(cacheConfiguration().getEtcd().stream().map(URI::create).collect(toList()));
    }

    @Bean
    public CacheService cacheService(final Set<BackingStore> backingStores) {
        return backingStores.stream()
                .map(x -> new SimpleCacheService().backingStore(x))
                .peek(x -> LOGGER.info("Created candidate cache service {}", x))
                .findAny().orElseThrow(() -> {
                    LOGGER.error("No backing store provided and no default found");
                    return new NullPointerException();
                });
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Bean(name = "eureka-client")
    @ConditionalOnProperty(prefix = "eureka.client", name = "enabled")
    public ServiceInstanceRestController eurekaClient() {
        ServiceInstanceRestController serviceInstanceRestController = new ServiceInstanceRestController();
        LOGGER.info("Instantiated eurekaClient");
        return serviceInstanceRestController;
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
