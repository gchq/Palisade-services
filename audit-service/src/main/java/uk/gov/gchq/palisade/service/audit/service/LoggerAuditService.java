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
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * A LoggerAuditService is a simple implementation of an {@link AuditService} that simply constructs a message and logs
 * it using slf4j {@link Logger}.
 * <ul>
 *     <li>Messages are logged at INFO logging level.</li>
 *     <li>Error messages are logged at ERROR logging level.</li>
 * </ul>
 * <p> An example message is: </p>
 * <pre>
 * 'Alice' accessed 'file1' for 'Payroll' and it was processed using 'Age off and visibility filtering'
 * </pre>
 */
public class LoggerAuditService implements AuditService {

    /**
     * The configuration key for property "audit.implementations". This property is
     * used to decide which service implementation Spring will inject.
     *
     * @see ApplicationConfiguration
     */
    public static final String CONFIG_KEY = "logger";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerAuditService.class);
    private static final Map<Class<?>, BiConsumer<Logger, AuditMessage>> DISPATCHER = new HashMap<>();
    private static final String AUDIT_MESSAGE = "AuditMessage: {}";
    private static final String AUDIT_MESSAGE_NULL = "The AuditMessage cannot be null";
    private static final String ERROR_CALLED = "auditErrorMessage from {}, logger is: {}, and request is {}";
    private static final String LOGGER_NULL = "The Logger cannot be null";
    private static final String SUCCESS_CALLED = "auditSuccessMessage from {}, logger is: {}, and request is {}";


    static {
        DISPATCHER.put(AuditSuccessMessage.class, LoggerAuditService::auditSuccessMessage);
        DISPATCHER.put(AuditErrorMessage.class, LoggerAuditService::auditErrorMessage);
    }

    @SuppressWarnings("java:S1312") // Suppress the 'Naming convention for loggers' warning
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
        requireNonNull(logger, LOGGER_NULL);
        requireNonNull(request, AUDIT_MESSAGE_NULL);
        if (request.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.value) || request.getServiceName().equals(ServiceName.DATA_SERVICE.value)) {
            logger.debug(SUCCESS_CALLED, request.getServiceName(), logger, request);
            logger.info(AUDIT_MESSAGE, request);
        } else {
            logger.warn("An AuditSuccessMessage should only be sent by the 'Filtered Resource Service' or the 'Data Service'. Message received from {}",
                    request.getServiceName());
        }
    }

    private static void auditErrorMessage(final Logger logger, final AuditMessage request) {
        requireNonNull(logger, LOGGER_NULL);
        requireNonNull(request, AUDIT_MESSAGE_NULL);
        logger.debug(ERROR_CALLED, request.getServiceName(), logger, request);
        logger.error(AUDIT_MESSAGE, request);
    }

    @Override
    public Boolean audit(final String token, final AuditMessage message) {
        LOGGER.debug("LoggerAuditService received an audit request for token '{}'", token);
        if (message instanceof AuditSuccessMessage) {
            AuditSuccessMessage successMessage = (AuditSuccessMessage) message;
            if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.value) || message.getServiceName().equals(ServiceName.DATA_SERVICE.value)) {
                auditSuccessMessage(auditLogger, successMessage);
                return true;
            }
            auditLogger.warn(
                    "An AuditSuccessMessage should only be sent by the 'Filtered Resource Service' or the 'Data Service'. Message received from {}",
                    message.getServiceName());
            return false;

        } else if (message instanceof AuditErrorMessage) {
            AuditErrorMessage errorMessage = (AuditErrorMessage) message;
            auditErrorMessage(auditLogger, errorMessage);
            return true;
        } else {
            auditLogger.warn("The service {} has created unknown type of AuditMessage for token {}. Request: {}", message.getServiceName(), token, message);
            return false;
        }
    }
}
