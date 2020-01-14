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
package uk.gov.gchq.palisade.service.audit.config;

import event.logging.impl.DefaultEventLoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.SimpleAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Primary
    @Bean(name = "simple")
    @ConditionalOnProperty(prefix = "audit.implementations", name = SimpleAuditService.CONFIG_KEY)
    public SimpleAuditService auditService() {
        SimpleAuditService simpleUserService = new SimpleAuditService();
        LOGGER.info("Instantiated SimpleAuditService");
        return simpleUserService;
    }

    @Bean(name = "stroom")
    @ConditionalOnProperty(prefix = "audit.implementations", name = StroomAuditService.CONFIG_KEY)
    public StroomAuditService stroomAuditService() {
        LOGGER.info("Instantiated StroomAuditService");
        return new StroomAuditService(new DefaultEventLoggingService());
    }

    @Bean(name = "logger")
    @ConditionalOnProperty(prefix = "audit.implementations", name = LoggerAuditService.CONFIG_KEY)
    public LoggerAuditService loggerAuditService() {
        LOGGER.info("Instantiated LoggerAuditService");
        return new LoggerAuditService(LoggerFactory.getLogger(LoggerAuditService.class));
    }

}
