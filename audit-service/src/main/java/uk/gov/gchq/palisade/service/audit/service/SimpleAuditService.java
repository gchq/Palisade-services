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

package uk.gov.gchq.palisade.service.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;

/**
 * A SimpleAuditService is a simple implementation of a {@link AuditService} that keeps user data in the cache service
 */
public class SimpleAuditService implements AuditService {

    /**
     * The configuration key for property "audit.implementations". This property is
     * used to decide which service implementation Spring will inject.
     *
     * @see ApplicationConfiguration
     */
    public static final String CONFIG_KEY = "simple";

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAuditService.class);

    @Override
    public Boolean audit(final String token, final AuditMessage request) {
        LOGGER.info("SimpleAuditService.audit called for token '{}' with request '{}'", token, request);
        return true;
    }
}

