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

package uk.gov.gchq.palisade.service.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class AuditServiceAsyncProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceAsyncProxy.class);
    private final Executor executor;
    private final Map<String, AuditService> services;

    /**
     * Constructor for the {@link AuditServiceAsyncProxy}
     *
     * @param services           a {@link Map} of services to use for this proxy
     * @param executor           the {@link Executor} for the service
     */
    public AuditServiceAsyncProxy(final Map<String, AuditService> services,
                                 final Executor executor) {
        this.services = services;
        this.executor = executor;
    }

    /**
     * Takes the {@link String} token value and an {@link AuditMessage} and audits the information in an {@link AuditService} implementation
     *
     * @param token    the token for the Palisade request.
     * @param message  the message received from another service
     * @return a {@link CompletableFuture} of a {@link Boolean} value.
     */
    public CompletableFuture<List<Boolean>> audit(final String token, final AuditMessage message) {
        LOGGER.debug("Attempting to audit a {} for token {}", message.getClass(), token);
        return CompletableFuture.supplyAsync(() -> services.values().stream()
                .map(auditService -> {
                    if (message instanceof AuditSuccessMessage) {
                        AuditSuccessMessage successMessage = (AuditSuccessMessage) message;
                        if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.name) || message.getServiceName().equals(ServiceName.DATA_SERVICE.name)) {
                            auditService.audit(token, successMessage);
                            return true;
                        } else {
                            LOGGER.warn("An AuditSuccessMessage should only be sent by the FilteredResourceService or the DataService. Message received from {}",
                                    message.getServiceName());
                            return false;
                        }
                    } else if (message instanceof AuditErrorMessage) {
                        AuditErrorMessage errorMessage = (AuditErrorMessage) message;
                        auditService.audit(token, errorMessage);
                        return true;
                    } else {
                        LOGGER.warn("The service {} has created unknown type of AuditMessage for token {}. Request: {}", message.getServiceName(), token, message);
                        return false;
                    }

                })
                .collect(Collectors.toList())
        );
    }
}
