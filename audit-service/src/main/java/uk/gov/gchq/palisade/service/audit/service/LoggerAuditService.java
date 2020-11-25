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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * A LoggerAuditService is a simple implementation of an {@link AuditService} that simply constructs a message and logs
 * it using slf4j {@link Logger}. <ul> <li>Messages are logged at INFO logging level.</li> <li>Error messages are logged
 * at ERROR logging level.</li> </ul> <p> An example message is: </p>
 * <pre>
 * 'Alice' accessed 'file1' for 'Payroll' and it was processed using 'Age off and visibility filtering'
 * </pre>
 */
public class LoggerAuditService implements AuditService {
    public static final String CONFIG_KEY = "logger";
    static final String AUDIT_MESSAGE = "AuditMessage: {}";
    static final String AUDIT_MESSAGE_NULL = "The AuditMessage cannot be null";
    static final String ERROR_CALLED = "auditErrorMessage from {}, logger is: {}, and request is {}";
    static final String LOGGER_NULL = "The Logger cannot be null";
    static final String SUCCESS_CALLED = "auditSuccessMessage from {}, logger is: {}, and request is {}";
    private static final Map<Class, BiConsumer<Logger, AuditMessage>> DISPATCHER = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerAuditService.class);


    static {
        DISPATCHER.put(AuditSuccessMessage.class, LoggerAuditService::auditSuccessMessage);
        DISPATCHER.put(AuditErrorMessage.class, LoggerAuditService::auditErrorMessage);
    }

    private final Logger auditLogger;

    /**
     * Create a new LoggerAuditService, which will take in an {@link AuditMessage} and write information from the request to a {@link Logger}
     *
     * @param loggingService the target {@link Logger} to output data to
     */
    public LoggerAuditService(final Logger loggingService) {
        auditLogger = loggingService;
    }

    private static void auditSuccessMessage(final Logger logger, final AuditMessage request) {
        requireNonNull(request, LOGGER_NULL);
        requireNonNull(request, AUDIT_MESSAGE_NULL);
        if (request.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.name()) || request.getServiceName().equals(ServiceName.DATA_SERVICE.name())) {
            logger.debug(SUCCESS_CALLED, request.getServiceName(), logger, request);
            logger.info(AUDIT_MESSAGE, request);
        } else {
            logger.warn("An AuditSuccessMessage should only be sent by the FILTERED_RESOURCE_SERVICE or the DATA_SERVICE. Message received from {}",
                    request.getServiceName());
        }
    }

    private static void auditErrorMessage(final Logger logger, final AuditMessage request) {
        requireNonNull(request, LOGGER_NULL);
        requireNonNull(request, AUDIT_MESSAGE_NULL);
        logger.debug(ERROR_CALLED, request.getServiceName(), logger, request);
        logger.error(AUDIT_MESSAGE, request);
    }

    @Override
    public CompletableFuture<Boolean> audit(final String token, final AuditMessage request) {
        requireNonNull(request, AUDIT_MESSAGE_NULL);
        LOGGER.debug("LoggerAuditService received an audit request for token '{}'", token);
        BiConsumer<Logger, AuditMessage> handler = DISPATCHER.get(request.getClass());
        if (handler != null) {
            handler.accept(auditLogger, request);
        } else {
            // received an AuditMessage derived class that is not defined as a Handler above.
            // need to add handler for this class.
            LOGGER.error("handler == null for {}", request.getClass().getName());
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}
