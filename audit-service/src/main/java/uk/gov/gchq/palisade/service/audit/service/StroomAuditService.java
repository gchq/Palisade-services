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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

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
    private static final System SYSTEM = new System();
    private static final String EVENT_GENERATOR = "Palisade";
    private static final Logger LOGGER = LoggerFactory.getLogger(StroomAuditService.class);

    static {
        DISPATCHER.put(AuditRequest.RegisterRequestCompleteAuditRequest.class, StroomAuditService::onRegisterRequestComplete);
        DISPATCHER.put(AuditRequest.RegisterRequestExceptionAuditRequest.class, StroomAuditService::onRegisterRequestException);
        DISPATCHER.put(AuditRequest.ReadRequestCompleteAuditRequest.class, StroomAuditService::onReadRequestComplete);
        DISPATCHER.put(AuditRequest.ReadRequestExceptionAuditRequest.class, StroomAuditService::onReadRequestException);
    }

    private final DefaultEventLoggingService eventLogger;
    private final Logger errorLogger;

    /**
     * Create a new StroomAuditService, which will take in an {@link AuditRequest} and produce events from the request to be logged to an {@link event.logging.EventLoggingService}
     *
     * @param eventLoggingService the target {@link event.logging.EventLoggingService} for all created events
     */
    public StroomAuditService(final DefaultEventLoggingService eventLoggingService) {
        errorLogger = LoggerFactory.getLogger(StroomAuditService.class);
        eventLogger = eventLoggingService;
        LOGGER.debug("StroomAuditService called and the defaultEventLoggingService is: {}", eventLoggingService);
    }

    private static void addUserToEvent(final Event event, final UserId user) {
        Event.EventSource eventSource = event.getEventSource();
        User stroomUser = EventLoggingUtil.createUser(user.getId());
        eventSource.setUser(stroomUser);
        LOGGER.debug("addUserToEvent called and the event is: {}, and userId is: {}", event, user);

    }

    private static void addPurposeToEvent(final Event event, final uk.gov.gchq.palisade.Context context) {
        Event.EventDetail eventDetail = event.getEventDetail();
        Purpose purpose = new Purpose();
        purpose.setJustification(context.getPurpose());
        eventDetail.setPurpose(purpose);
        LOGGER.debug("addPurposeToEvent called and the event is: {}, and context is: {}", event, context);
    }

    private static Outcome createOutcome(final boolean success) {
        Outcome outcome = new Outcome();
        outcome.setSuccess(success);
        LOGGER.debug("createOutcome called and the success is: {}, and outcome is: {}", success, outcome);
        return outcome;
    }

    private static Event generateNewGenericEvent(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        LOGGER.debug("generateNewGenericEvent called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, request);
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
        LOGGER.debug("generateNewGenericEvent returned {}", event);
        return event;
    }

    private static void onRegisterRequestComplete(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "AuditRequest$RegisterRequestCompleteAuditRequest cannot be null");
        LOGGER.debug("onRegisterRequestComplete called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, request);
        AuditRequest.RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest = (AuditRequest.RegisterRequestCompleteAuditRequest) request;
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
            LOGGER.debug("onRegisterRequestComplete resources is empty");
            authorisationEventDetail.setTypeId(REGISTER_REQUEST_NO_RESOURCES_TYPE_ID);
            authorisationEventDetail.setDescription(REGISTER_REQUEST_NO_RESOURCES_DESCRIPTION);
            outcome = createOutcome(false);
            outcome.setDescription(REGISTER_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION);
        } else {
            LOGGER.debug("onRegisterRequestComplete resources is not empty");
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
        LOGGER.debug("onRegisterRequestComplete authorisationEvent is {}", authorisationEvent);

    }

    private static void onRegisterRequestException(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "AuditRequest$RegisterRequestExceptionAuditRequest cannot be null");
        LOGGER.debug("onRegisterRequestException called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, request);
        AuditRequest.RegisterRequestExceptionAuditRequest registerRequestExceptionAuditRequest = (AuditRequest.RegisterRequestExceptionAuditRequest) request;
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
        if (ServiceName.USER_SERVICE.name().equalsIgnoreCase(registerRequestExceptionAuditRequest.serviceName)) {
            LOGGER.debug("onRegisterRequestException  registerRequestExceptionAuditRequest is UserService");
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_USER_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_USER_DESCRIPTION);
            outcome.setDescription(REGISTER_REQUEST_EXCEPTION_USER_OUTCOME_DESCRIPTION);
        } else if (ServiceName.RESOURCE_SERVICE.name().equalsIgnoreCase(registerRequestExceptionAuditRequest.serviceName)) {
            LOGGER.debug("onRegisterRequestException  registerRequestExceptionAuditRequest is ResourceService");
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_RESOURCE_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_RESOURCE_DESCRIPTION);
            outcome.setDescription(REGISTER_REQUEST_EXCEPTION_RESOURCE_OUTCOME_DESCRIPTION);
        } else {
            LOGGER.debug("onRegisterRequestException  registerRequestExceptionAuditRequest is not set");
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION);
            outcome.setDescription(registerRequestExceptionAuditRequest.exception.getMessage());
        }
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        exceptionEventDetail.setAuthorise(authorise);
        loggingService.log(exceptionEvent);
        LOGGER.debug("onRegisterRequestException authorisationEvent is {}", exceptionEvent);
    }

    private static void onReadRequestComplete(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "AuditRequest$ReadRequestCompleteAuditRequest cannot be null");
        LOGGER.debug("onReadRequestComplete called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, request);
        AuditRequest.ReadRequestCompleteAuditRequest readRequestCompleteAuditRequest = (AuditRequest.ReadRequestCompleteAuditRequest) request;
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
        LOGGER.debug("onReadRequestComplete returned {}", viewEvent);
    }

    private static void onReadRequestException(final DefaultEventLoggingService loggingService, final AuditRequest request) {
        requireNonNull(request, "AuditRequest$ReadRequestExceptionAuditRequest cannot be null");
        LOGGER.debug("onReadRequestException called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, request);
        AuditRequest.ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest = (AuditRequest.ReadRequestExceptionAuditRequest) request;
        // view request
        Event viewEvent = generateNewGenericEvent(loggingService, readRequestExceptionAuditRequest);
        Event.EventDetail viewEventDetail = new Event.EventDetail();
        viewEvent.setEventDetail(viewEventDetail);
        ObjectOutcome view = new ObjectOutcome();
        viewEventDetail.setView(view);
        Outcome outcome = createOutcome(false);
        view.setOutcome(outcome);
        if (readRequestExceptionAuditRequest.exception.getMessage().startsWith(TOKEN_NOT_FOUND_MESSAGE)) {
            LOGGER.debug("onReadRequestException readRequestExceptionAuditRequest starts with Token not found");
            viewEventDetail.setTypeId(READ_REQUEST_EXCEPTION_TOKEN_TYPE_ID);
            viewEventDetail.setDescription(READ_REQUEST_EXCEPTION_TOKEN_DESCRIPTION);
            outcome.setDescription(READ_REQUEST_EXCEPTION_TOKEN_OUTCOME_DESCRIPTION);
        } else {
            LOGGER.debug("onReadRequestException readRequestExceptionAuditRequest doesnt starts with Token not found");
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
        LOGGER.debug("onReadRequestException returned {}", viewEvent);

    }

    /**
     * @param systemName the name of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService systemName(final String systemName) {
        requireNonNull(systemName, "The system name cannot be null.");
        LOGGER.debug("systemName is {}", systemName);
        SYSTEM.setName(systemName);
        return this;
    }

    @Generated
    public String getSystemName() {
        return SYSTEM.getName();
    }

    @Generated
    public void setSystemName(final String systemName) {
        systemName(systemName);
    }

    /**
     * @param organisation the organisation that the system belongs too
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService organisation(final String organisation) {
        requireNonNull(organisation, "The organisation cannot be null.");
        LOGGER.debug("organisation is {}", organisation);
        SYSTEM.setOrganisation(organisation);
        return this;
    }

    @Generated
    public String getOrganisation() {
        LOGGER.debug("organisation is {}", SYSTEM.getOrganisation());
        return SYSTEM.getOrganisation();
    }

    @Generated
    public void setOrganisation(final String organisation) {
        LOGGER.debug("organisation is {}", organisation);
        organisation(organisation);
    }

    /**
     * @param env the system environment of this deployment, e.g prod, ref, test
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService systemEnv(final String env) {
        requireNonNull(env, "The env cannot be null.");
        LOGGER.debug("systemEnv is {}", env);
        SYSTEM.setEnvironment(env);
        return this;
    }

    @Generated
    public String getSystemEnv() {
        return SYSTEM.getEnvironment();
    }

    @Generated
    public void setSystemEnv(final String systemEnv) {
        systemEnv(systemEnv);
    }

    /**
     * @param description the description of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService systemDescription(final String description) {
        requireNonNull(description, "The description cannot be null.");
        LOGGER.debug("systemDescription is {}", description);
        SYSTEM.setDescription(description);
        return this;
    }

    @Generated
    public String getSystemDescription() {
        return SYSTEM.getDescription();
    }

    @Generated
    public void setSystemDescription(final String description) {
        systemDescription(description);
    }

    /**
     * @param systemVersion the system version of this deployment, v1, v1.0.2, v2, etc
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService systemVersion(final String systemVersion) {
        requireNonNull(systemVersion, "The systemVersion cannot be null.");
        SYSTEM.setVersion(systemVersion);
        return this;
    }

    @Generated
    public String getSystemVersion() {
        return SYSTEM.getVersion();
    }

    @Generated
    public void setSystemVersion(final String systemVersion) {
        systemVersion(systemVersion);
    }

    /**
     * @param systemClassification the classification of the system from which the audit service is receiving audit logs from
     * @return {@link StroomAuditService}
     */
    @Generated
    public StroomAuditService systemClassification(final String systemClassification) {
        requireNonNull(systemClassification, "The systemClassification cannot be null.");
        LOGGER.debug("systemClassification is {}", systemClassification);
        Classification classification = new Classification();
        classification.setText(systemClassification);
        SYSTEM.setClassification(classification);
        return this;
    }

    @Generated
    public String getSystemClassification() {
        LOGGER.debug("systemClassification is {}", SYSTEM.getClassification().getText());
        return SYSTEM.getClassification().getText();
    }

    @Generated
    public void setSystemClassification(final String systemClassification) {
        LOGGER.debug("systemClassification is {}", systemClassification);
        systemClassification(systemClassification);
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

}
