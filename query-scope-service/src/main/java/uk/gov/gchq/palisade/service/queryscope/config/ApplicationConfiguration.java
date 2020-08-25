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

package uk.gov.gchq.palisade.service.queryscope.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.queryscope.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.queryscope.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.queryscope.service.AuditService;
import uk.gov.gchq.palisade.service.queryscope.service.SimpleQueryScopeService;

@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    public JpaPersistenceLayer persistenceLayer(final AuthorisedRequestsRepository authorisedRequestsRepository) {
        return new JpaPersistenceLayer(authorisedRequestsRepository);
    }

    @Bean
    public SimpleQueryScopeService simpleQueryScopeService(final JpaPersistenceLayer persistenceLayer) {
        return new SimpleQueryScopeService(persistenceLayer);
    }

    @Bean
    public AuditService nullAuditService() {
        return (token, message) -> LOGGER.warn("This is the null audit service - it does nothing");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
