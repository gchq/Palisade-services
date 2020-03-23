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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.HadoopDataReader;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.service.data.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.PalisadeService;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;
import uk.gov.gchq.palisade.service.data.web.AuditClient;
import uk.gov.gchq.palisade.service.data.web.PalisadeClient;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "web")
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration();
    }

    @Bean
    public SimpleDataService simpleDataService(final AuditService auditService,
                                               final PalisadeService palisadeService,
                                               final DataReader dataReader) {
        return new SimpleDataService(auditService, palisadeService, dataReader);
    }

    @Bean
    public DataReader hadoopDataReader() throws IOException {
        return new HadoopDataReader();
    }

    @Bean
    public PalisadeService palisadeService(final PalisadeClient palisadeClient, final ClientConfiguration clientConfig) {
        Supplier<URI> palisadeUriSupplier = () -> clientConfig
                .getClientUri("palisade-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'palisade-service' - see 'web.client' properties or discovery service registration"));
        return new PalisadeService(palisadeClient, palisadeUriSupplier, getAsyncExecutor());
    }

    @Bean
    public AuditService auditService(final AuditClient auditClient, final ClientConfiguration clientConfig) {
        Supplier<URI> auditUriSupplier = () -> clientConfig
                .getClientUri("audit-service")
                .orElseThrow(() -> new RuntimeException("Cannot find any instance of 'audit-service' - see 'web.client' properties or discovery service registration"));
        return new AuditService(auditClient, auditUriSupplier, getAsyncExecutor());
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

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor(getTaskExecutor());
            }
        };
    }

    @Bean(name = "concurrentTaskExecutor")
    public ConcurrentTaskExecutor getTaskExecutor() {
        return new ConcurrentTaskExecutor(this.getAsyncExecutor());
    }
}
