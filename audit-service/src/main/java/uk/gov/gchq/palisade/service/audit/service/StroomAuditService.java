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

import avro.shaded.com.google.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.core.JsonProcessingException;
import event.logging.Activity;
import event.logging.Authorisation;
import event.logging.Classification;
import event.logging.Data;
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

import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.common.Generated;
import uk.gov.gchq.palisade.service.audit.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.sql.Date;

import static java.util.Objects.requireNonNull;

/**
 * A StroomAuditService is a simple implementation of an {@link AuditService} that simply constructs a message and logs
 * it using the Stroom EventLoggingService.
 */
public class StroomAuditService implements AuditService {

    /**
     * The configuration key for property "audit.implementations". This property is
     * used to decide which service implementation Spring will inject.
     *
     * @see ApplicationConfiguration
     */
    public static final String CONFIG_KEY = "stroom";

    private static final Logger LOGGER = LoggerFactory.getLogger(StroomAuditService.class);
    private static final String AUDIT_MESSAGE_NULL = "The AuditMessage cannot be null";
    private static final String ERROR_MESSAGE_FROM = "AuditErrorMessage received from {}";
    private static final String EVENT_GENERATOR = "Palisade";
    private static final String JSON_SERIALISING_ERROR = "An error occurred while deserialising the {} - Message: {}";
    private static final String ORGANISATION = "organisation is {}";
    private static final String SYSTEM_CLASSIFICATION = "systemClassification is {}";
    private static final System SYSTEM = new System();

    @VisibleForTesting
    static final String READ_SUCCESS = "READ_REQUEST_COMPLETED";
    @VisibleForTesting
    static final String REQUEST_SUCCESS = "REGISTER_REQUEST_COMPLETED";

    private final DefaultEventLoggingService eventLogger;

    /**
     * Create a new StroomAuditService, which will take in an {@link AuditMessage} and produce events from the
     * request to be logged to an {@link event.logging.EventLoggingService}
     *
     * @param eventLoggingService the target {@link event.logging.EventLoggingService} for all created events
     */
    public StroomAuditService(final DefaultEventLoggingService eventLoggingService) {
        eventLogger = eventLoggingService;
        LOGGER.debug("StroomAuditService called and the defaultEventLoggingService is: {}", eventLoggingService);
    }

    private static void addUserToEvent(final Event event, final String user) {
        Event.EventSource eventSource = event.getEventSource();
        User stroomUser = EventLoggingUtil.createUser(user);
        eventSource.setUser(stroomUser);
        LOGGER.debug("addUserToEvent called and the event is: {}, and userId is: {}", event, user);

    }

    private static void addPurposeToEvent(final Event event, final Context context) {
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
        LOGGER.debug("auditErrorMessage called and the DefaultEventLoggingService is: {}, and AuditMessage is: {}", loggingService, message);
        LOGGER.debug(ERROR_MESSAGE_FROM, message.getServiceName());
        // Authorisation successful
        Event successEvent = generateNewGenericEvent(token, loggingService, message);
        Event.EventDetail successEventDetail = new Event.EventDetail();
        successEvent.setEventDetail(successEventDetail);
        // Log the user
        addUserToEvent(successEvent, message.getUserId());
        // Log the purpose that was supplied with the request
        try {
            addPurposeToEvent(successEvent, message.getContext());
        } catch (JsonProcessingException ex) {
            LOGGER.error(JSON_SERIALISING_ERROR, "context", ex.getMessage());
        }
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        // Log the resource
        event.logging.Object stroomResource = new event.logging.Object();
        stroomResource.setId(message.getLeafResourceId());
        authorise.getObjects().add(stroomResource);
        // Log the success message
        Outcome outcome = createOutcome(true);
        successEventDetail.setTypeId(message.getServiceName());
        if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.value)) {
            successEventDetail.setDescription(REQUEST_SUCCESS);
        } else if (message.getServiceName().equals(ServiceName.DATA_SERVICE.value)) {
            successEventDetail.setDescription(READ_SUCCESS);
        } else {
            successEventDetail.setDescription("UNKNOWN_SUCCESS_MESSAGE");
        }
        // Log the attributes
        Data data = new Data();
        data.setName("attributes");
        data.setValue(message.getAttributesNode().asText());
        outcome.getData().add(data);
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        successEventDetail.setAuthorise(authorise);
        // Send to Stroom
        loggingService.log(successEvent);
        LOGGER.debug("Success Message authorisationEvent is {}", successEvent);
    }

    private static void auditErrorMessage(final String token, final DefaultEventLoggingService loggingService, final AuditErrorMessage message) {
        requireNonNull(message, AUDIT_MESSAGE_NULL);
        LOGGER.debug("auditErrorMessage called and the DefaultEventLoggingService is: {}, and AuditMessage is: {}", loggingService, message);
        LOGGER.debug(ERROR_MESSAGE_FROM, message.getServiceName());
        // Authorisation exception
        Event exceptionEvent = generateNewGenericEvent(token, loggingService, message);
        Event.EventDetail exceptionEventDetail = new Event.EventDetail();
        exceptionEvent.setEventDetail(exceptionEventDetail);
        // Log the user
        addUserToEvent(exceptionEvent, message.getUserId());
        // Log the purpose that was supplied with the request
        try {
            addPurposeToEvent(exceptionEvent, message.getContext());
        } catch (JsonProcessingException ex) {
            LOGGER.error(JSON_SERIALISING_ERROR, "context", ex.getMessage());
        }
        Event.EventDetail.Authorise authorise = new Event.EventDetail.Authorise();
        // Log the resource
        event.logging.Object stroomResource = new event.logging.Object();
        stroomResource.setId(message.getResourceId());
        authorise.getObjects().add(stroomResource);
        // Log the exception
        Outcome outcome = createOutcome(false);
        exceptionEventDetail.setTypeId(message.getServiceName());
        exceptionEventDetail.setDescription(message.getErrorNode().get("stackTrace").get(0).get("className").textValue());
        outcome.setDescription(message.getErrorNode().get("message").textValue());
        // Log the attributes
        Data data = new Data();
        data.setName("attributes");
        data.setValue(message.getAttributesNode().asText());
        outcome.getData().add(data);
        authorise.setOutcome(outcome);
        authorise.setAction(Authorisation.REQUEST);
        exceptionEventDetail.setAuthorise(authorise);
        // Send to Stroom
        loggingService.log(exceptionEvent);
        LOGGER.debug("Error Message authorisationEvent is {}", exceptionEvent);
    }

    /**
     * Sets the new system name from which the audit service is receiving an audit
     * message
     *
     * @param systemName the name of the system
     * @return StroomAuditService
     * @throws NullPointerException if {@code systemName} is null
     */
    @Generated
    public StroomAuditService systemName(final String systemName) {
        requireNonNull(systemName, "The system name cannot be null.");
        LOGGER.debug("systemName is {}", systemName);
        SYSTEM.setName(systemName);
        return this;
    }

    /**
     * Returns the system name
     *
     * @return the system name
     */
    @Generated
    public String getSystemName() {
        return SYSTEM.getName();
    }

    /**
     * Sets the new system name from which the audit service is receiving audit logs
     * from
     *
     * @param systemName the name of the system
     * @throws NullPointerException if {@code systemName} is null
     */
    @Generated
    public void setSystemName(final String systemName) {
        systemName(systemName);
    }

    /**
     * Sets the new organisation
     *
     * @param organisation the organisation that the system belongs too
     * @return {@link StroomAuditService}
     * @throws NullPointerException if {@code organisation} is null
     */
    @Generated
    public StroomAuditService organisation(final String organisation) {
        requireNonNull(organisation, "The organisation cannot be null.");
        LOGGER.debug(ORGANISATION, organisation);
        SYSTEM.setOrganisation(organisation);
        return this;
    }

    /**
     * Returns the organisation
     *
     * @return the organisation
     */
    @Generated
    public String getOrganisation() {
        LOGGER.debug(ORGANISATION, SYSTEM.getOrganisation());
        return SYSTEM.getOrganisation();
    }

    /**
     * Sets the new organisation
     *
     * @param organisation the organisation that the system belongs too
     * @throws NullPointerException if {@code organisation} is null
     */
    @Generated
    public void setOrganisation(final String organisation) {
        LOGGER.debug(ORGANISATION, organisation);
        organisation(organisation);
    }

    /**
     * Sets the new system environment of this deployment, e.g prod, ref, test
     *
     * @param systemEnv the system environment
     * @return {@link StroomAuditService}
     * @throws NullPointerException if {@code systemEnv} is null
     */
    @Generated
    public StroomAuditService systemEnv(final String systemEnv) {
        requireNonNull(systemEnv, "The env cannot be null.");
        LOGGER.debug("systemEnv is {}", systemEnv);
        SYSTEM.setEnvironment(systemEnv);
        return this;
    }

    /**
     * Returns the system environment
     *
     * @return the system environment
     */
    @Generated
    public String getSystemEnv() {
        return SYSTEM.getEnvironment();
    }

    /**
     * Sets the new system environment of this deployment, e.g prod, ref, test
     *
     * @param systemEnv the system environment
     * @throws NullPointerException if {@code env} is null
     */
    @Generated
    public void setSystemEnv(final String systemEnv) {
        systemEnv(systemEnv);
    }

    /**
     * Sets the new system description from which the audit service is receiving
     * audit logs from
     *
     * @param description the system description
     * @return {@link StroomAuditService}
     * @throws NullPointerException if {@code description} is null
     */
    @Generated
    public StroomAuditService systemDescription(final String description) {
        requireNonNull(description, "The description cannot be null.");
        LOGGER.debug("systemDescription is {}", description);
        SYSTEM.setDescription(description);
        return this;
    }

    /**
     * Returns the system description
     *
     * @return the system description
     */
    @Generated
    public String getSystemDescription() {
        return SYSTEM.getDescription();
    }

    /**
     * Sets the new system description from which the audit service is receiving
     * audit logs from
     *
     * @param description the system description
     * @throws NullPointerException if {@code description} is null
     */
    @Generated
    public void setSystemDescription(final String description) {
        systemDescription(description);
    }

    /**
     * Sets the new system version of this deployment, v1, v1.0.2, v2, etc
     *
     * @param systemVersion the system version
     * @return {@link StroomAuditService}
     * @throws NullPointerException if {@code systemVersion} is null
     */
    @Generated
    public StroomAuditService systemVersion(final String systemVersion) {
        requireNonNull(systemVersion, "The systemVersion cannot be null.");
        SYSTEM.setVersion(systemVersion);
        return this;
    }

    /**
     * Returns the system version
     *
     * @return the system version
     */
    @Generated
    public String getSystemVersion() {
        return SYSTEM.getVersion();
    }

    /**
     * Sets the new system version of this deployment, v1, v1.0.2, v2, etc
     *
     * @param systemVersion the system version
     * @throws NullPointerException if {@code systemVersion} is null
     */
    @Generated
    public void setSystemVersion(final String systemVersion) {
        systemVersion(systemVersion);
    }

    /**
     * Sets the new system classification of the system from which the audit service
     * is receiving audit logs from
     *
     * @param systemClassification the new system classification
     * @return {@link StroomAuditService}
     * @throws NullPointerException if {@code systemClassification} is null
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

    /**
     * Returns the system classification
     *
     * @return the system classification
     */
    @Generated
    public String getSystemClassification() {
        LOGGER.debug(SYSTEM_CLASSIFICATION, SYSTEM.getClassification().getText());
        return SYSTEM.getClassification().getText();
    }

    /**
     * Sets the new system classification of the system from which the audit service
     * is receiving audit logs from
     *
     * @param systemClassification the new system classification
     * @throws NullPointerException if {@code systemClassification} is null
     */
    @Generated
    public void setSystemClassification(final String systemClassification) {
        LOGGER.debug(SYSTEM_CLASSIFICATION, systemClassification);
        systemClassification(systemClassification);
    }

    @Override
    public Boolean audit(final String token, final AuditMessage message) {
        if (message instanceof AuditSuccessMessage) {
            AuditSuccessMessage successMessage = (AuditSuccessMessage) message;
            if (message.getServiceName().equals(ServiceName.FILTERED_RESOURCE_SERVICE.value) || message.getServiceName().equals(ServiceName.DATA_SERVICE.value)) {
                auditSuccessMessage(token, eventLogger, successMessage);
                return true;
            }
            LOGGER.warn(
                "An AuditSuccessMessage should only be sent by the FilteredResourceService or the DataService. Message received from {}",
                message.getServiceName());
            return false;
        } else if (message instanceof AuditErrorMessage) {
            AuditErrorMessage errorMessage = (AuditErrorMessage) message;
            auditErrorMessage(token, eventLogger, errorMessage);
            return true;
        } else {
            LOGGER.warn("The service {} has created unknown type of AuditMessage for token {}. Request: {}", message.getServiceName(), token, message);
            return false;
        }
    }

}
