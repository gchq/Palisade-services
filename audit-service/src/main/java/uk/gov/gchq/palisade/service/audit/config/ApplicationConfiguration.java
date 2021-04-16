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
package uk.gov.gchq.palisade.service.audit.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import event.logging.impl.DefaultEventLoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.service.AuditServiceAsyncProxy;
import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.SimpleAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

import java.util.Map;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Primary
    @Bean(name = "simple")
    @ConditionalOnProperty(prefix = "audit.implementations", name = SimpleAuditService.CONFIG_KEY)
    SimpleAuditService auditService() {
        SimpleAuditService simpleUserService = new SimpleAuditService();
        LOGGER.info("Instantiated SimpleAuditService");
        return simpleUserService;
    }

    @Bean(name = "stroom")
    @ConditionalOnProperty(prefix = "audit.implementations", name = StroomAuditService.CONFIG_KEY)
    StroomAuditService stroomAuditService() {
        LOGGER.info("Instantiated StroomAuditService");
        return new StroomAuditService(new DefaultEventLoggingService());
    }

    @Bean(name = "logger")
    @ConditionalOnProperty(prefix = "audit.implementations", name = LoggerAuditService.CONFIG_KEY)
    LoggerAuditService loggerAuditService() {
        LOGGER.info("Instantiated LoggerAuditService");
        return new LoggerAuditService(LoggerFactory.getLogger(LoggerAuditService.class));
    }

    @Bean
    AuditServiceAsyncProxy auditServiceAsyncProxy(final Map<String, AuditService> services) {
        return new AuditServiceAsyncProxy(services);
    }

    /**
     * Used so that you can create custom mapper by starting with the default and then modifying if needed
     *
     * @return a configured object mapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }
}
