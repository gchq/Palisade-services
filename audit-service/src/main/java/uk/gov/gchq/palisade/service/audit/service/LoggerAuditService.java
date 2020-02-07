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
import uk.gov.gchq.palisade.service.audit.request.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestExceptionAuditRequest;

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
    static final String REGISTER_REQUEST_COMPLETE = "RegisterRequestCompleteAuditRequest";
    static final String REGISTER_REQUEST_EXCEPTION = "RegisterRequestExceptionAuditRequest";
    static final String READ_REQUEST_COMPLETE = "ReadRequestCompleteAuditRequest";
    static final String READ_REQUEST_EXCEPTION = "ReadRequestExceptionAuditRequest";
    private static final Map<Class, BiConsumer<Logger, AuditRequest>> DISPATCHER = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerAuditService.class);


    static {
        DISPATCHER.put(RegisterRequestCompleteAuditRequest.class, LoggerAuditService::onRegisterRequestComplete);
        DISPATCHER.put(RegisterRequestExceptionAuditRequest.class, LoggerAuditService::onRegisterRequestException);
        DISPATCHER.put(ReadRequestCompleteAuditRequest.class, LoggerAuditService::onReadRequestComplete);
        DISPATCHER.put(ReadRequestExceptionAuditRequest.class, LoggerAuditService::onReadRequestException);
    }

    private final Logger auditLogger;

    public LoggerAuditService(final Logger loggingService) {
        auditLogger = loggingService;
    }

    private static void onRegisterRequestComplete(final Logger logger, final AuditRequest request) {
        requireNonNull(logger, "Logger cannot be null");
        requireNonNull(request, "RegisterRequestCompleteAuditRequest cannot be null");
        LOGGER.debug("onRegisterRequestComplete called, logger is: {}, and request is {}", logger, request);
        final String msg = String.format("'%s': %s", REGISTER_REQUEST_COMPLETE, request);
        logger.info(msg);
        LOGGER.info("RegisterRequestComplete: {}", msg);
    }

    private static void onRegisterRequestException(final Logger logger, final AuditRequest request) {
        requireNonNull(logger, "Logger cannot be null");
        requireNonNull(request, "RegisterRequestExceptionAuditRequest cannot be null");
        LOGGER.debug("onRegisterRequestException called, logger is: {}, and request is {}", logger, request);
        final String msg = String.format("'%s': %s", REGISTER_REQUEST_EXCEPTION, request);
        logger.error(msg);
        LOGGER.error("RegisterRequestException: {}", msg);
    }

    private static void onReadRequestComplete(final Logger logger, final AuditRequest request) {
        requireNonNull(logger, "Logger cannot be null");
        requireNonNull(request, "ReadRequestCompleteAuditRequest cannot be null");
        LOGGER.debug("onReadRequestComplete called, logger is: {}, and request is {}", logger, request);
        final String msg = String.format("'%s': %s", READ_REQUEST_COMPLETE, request);
        logger.info(msg);
        LOGGER.info("ReadRequestComplete: {}", msg);

    }

    private static void onReadRequestException(final Logger logger, final AuditRequest request) {
        requireNonNull(logger, "Logger cannot be null");
        requireNonNull(request, "ReadRequestExceptionAuditRequest cannot be null");
        LOGGER.debug("onReadRequestException called, logger is: {}, and request is {}", logger, request);
        final String msg = String.format("'%s': %s", READ_REQUEST_EXCEPTION, request);
        logger.error(msg);
        LOGGER.error("ReadRequestException: {}", msg);
    }

    @Override
    public CompletableFuture<Boolean> audit(final AuditRequest request) {
        requireNonNull(request, "The audit request can not be null.");
        BiConsumer<Logger, AuditRequest> handler = DISPATCHER.get(request.getClass());
        if (handler != null) {
            handler.accept(auditLogger, request);
        } else {
            // received an AuditRequest derived class that is not defined as a Handler above.
            // need to add handler for this class.
            LOGGER.error("handler == null for " + request.getClass().getName());
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

}
