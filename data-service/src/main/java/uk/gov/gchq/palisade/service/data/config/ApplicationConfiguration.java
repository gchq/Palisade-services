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
package uk.gov.gchq.palisade.service.data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.HadoopDataReader;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.service.data.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.data.repository.BackingStore;
import uk.gov.gchq.palisade.service.data.repository.EtcdBackingStore;
import uk.gov.gchq.palisade.service.data.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.data.repository.K8sBackingStore;
import uk.gov.gchq.palisade.service.data.repository.PropertiesBackingStore;
import uk.gov.gchq.palisade.service.data.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.data.request.AuditRequestReceiver;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.CacheService;
import uk.gov.gchq.palisade.service.data.service.PalisadeService;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;
import uk.gov.gchq.palisade.service.data.web.AuditClient;
import uk.gov.gchq.palisade.service.data.web.PalisadeClient;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    public SimpleDataService SimpleDataService(final Map<String, BackingStore> backingStores,
                                               final AuditService auditService,
                                               final PalisadeService palisadeService,
                                               final DataReader dataReader,
                                               final AuditRequestReceiver auditRequestReceiver) {
        return new SimpleDataService(cacheService(backingStores),
                auditService,
                palisadeService,
                dataReader,
                auditRequestReceiver
        );
    }

    @Bean
    public AuditRequestReceiver auditRequestReceiver(final AuditService auditService) {
        return new AuditRequestReceiver(auditService);
    }

    @Bean
    public DataReader dataReader() {
        return new HadoopDataReader();
    }

    @Bean
    public PalisadeService palisadeService(final PalisadeClient palisadeClient) {
        return new PalisadeService(palisadeClient, getAsyncExecutor());
    }

    @Bean
    public AuditService auditService(final AuditClient auditClient) {
        return new AuditService(auditClient, getAsyncExecutor());
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
    public CacheService cacheService(final Map<String, BackingStore> backingStores) {
        return Optional.of(new SimpleCacheService()).stream().peek(cache -> {
            LOGGER.info("Cache backing implementation = {}", Objects.requireNonNull(backingStores.values().stream().findFirst().orElse(null)).getClass().getSimpleName());
            cache.backingStore(backingStores.values().stream().findFirst().orElse(null));
        }).findFirst().orElse(null);
    }

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
