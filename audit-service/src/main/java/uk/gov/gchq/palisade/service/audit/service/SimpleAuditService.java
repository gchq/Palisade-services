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

package uk.gov.gchq.palisade.service.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A SimpleAuditService is a simple implementation of a {@link AuditService} that keeps user data in the cache service
 */
public class SimpleAuditService implements AuditService {
    public static final String CONFIG_KEY = "simple";
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAuditService.class);

    @Override
    public CompletableFuture<Boolean> audit(final AuditRequest request) {
        LOGGER.info("SimpleAuditService.audit called with {}", request);
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}

