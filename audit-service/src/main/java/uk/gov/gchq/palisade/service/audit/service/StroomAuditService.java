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

import event.logging.Activity;
import event.logging.Authorisation;
import event.logging.Classification;
import event.logging.Data;
import event.logging.Event;
import event.logging.ObjectOutcome;
import event.logging.Outcome;
import event.logging.Purpose;
import event.logging.System;
import event.logging.User;
import event.logging.impl.DefaultEventLoggingService;
import event.logging.util.DeviceUtil;
import event.logging.util.EventLoggingUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uk.gov.gchq.palisade.exception.NoConfigException;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ServiceState;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestExceptionAuditRequest;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * A StroomAuditService is a simple implementation of an {@link AuditService} that simply constructs a message and logs
 * it using the Stroom EventLoggingService.
 */
public class StroomAuditService implements AuditService {
    public static final String CONFIG_KEY = "stroom";
    static final String REGISTER_REQUEST_NO_RESOURCES_TYPE_ID = "REGISTER_REQUEST_NO_RESOURCES";
    static final String REGISTER_REQUEST_NO_RESOURCES_DESCRIPTION = "Audits the fact that the user requested access to some resources however they do not have permission to access any of those resources.";
    static final String REGISTER_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION = "The user does not have permission to access any of those resources.";
    private static final System SYSTEM = new System();
    private static final String EVENT_GENERATOR = "Palisade";
    static final String REGISTER_REQUEST_COMPLETED_TYPE_ID = "REGISTER_REQUEST_COMPLETED";
    static final String REGISTER_REQUEST_COMPLETED_DESCRIPTION = "Audits the fact that this request for data has been approved and these are the resources they have been given course grain approval to query.";
    static final String REGISTER_REQUEST_EXCEPTION_USER_TYPE_ID = "REGISTER_REQUEST_EXCEPTION_USER";
    static final String REGISTER_REQUEST_EXCEPTION_USER_DESCRIPTION = "Audits the fact that the user could not be authenticated by the system and therefore the request has been denied.";
    static final String REGISTER_REQUEST_EXCEPTION_USER_OUTCOME_DESCRIPTION = "The user could not be authenticated by the system.";
    static final String REGISTER_REQUEST_EXCEPTION_RESOURCE_TYPE_ID = "REGISTER_REQUEST_EXCEPTION_RESOURCE";
    static final String REGISTER_REQUEST_EXCEPTION_RESOURCE_DESCRIPTION = "Audits the fact that the supplied resource id could not be resolved and therefore the request has been denied.";
    static final String REGISTER_REQUEST_EXCEPTION_RESOURCE_OUTCOME_DESCRIPTION = "The supplied resource id could not be resolved.";
    static final String REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID = "REGISTER_REQUEST_EXCEPTION_OTHER";
    static final String REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION = "Audits the fact that for some reason the request has thrown an exception and therefore the request has been denied";
    static final String READ_REQUEST_COMPLETED_TYPE_ID = "READ_REQUEST_COMPLETED";
    static final String READ_REQUEST_COMPLETED_DESCRIPTION = "Audits the fact that a user has finished reading a specific data resource.";
    static final String READ_REQUEST_EXCEPTION_TOKEN_TYPE_ID = "READ_REQUEST_EXCEPTION_TOKEN";
    static final String READ_REQUEST_EXCEPTION_TOKEN_DESCRIPTION = "Audits the fact that the provided token is invalid, probably because it the request wasn't registered first and therefore the request has been denied.";
    static final String READ_REQUEST_EXCEPTION_TOKEN_OUTCOME_DESCRIPTION = "The provided token is invalid.";
    static final String READ_REQUEST_EXCEPTION_OTHER_TYPE_ID = "READ_REQUEST_EXCEPTION_OTHER";
    static final String READ_REQUEST_EXCEPTION_OTHER_DESCRIPTION = "Audits the fact that an exception was thrown when trying to provide the data to the user.";
    static final String TOKEN_NOT_FOUND_MESSAGE = "User's request was not in the cache: ";
    private static final Map<Class, BiConsumer<DefaultEventLoggingService, AuditRequest>> DISPATCHER = new HashMap<>();

    static {
        DISPATCHER.put(RegisterRequestCompleteAuditRequest.class, StroomAuditService::onRegisterRequestComplete);
        DISPATCHER.put(RegisterRequestExceptionAuditRequest.class, StroomAuditService::onRegisterRequestException);
        DISPATCHER.put(ReadRequestCompleteAuditRequest.class, StroomAuditService::onReadRequestComplete);
        DISPATCHER.put(ReadRequestExceptionAuditRequest.class, StroomAuditService::onReadRequestException);
    }

    private final DefaultEventLoggingService eventLogger;
    private final Logger errorLogger;

    /**
     * @param systemName the name of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    public StroomAuditService systemName(final String systemName) {
        requireNonNull(systemName, "The system name cannot be null.");
        SYSTEM.setName(systemName);
        return this;
    }

    public void setSystemName(final String systemName) {
        systemName(systemName);
    }

    public String getSystemName() {
        return SYSTEM.getName();
    }

    /**
     * @param organisation the organisation that the system belongs too
     * @return {@link StroomAuditService}
     */
    public StroomAuditService organisation(final String organisation) {
        requireNonNull(organisation, "The organisation cannot be null.");
        SYSTEM.setOrganisation(organisation);
        return this;
    }

    public void setOrganisation(final String organisation) {
        organisation(organisation);
    }

    public String getOrganisation() {
        return SYSTEM.getOrganisation();
    }

    /**
     * @param env the system environment of this deployment, e.g prod, ref, test
     * @return {@link StroomAuditService}
     */
    public StroomAuditService systemEnv(final String env) {
        requireNonNull(env, "The env cannot be null.");
        SYSTEM.setEnvironment(env);
        return this;
    }

    public void setSystemEnv(final String systemEnv) {
        systemEnv(systemEnv);
    }

    public String getSystemEnv() {
        return SYSTEM.getEnvironment();
    }

    /**
     * @param description the description of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    public StroomAuditService systemDescription(final String description) {
        requireNonNull(description, "The description cannot be null.");
        SYSTEM.setDescription(description);
        return this;
    }

    public void setSystemDescription(final String description) {
        systemDescription(description);
    }

    public String getSystemDescription() {
        return SYSTEM.getDescription();
    }

    /**
     * @param systemVersion the system version of this deployment, v1, v1.0.2, v2, etc
     * @return {@link StroomAuditService}
     */
    public StroomAuditService systemVersion(final String systemVersion) {
        requireNonNull(systemVersion, "The systemVersion cannot be null.");
        SYSTEM.setVersion(systemVersion);
        return this;
    }

    public void setSystemVersion(final String systemVersion) {
        systemVersion(systemVersion);
    }

    public String getSystemVersion() {
        return SYSTEM.getVersion();
    }

    /**
     * @param systemClassification the classification of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    public StroomAuditService systemClassification(final String systemClassification) {
        requireNonNull(systemClassification, "The systemClassification cannot be null.");
        Classification classification = new Classification();
        classification.setText(systemClassification);
        SYSTEM.setClassification(classification);
        return this;
    }

    public void setSystemClassification(final String systemClassification) {
        systemClassification(systemClassification);
    }

    public String getSystemClassification() {
        return SYSTEM.getClassification().getText();
    }

    public StroomAuditService(final DefaultEventLoggingService eventLoggingService) {
        errorLogger = LogManager.getLogger(StroomAuditService.class);
        eventLogger = eventLoggingService;
    }

    private static void addUserToEvent(final Event event, final uk.gov.gchq.palisade.UserId user) {
        Event.EventSource eventSource = event.getEventSource();
        User stroomUser = EventLoggingUtil.createUser(user.getId());
        eventSource.setUser(stroomUser);
    }

    private static void addPurposeToEvent(final Event event, final uk.gov.gchq.palisade.Context context) {
        Event.EventDetail eventDetail = event.getEventDetail();
        Purpose purpose = new Purpose();
        purpose.setJustification(context.getPurpose());
        eventDetail.setPurpose(purpose);
    }

    private static Outcome createOutcome(final boolean success) {
        Outcome outcome = new Outcome();
        outcome.setSuccess(success);
        return outcome;
    }

    private static Event generateNewGenericEvent(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        Event event = loggingService.createEvent();
        // set the event time
        Event.EventTime eventTime = EventLoggingUtil.createEventTime(Date.from(request.timestamp.toInstant()));
        event.setEventTime(eventTime);
        // set the event chain
        Event.EventChain eventChain = new Event.EventChain();
        Activity parent = new Activity();
        parent.setId(request.getOriginalRequestId().getId());
        Activity activity = new Activity();
        activity.setParent(parent);
        activity.setId(request.getId().getId());
        eventChain.setActivity(activity);
        event.setEventChain(eventChain);
        // set the event source
        Event.EventSource eventSource = new Event.EventSource();
        eventSource.setSystem(SYSTEM);
        eventSource.setGenerator(EVENT_GENERATOR);
        eventSource.setDevice(DeviceUtil.createDevice(request.serverHostname, request.serverIp));
        event.setEventSource(eventSource);
        return event;
    }

    private static void onRegisterRequestComplete(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "RegisterRequestCompleteAuditRequest cannot be null");
        RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest = (RegisterRequestCompleteAuditRequest) request;
        Event authorisationEvent = generateNewGenericEvent(loggingService, registerRequestCompleteAuditRequest);
        Event.EventDetail authorisationEventDetail = new Event.EventDetail();
        authorisationEvent.setEventDetail(authorisationEventDetail);
        // log the user
        addUserToEvent(authorisationEvent, registerRequestCompleteAuditRequest.user.getUserId());
        // log the purpose that was supplied with the request
        addPurposeToEvent(authorisationEvent, registerRequestCompleteAuditRequest.context);
        // log the list of resources
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        Outcome outcome;
        // if no files then authorisation request failure
        Set<LeafResource> resources = registerRequestCompleteAuditRequest.leafResources;
        if (resources.isEmpty()) {
            authorisationEventDetail.setTypeId(REGISTER_REQUEST_NO_RESOURCES_TYPE_ID);
            authorisationEventDetail.setDescription(REGISTER_REQUEST_NO_RESOURCES_DESCRIPTION);
            outcome = createOutcome(false);
            outcome.setDescription(REGISTER_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION);
        } else {
            authorisationEventDetail.setTypeId(REGISTER_REQUEST_COMPLETED_TYPE_ID);
            authorisationEventDetail.setDescription(REGISTER_REQUEST_COMPLETED_DESCRIPTION);
            for (LeafResource resource : resources) {
                event.logging.Object stroomResource = new event.logging.Object();
                stroomResource.setId(resource.getId());
                stroomResource.setType(resource.getType());
                authorise.getObjects().add(stroomResource);
            }
            outcome = createOutcome(true);
        }
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        authorisationEventDetail.setAuthorise(authorise);
        loggingService.log(authorisationEvent);
    }

    private static void onRegisterRequestException(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "RegisterRequestExceptionAuditRequest cannot be null");
        RegisterRequestExceptionAuditRequest registerRequestExceptionAuditRequest = (RegisterRequestExceptionAuditRequest) request;
        // authorisation exception
        Event exceptionEvent = generateNewGenericEvent(loggingService, registerRequestExceptionAuditRequest);
        Event.EventDetail exceptionEventDetail = new Event.EventDetail();
        exceptionEvent.setEventDetail(exceptionEventDetail);
        // log the user
        addUserToEvent(exceptionEvent, registerRequestExceptionAuditRequest.userId);
        // log the purpose that was supplied with the request
        addPurposeToEvent(exceptionEvent, registerRequestExceptionAuditRequest.context);
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        // log the resource
        event.logging.Object stroomResource = new event.logging.Object();
        stroomResource.setId(registerRequestExceptionAuditRequest.resourceId);
        authorise.getObjects().add(stroomResource);
        Outcome outcome = createOutcome(false);
        if (registerRequestExceptionAuditRequest.serviceClass.getSimpleName().equals("UserService")) {
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_USER_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_USER_DESCRIPTION);
            outcome.setDescription(REGISTER_REQUEST_EXCEPTION_USER_OUTCOME_DESCRIPTION);
        } else if (registerRequestExceptionAuditRequest.serviceClass.getSimpleName().equals("ResourceService")) {
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_RESOURCE_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_RESOURCE_DESCRIPTION);
            outcome.setDescription(REGISTER_REQUEST_EXCEPTION_RESOURCE_OUTCOME_DESCRIPTION);
        } else {
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION);
            outcome.setDescription(registerRequestExceptionAuditRequest.exception.getMessage());
        }
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        exceptionEventDetail.setAuthorise(authorise);
        loggingService.log(exceptionEvent);
    }

    private static void onReadRequestComplete(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "ReadRequestCompleteAuditRequest cannot be null");
        ReadRequestCompleteAuditRequest readRequestCompleteAuditRequest = (ReadRequestCompleteAuditRequest) request;
        // view request
        Event viewEvent = generateNewGenericEvent(loggingService, readRequestCompleteAuditRequest);
        Event.EventDetail viewEventDetail = new Event.EventDetail();
        viewEvent.setEventDetail(viewEventDetail);
        viewEventDetail.setTypeId(READ_REQUEST_COMPLETED_TYPE_ID);
        viewEventDetail.setDescription(READ_REQUEST_COMPLETED_DESCRIPTION);
        // log the user
        addUserToEvent(viewEvent, readRequestCompleteAuditRequest.user.getUserId());
        // log the purpose that was supplied with the request
        addPurposeToEvent(viewEvent, readRequestCompleteAuditRequest.context);
        // log event outcome
        ObjectOutcome view = new ObjectOutcome();
        viewEventDetail.setView(view);
        view.setOutcome(createOutcome(true));
        // set the number of records returned
        Data resultsReturned = new Data();
        resultsReturned.setName("Number of records returned");
        resultsReturned.setValue(String.valueOf(readRequestCompleteAuditRequest.numberOfRecordsReturned));
        view.getData().add(resultsReturned);
        Data resultsProcessed = new Data();
        resultsProcessed.setName("Number of records processed");
        resultsProcessed.setValue(String.valueOf(readRequestCompleteAuditRequest.numberOfRecordsProcessed));
        view.getData().add(resultsProcessed);
        Data rulesApplied = new Data();
        rulesApplied.setName("Rules applied");
        rulesApplied.setValue(String.valueOf(readRequestCompleteAuditRequest.rulesApplied.getMessage()));
        view.getData().add(rulesApplied);
        // set the resource that those records were read from
        event.logging.Object resource = new event.logging.Object();
        resource.setId(readRequestCompleteAuditRequest.leafResource.getId());
        resource.setType(readRequestCompleteAuditRequest.leafResource.getType());
        view.getObjects().add(resource);
        loggingService.log(viewEvent);
    }

    private static void onReadRequestException(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "ReadRequestExceptionAuditRequest cannot be null");
        ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest = (ReadRequestExceptionAuditRequest) request;
        // view request
        Event viewEvent = generateNewGenericEvent(loggingService, readRequestExceptionAuditRequest);
        Event.EventDetail viewEventDetail = new Event.EventDetail();
        viewEvent.setEventDetail(viewEventDetail);
        ObjectOutcome view = new ObjectOutcome();
        viewEventDetail.setView(view);
        Outcome outcome = createOutcome(false);
        view.setOutcome(outcome);
        if (readRequestExceptionAuditRequest.exception.getMessage().startsWith(TOKEN_NOT_FOUND_MESSAGE)) {
            viewEventDetail.setTypeId(READ_REQUEST_EXCEPTION_TOKEN_TYPE_ID);
            viewEventDetail.setDescription(READ_REQUEST_EXCEPTION_TOKEN_DESCRIPTION);
            outcome.setDescription(READ_REQUEST_EXCEPTION_TOKEN_OUTCOME_DESCRIPTION);
        } else {
            viewEventDetail.setTypeId(READ_REQUEST_EXCEPTION_OTHER_TYPE_ID);
            viewEventDetail.setDescription(READ_REQUEST_EXCEPTION_OTHER_DESCRIPTION);
            outcome.setDescription(readRequestExceptionAuditRequest.exception.getMessage());
        }
        // set the resource that those records were read from
        event.logging.Object resource = new event.logging.Object();
        resource.setId(readRequestExceptionAuditRequest.leafResource.getId());
        resource.setType(readRequestExceptionAuditRequest.leafResource.getType());
        view.getObjects().add(resource);
        // set the token used for this read request
        Data token = new Data();
        token.setName("token");
        token.setValue(readRequestExceptionAuditRequest.token);
        view.getData().add(token);
        loggingService.log(viewEvent);
    }

    @Override
    public CompletableFuture<Boolean> audit(final AuditRequest request) {
        requireNonNull(request, "The audit request can not be null.");
        BiConsumer<DefaultEventLoggingService, AuditRequest> handler = DISPATCHER.get(request.getClass());
        if (handler != null) {
            handler.accept(eventLogger, request);
        } else {
            // received an AuditRequest derived class that is not defined as a Handler above.
            // need to add handler for this class.
            errorLogger.error("handler == null for " + request.getClass().getName());

        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @Override
    public void recordCurrentConfigTo(final ServiceState config) {
        requireNonNull(config, "config");
        config.put(AuditService.class.getTypeName(), getClass().getTypeName());
        errorLogger.debug("Wrote configuration data: no-op");
    }

    @Override
    public void applyConfigFrom(final ServiceState config) throws NoConfigException {
        requireNonNull(config, "config");
        errorLogger.debug("Read configuration data: no-op");
    }
}
