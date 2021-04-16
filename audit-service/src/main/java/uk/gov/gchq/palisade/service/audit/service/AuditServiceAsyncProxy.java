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
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * An asynchronous service for processing requests that might be slow.
 */
public class AuditServiceAsyncProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceAsyncProxy.class);
    private final Map<String, AuditService> services;

    /**
     * Constructor for the AuditServiceAsyncProxy
     *
     * @param services a {@link Map} of services to use for this proxy
     */
    public AuditServiceAsyncProxy(final Map<String, AuditService> services) {
        this.services = services;
    }

    /**
     * Takes the {@link String} token value and an {@link AuditMessage} and audits the information in an {@link AuditService} implementation
     *
     * @param token   the token for the Palisade request.
     * @param message the message received from another service
     * @return a {@link CompletableFuture} containing a {@link List} of {@link Boolean} values.
     */
    public CompletableFuture<List<Boolean>> audit(final String token, final AuditMessage message) {
        LOGGER.debug("Attempting to audit an `{}` for token `{}`", message.getClass().getSimpleName(), token);
        return CompletableFuture.supplyAsync(() -> services.values().stream()
                .map((final AuditService auditService) -> {
                    if (message instanceof AuditSuccessMessage) {
                        AuditSuccessMessage successMessage = (AuditSuccessMessage) message;
                        if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.value) || message.getServiceName().equals(ServiceName.DATA_SERVICE.value)) {
                            auditService.audit(token, successMessage);
                            return true;
                        }
                        LOGGER.warn("An AuditSuccessMessage should only be sent by the `Filtered Resource Service` or the `Data Service`. Message received from `{}`",
                                message.getServiceName());
                        return false;
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
