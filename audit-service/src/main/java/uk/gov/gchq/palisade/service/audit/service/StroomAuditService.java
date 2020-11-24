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

import com.fasterxml.jackson.core.JsonProcessingException;
import event.logging.Activity;
import event.logging.Authorisation;
import event.logging.Classification;
import event.logging.Event;
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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.sql.Date;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A StroomAuditService is a simple implementation of an {@link AuditService} that simply constructs a message and logs
 * it using the Stroom EventLoggingService.
 */
public class StroomAuditService implements AuditService {
    public static final String CONFIG_KEY = "stroom";
    static final String AUDIT_ERROR_RESOURCE_EXCEPTION_ID = "NO_SUCH_FILE_EXCEPTION";
    static final String AUDIT_ERROR_RESOURCE_EXCEPTION_DESCRIPTION = "Audits the fact that the supplied resource id could not be resolved and therefore the request has been denied.";
    static final String AUDIT_ERROR_RESOURCE_EXCEPTION_OUTCOME = "The supplied resource id could not be resolved.";
    static final String AUDIT_ERROR_USER_EXCEPTION_ID = "NO_SUCH_USER_EXCEPTION";
    static final String AUDIT_ERROR_USER_EXCEPTION_DESCRIPTION = "Audits the fact that the user could not be authenticated by the system and therefore the request has been denied.";
    static final String AUDIT_ERROR_USER_EXCEPTION_OUTCOME = "The user could not be authenticated by the system.";
    static final String AUDIT_MESSAGE_NULL = "The AuditMessage cannot be null";
    static final String AUDIT_SUCCESS_READ_ID = "READ_REQUEST_COMPLETED";
    static final String AUDIT_SUCCESS_READ_DESCRIPTION = "Audits the fact that the read request for the data has been completed";
    static final String AUDIT_SUCCESS_REQUEST_ID = "REGISTER_REQUEST_COMPLETED";
    static final String AUDIT_SUCCESS_REQUEST_DESCRIPTION = "Audits the fact that this request for data has been approved and these are the resources they have been given course grain approval to query";
    static final String AUDIT_SUCCESS_REQUEST_NO_RESOURCES_TYPE_ID = "REGISTER_REQUEST_NO_RESOURCES";
    static final String AUDIT_SUCCESS_REQUEST_NO_RESOURCES_DESCRIPTION = "Audits the fact that the user requested access to some resources however they do not have permission to access any of those resources.";
    static final String AUDIT_SUCCESS_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION = "The user does not have permission to access any of those resources.";
    static final String CONTEXT = "context";
    static final String ERROR_MESSAGE_FROM = "AuditErrorMessage received from {}";
    static final String ORGANISATION = "organisation is {}";
    static final String RESOURCE_CREATED = "Resource created from leafResourceId";
    static final String REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID = "REGISTER_REQUEST_EXCEPTION_OTHER";
    static final String REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION = "Audits the fact that for some reason the request has thrown an exception and therefore the request has been denied";
    static final String SYSTEM_CLASSIFICATION = "systemClassification is {}";
    static final String TOKEN_NOT_FOUND_MESSAGE = "User's request was not in the cache: ";
    private static final System SYSTEM = new System();
    private static final String EVENT_GENERATOR = "Palisade";
    private static final Logger LOGGER = LoggerFactory.getLogger(StroomAuditService.class);
    private static final String JSON_SERIALISING_ERROR = "An error occurred while deseriaising the {}";

    private final DefaultEventLoggingService eventLogger;
    private final Logger errorLogger;

    /**
     * Create a new StroomAuditService, which will take in an {@link AuditMessage} and produce events from the
     * request to be logged to an {@link event.logging.EventLoggingService}
     *
     * @param eventLoggingService the target {@link event.logging.EventLoggingService} for all created events
     */
    public StroomAuditService(final DefaultEventLoggingService eventLoggingService) {
        errorLogger = LoggerFactory.getLogger(StroomAuditService.class);
        eventLogger = eventLoggingService;
        LOGGER.debug("StroomAuditService called and the defaultEventLoggingService is: {}", eventLoggingService);
    }

    private static void addUserToEvent(final Event event, final String user) {
        Event.EventSource eventSource = event.getEventSource();
        User stroomUser = EventLoggingUtil.createUser(user);
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

    private static Event generateNewGenericEvent(final String token, final DefaultEventLoggingService loggingService, final AuditMessage request) {
        LOGGER.debug("generateNewGenericEvent called and the DefaultEventLoggingService is: {}, and AuditMessage is: {}", loggingService, request);
        Event event = loggingService.createEvent();
        // set the event time
        Event.EventTime eventTime = EventLoggingUtil.createEventTime(Date.valueOf(request.getTimestamp()));
        event.setEventTime(eventTime);
        // set the event chain
        Event.EventChain eventChain = new Event.EventChain();
        Activity activity = new Activity();
        activity.setId(token);
        eventChain.setActivity(activity);
        event.setEventChain(eventChain);
        // set the event source
        Event.EventSource eventSource = new Event.EventSource();
        eventSource.setSystem(SYSTEM);
        eventSource.setGenerator(EVENT_GENERATOR);
        eventSource.setDevice(DeviceUtil.createDevice(request.getServerHostname(), request.getServerIP()));
        event.setEventSource(eventSource);
        LOGGER.debug("generateNewGenericEvent returned {}", event);
        return event;
    }

    private static void auditSuccessMessage(final String token, final DefaultEventLoggingService loggingService, final AuditSuccessMessage message) {
        requireNonNull(message, AUDIT_MESSAGE_NULL);
        LOGGER.debug("auditRequestSuccessMessage called and the DefaultEventLoggingService is: {}, and AuditSuccessMessage is: {}", loggingService, message);
        Event authorisationEvent = generateNewGenericEvent(token, loggingService, message);
        Event.EventDetail authorisationEventDetail = new Event.EventDetail();
        authorisationEvent.setEventDetail(authorisationEventDetail);
        // log the user
        addUserToEvent(authorisationEvent, message.getUserId());
        // log the purpose that was supplied with the request
        try {
            addPurposeToEvent(authorisationEvent, message.getContext());
        } catch (JsonProcessingException ex) {
            LOGGER.error(JSON_SERIALISING_ERROR, CONTEXT);
        }
        // log the list of resources
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        Outcome outcome;
        // if no files then authorisation request failure
        LeafResource resource = (LeafResource) ResourceBuilder.create(message.getLeafResourceId());
        if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.name())) {
            if (message.getLeafResourceId().isEmpty()) {
                LOGGER.debug("The leafResourceId field is empty therefore a resource could not be created");
                authorisationEventDetail.setTypeId(AUDIT_SUCCESS_REQUEST_NO_RESOURCES_TYPE_ID);
                authorisationEventDetail.setDescription(AUDIT_SUCCESS_REQUEST_NO_RESOURCES_DESCRIPTION);
                outcome = createOutcome(false);
                outcome.setDescription(AUDIT_SUCCESS_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION);
            } else {
                LOGGER.debug(RESOURCE_CREATED);
                authorisationEventDetail.setTypeId(AUDIT_SUCCESS_REQUEST_ID);
                authorisationEventDetail.setDescription(AUDIT_SUCCESS_REQUEST_DESCRIPTION);
                event.logging.Object stroomResource = new event.logging.Object();
                stroomResource.setId(resource.getId());
                stroomResource.setType(resource.getType());
                authorise.getObjects().add(stroomResource);
                outcome = createOutcome(true);
            }
            authorise.setOutcome(outcome);
            authorise.setAction(Authorisation.REQUEST);
            authorisationEventDetail.setAuthorise(authorise);
            loggingService.log(authorisationEvent);
            LOGGER.debug("RegisterRequest authorisationEvent is {}", authorisationEvent);
        } else if (message.getServiceName().equals(ServiceName.DATA_SERVICE.name())) {
            LOGGER.debug(RESOURCE_CREATED);
            authorisationEventDetail.setTypeId(AUDIT_SUCCESS_READ_ID);
            authorisationEventDetail.setDescription(AUDIT_SUCCESS_READ_DESCRIPTION);
            event.logging.Object stroomResource = new event.logging.Object();
            stroomResource.setId(resource.getId());
            stroomResource.setType(resource.getType());
            authorise.getObjects().add(stroomResource);
            outcome = createOutcome(true);
            authorise.setOutcome(outcome);
            authorise.setAction(Authorisation.REQUEST);
            authorisationEventDetail.setAuthorise(authorise);
            loggingService.log(authorisationEvent);
            LOGGER.debug("ReadRequest authorisationEvent is {}", authorisationEvent);
        }
    }

    private static void auditErrorMessage(final String token, final DefaultEventLoggingService loggingService, final AuditErrorMessage message) {
        requireNonNull(message, AUDIT_MESSAGE_NULL);
        LOGGER.debug("auditErrorMessage called and the DefaultEventLoggingService is: {}, and AuditRequest is: {}", loggingService, message);
        // authorisation exception
        Event exceptionEvent = generateNewGenericEvent(token, loggingService, message);
        Event.EventDetail exceptionEventDetail = new Event.EventDetail();
        exceptionEvent.setEventDetail(exceptionEventDetail);
        // log the user
        addUserToEvent(exceptionEvent, message.getUserId());
        // log the purpose that was supplied with the request
        try {
            addPurposeToEvent(exceptionEvent, message.getContext());
        } catch (JsonProcessingException ex) {
            LOGGER.error(JSON_SERIALISING_ERROR, CONTEXT);
        }
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        // log the resource
        event.logging.Object stroomResource = new event.logging.Object();
        stroomResource.setId(message.getResourceId());
        authorise.getObjects().add(stroomResource);
        Outcome outcome = createOutcome(false);
        LOGGER.debug(ERROR_MESSAGE_FROM, message.getServiceName());
        if (ServiceName.USER_SERVICE.name().equalsIgnoreCase(message.getServiceName())) {
            exceptionEventDetail.setTypeId(AUDIT_ERROR_USER_EXCEPTION_ID);
            exceptionEventDetail.setDescription(AUDIT_ERROR_USER_EXCEPTION_DESCRIPTION);
            outcome.setDescription(AUDIT_ERROR_USER_EXCEPTION_OUTCOME);
        } else if (ServiceName.RESOURCE_SERVICE.name().equalsIgnoreCase(message.getServiceName())) {
            exceptionEventDetail.setTypeId(AUDIT_ERROR_RESOURCE_EXCEPTION_ID);
            exceptionEventDetail.setDescription(AUDIT_ERROR_RESOURCE_EXCEPTION_DESCRIPTION);
            outcome.setDescription(AUDIT_ERROR_RESOURCE_EXCEPTION_OUTCOME);
        } else {
            exceptionEventDetail.setTypeId(REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID);
            exceptionEventDetail.setDescription(REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION);
            try {
                outcome.setDescription(message.getError().getMessage());
            } catch (JsonProcessingException ex) {
                LOGGER.error(JSON_SERIALISING_ERROR, "error");
            }
        }
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        exceptionEventDetail.setAuthorise(authorise);
        loggingService.log(exceptionEvent);
        LOGGER.debug("onRegisterRequestException authorisationEvent is {}", exceptionEvent);
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
        LOGGER.debug(ORGANISATION, organisation);
        SYSTEM.setOrganisation(organisation);
        return this;
    }

    @Generated
    public String getOrganisation() {
        LOGGER.debug(ORGANISATION, SYSTEM.getOrganisation());
        return SYSTEM.getOrganisation();
    }

    @Generated
    public void setOrganisation(final String organisation) {
        LOGGER.debug(ORGANISATION, organisation);
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
        LOGGER.debug(SYSTEM_CLASSIFICATION, systemClassification);
        Classification classification = new Classification();
        classification.setText(systemClassification);
        SYSTEM.setClassification(classification);
        return this;
    }

    @Generated
    public String getSystemClassification() {
        LOGGER.debug(SYSTEM_CLASSIFICATION, SYSTEM.getClassification().getText());
        return SYSTEM.getClassification().getText();
    }

    @Generated
    public void setSystemClassification(final String systemClassification) {
        LOGGER.debug(SYSTEM_CLASSIFICATION, systemClassification);
        systemClassification(systemClassification);
    }

    @Override
    public CompletableFuture<Boolean> audit(final String token, final AuditMessage message) {
        requireNonNull(message, AUDIT_MESSAGE_NULL);
        if (message instanceof AuditSuccessMessage) {
            AuditSuccessMessage successMessage = (AuditSuccessMessage) message;
            if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.name()) || message.getServiceName().equals(ServiceName.DATA_SERVICE.name())){
                auditSuccessMessage(token, eventLogger, successMessage);
            } else {
                LOGGER.warn("An AuditSuccessMessage should only be sent by the FilteredResourceService or the DataService. Message received from {}",
                        message.getServiceName());
            }
        } else if (message instanceof AuditErrorMessage) {
            AuditErrorMessage errorMessage = (AuditErrorMessage) message;
            auditErrorMessage(token, eventLogger, errorMessage);
        } else {
            LOGGER.warn("The service {} has created unknown type of AuditMessage for token {}. Request: {}", message.getServiceName(), token, message);
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

}
