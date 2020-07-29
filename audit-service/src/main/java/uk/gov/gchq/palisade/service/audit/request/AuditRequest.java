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
package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.request.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is the abstract class that is passed to the audit-service to be able to store an audit record. The default information consists of
 * when the audit record was created and which server created it.
 *
 * The four immutable data subclasses below can be instantiated by static {@code create(RequestId orig)} factory methods which chain
 * construction by fluid interface definitions.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "class"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestExceptionAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestExceptionAuditRequest.class)
})
public class AuditRequest extends Request {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRequest.class);

    /**
     * Timestamp for when the audit request was created
     */
    public final ZonedDateTime timestamp;
    /**
     * Localhost IP address of the machine that created this request
     */
    public final String serverIp;
    /**
     * Localhost hostname of the machine that created this request
     */
    public final String serverHostname;

    protected AuditRequest() {
        this.timestamp = null;
        this.serverIp = null;
        this.serverHostname = null;
    }

    protected AuditRequest(final RequestId originalRequestId) {
        super.setOriginalRequestId(requireNonNull(originalRequestId));
        LOGGER.debug("AuditRequest called passing in {}", originalRequestId);
        this.timestamp = ZonedDateTime.now();
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.error("AuditRequest UnknownHostException", e);
            throw new RuntimeException(e);
        }
        serverHostname = inetAddress.getHostName();
        serverIp = inetAddress.getHostAddress();
        LOGGER.debug("AuditRequest instantiated and serverHostname is: {}, and serverIP is {}", serverHostname, serverIp);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final AuditRequest that = (AuditRequest) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(serverIp, that.serverIp) &&
                Objects.equals(serverHostname, that.serverHostname);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, serverIp, serverHostname);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditRequest.class.getSimpleName() + "[", "]")
                .add("timestamp=" + timestamp)
                .add("serverIp='" + serverIp + "'")
                .add("serverHostname='" + serverHostname + "'")
                .add(super.toString())
                .toString();
    }

    /**
     * Used to indicate to the Audit service that a RegisterDataRequest has been successfully
     * processed and these are the resources that this user is approved to read for this data access request.
     */
    public static final class RegisterRequestCompleteAuditRequest extends AuditRequest {

        /**
         * The {@link User} who made a request to Palisade.
         */
        public final User user;
        /**
         * The {@link LeafResource}s returned as accessible to this user (after applying policy)
         */
        public final Set<LeafResource> leafResources;
        /**
         * The {@link Context} for this request
         */
        public final Context context;

        @JsonCreator
        private RegisterRequestCompleteAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user,
                                                    @JsonProperty("leafResources") final Set<LeafResource> leafResources, @JsonProperty("context") final Context context) {
            super(originalRequestId);
            this.user = requireNonNull(user);
            this.leafResources = requireNonNull(leafResources);
            this.context = requireNonNull(context);
        }

        /**
         * Static factory method.
         *
         * @param original the originating request Id
         * @return the {@link RegisterRequestCompleteAuditRequest}
         */
        public static IUser create(final RequestId original) {
            return user -> leafResources -> context -> new RegisterRequestCompleteAuditRequest(null, original, user, leafResources, context);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RegisterRequestCompleteAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final RegisterRequestCompleteAuditRequest that = (RegisterRequestCompleteAuditRequest) o;
            return Objects.equals(user, that.user) &&
                    Objects.equals(leafResources, that.leafResources) &&
                    Objects.equals(context, that.context);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), user, leafResources, context);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", RegisterRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add("user=" + user)
                    .add("leafResources=" + leafResources)
                    .add("context=" + context)
                    .add(super.toString())
                    .toString();
        }

        /**
         * Fluid interface requiring a {@link User} for this {@link AuditRequest}
         */
        public interface IUser {
            /**
             * Add a user to the request
             * @param user {@link User} is the user that made the initial registration request to access data
             * @return the {@link RegisterRequestCompleteAuditRequest}
             */
            ILeafResources withUser(User user);
        }

        /**
         * Fluid interface requiring a {@link Set} of {@link LeafResource}s for this {@link AuditRequest}
         */
        public interface ILeafResources {
            /**
             * Add a set of leaf resources to the request
             * @param leafResources a set of {@link LeafResource} which contains the relevant details about the resource being accessed
             * @return the {@link RegisterRequestCompleteAuditRequest}
             */
            IContext withLeafResources(Set<LeafResource> leafResources);
        }

        /**
         * Fluid interface requiring a {@link Context} for this {@link AuditRequest}
         */
        public interface IContext {
            /**
             * Add a context to the request
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link RegisterRequestCompleteAuditRequest}
             */
            RegisterRequestCompleteAuditRequest withContext(Context context);
        }
    }

    /**
     * Used to indicate to the Audit service that an exception has been received while processing the RegisterDataRequest
     * and which service triggered the exception.
     */
    public static final class RegisterRequestExceptionAuditRequest extends AuditRequest {

        /**
         * The {@link UserId} declared by the client on access
         */
        public final UserId userId;
        /**
         * The requested resourceId to be accessed by the client
         */
        public final String resourceId;
        /**
         * The {@link Context} for this request
         */
        public final Context context;
        /**
         * A caught exception that caused this audit
         */
        public final Throwable exception;
        /**
         * The service that threw the exception
         */
        public final String serviceName;

        @JsonCreator
        private RegisterRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId,
                                                     @JsonProperty("userId") final UserId userId, @JsonProperty("resourceId") final String resourceId, @JsonProperty("context") final Context context,
                                                     @JsonProperty("exception") final Throwable exception, @JsonProperty("serviceName") final String serviceName) {
            super(originalRequestId);
            this.userId = requireNonNull(userId);
            this.resourceId = requireNonNull(resourceId);
            this.context = requireNonNull(context);
            this.exception = requireNonNull(exception);
            this.serviceName = requireNonNull(serviceName);
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        public static IUserId create(final RequestId original) {
            return user -> resourceId -> context -> exception -> serviceClass -> new RegisterRequestExceptionAuditRequest(null, original, user, resourceId, context, exception, serviceClass);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RegisterRequestExceptionAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final RegisterRequestExceptionAuditRequest that = (RegisterRequestExceptionAuditRequest) o;
            return Objects.equals(userId, that.userId) &&
                    Objects.equals(resourceId, that.resourceId) &&
                    Objects.equals(context, that.context) &&
                    Objects.equals(exception, that.exception) &&
                    Objects.equals(serviceName, that.serviceName);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), userId, resourceId, context, exception, serviceName);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", RegisterRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add("userId=" + userId)
                    .add("resourceId='" + resourceId + "'")
                    .add("context=" + context)
                    .add("exception=" + exception)
                    .add("serviceClass=" + serviceName)
                    .add(super.toString())
                    .toString();
        }

        /**
         * Fluid interface requiring a {@link UserId} for this {@link AuditRequest}
         */
        public interface IUserId {
            /**
             * Add a userId to the request
             * @param userId {@link UserId} is the user id provided in the register request
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IResourceId withUserId(UserId userId);
        }

        /**
         * Fluid interface requiring a resourceId for this {@link AuditRequest}
         */
        public interface IResourceId {
            /**
             * Add a resourceId to the request
             * @param resourceId {@link String} is the resource id provided in the register request
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IContext withResourceId(String resourceId);
        }

        /**
         * Fluid interface requiring a {@link Context} for this {@link AuditRequest}
         */
        public interface IContext {
            /**
             * Add a context to the request
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IException withContext(Context context);
        }

        /**
         * Fluid interface requiring a {@link Throwable} for this {@link AuditRequest}
         */
        public interface IException {
            /**
             * Add an exception to the request
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IServiceClass withException(Throwable exception);
        }

        /**
         * Fluid interface requiring a {@link Service} for this {@link AuditRequest}
         */
        public interface IServiceClass {
            /**
             * Add a service class to the request
             * @param serviceName {@link String} name of the palisade service that the exception was triggered by.
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            RegisterRequestExceptionAuditRequest withServiceName(String serviceName);
        }
    }

    /**
     * Used to indicate to the Audit service that the data-service has successfully completed a ReadRequest and returned data to a client.
     */
    public static final class ReadRequestCompleteAuditRequest extends AuditRequest {

        /**
         * The {@link User} that requested to read some data
         */
        public final User user;
        /**
         * The {@link LeafResource} that was read, with data from this resource returned to the client
         */
        public final LeafResource leafResource;
        /**
         * The {@link Context} for this data read
         */
        public final Context context;
        /**
         * The {@link Rules} that were applied to each object read from the resource
         */
        public final Rules rulesApplied;
        /**
         * The number of records returned to the client (ie. excluding those which were redacted entirely)
         */
        public final long numberOfRecordsReturned;
        /**
         * The number of records processed by the data-reader (ex. including those which were redacted entirely)
         */
        public final long numberOfRecordsProcessed;

        @JsonCreator
        private ReadRequestCompleteAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId,
                                                @JsonProperty("user") final User user, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("context") final Context context,
                                                @JsonProperty("rulesApplied") final Rules rulesApplied, @JsonProperty("numberOfRecordsReturned") final long numberOfRecordsReturned,
                                                @JsonProperty("numberOfRecordsProcessed") final long numberOfRecordsProcessed) {
            super(originalRequestId);
            this.user = requireNonNull(user);
            this.leafResource = requireNonNull(leafResource);
            this.context = requireNonNull(context);
            this.rulesApplied = requireNonNull(rulesApplied);
            this.numberOfRecordsReturned = numberOfRecordsReturned;
            this.numberOfRecordsProcessed = numberOfRecordsProcessed;
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return {@link ReadRequestCompleteAuditRequest}
         */
        public static IUser create(final RequestId original) {
            return user -> leafResource -> context -> rulesApplied -> numberOfRecordsReturned -> numberOfRecordsProcessed ->
                    new ReadRequestCompleteAuditRequest(null, original, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReadRequestCompleteAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final ReadRequestCompleteAuditRequest that = (ReadRequestCompleteAuditRequest) o;
            return numberOfRecordsReturned == that.numberOfRecordsReturned &&
                    numberOfRecordsProcessed == that.numberOfRecordsProcessed &&
                    Objects.equals(user, that.user) &&
                    Objects.equals(leafResource, that.leafResource) &&
                    Objects.equals(context, that.context) &&
                    Objects.equals(rulesApplied, that.rulesApplied);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ReadRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add("user=" + user)
                    .add("leafResource=" + leafResource)
                    .add("context=" + context)
                    .add("rulesApplied=" + rulesApplied)
                    .add("numberOfRecordsReturned=" + numberOfRecordsReturned)
                    .add("numberOfRecordsProcessed=" + numberOfRecordsProcessed)
                    .add(super.toString())
                    .toString();
        }

        /**
         * Fluid interface requiring a {@link User} for this {@link AuditRequest}
         */
        public interface IUser {
            /**
             * Add a user to the request
             * @param user {@link User} is the user that made the initial registration request to access data
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ILeafResource withUser(User user);
        }

        /**
         * Fluid interface requiring a {@link LeafResource} for this {@link AuditRequest}
         */
        public interface ILeafResource {
            /**
             * Add a leaf resource to the request
             * @param leafResource the {@link LeafResource} which the data has just finished being read
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IContext withLeafResource(LeafResource leafResource);
        }

        /**
         * Fluid interface requiring a {@link Context} for this {@link AuditRequest}
         */
        public interface IContext {
            /**
             * Add a context to the request
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IRulesApplied withContext(Context context);
        }

        /**
         * Fluid interface requiring the {@link Rules} applied to the resource for this {@link AuditRequest}
         */
        public interface IRulesApplied {
            /**
             * Add a collection of rules to the request
             * @param rules {@link Rules} is the rules that are being applied to this resource for this request
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsReturned withRulesApplied(Rules rules);
        }

        /**
         * Fluid interface requiring the number of records returned for this {@link AuditRequest}
         */
        public interface INumberOfRecordsReturned {
            /**
             * Add the number of records returned to the request
             * @param numberOfRecordsReturned is the number of records that was returned to the user from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsProcessed withNumberOfRecordsReturned(long numberOfRecordsReturned);
        }

        /**
         * Fluid interface requiring the number of records processed for this {@link AuditRequest}
         */
        public interface INumberOfRecordsProcessed {
            /**
             * Add the number of records processed to the request
             * @param numberOfRecordsProcessed is the number of records that was processed from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ReadRequestCompleteAuditRequest withNumberOfRecordsProcessed(long numberOfRecordsProcessed);
        }
    }

    /**
     * Used to indicate to the Audit service that the data-service encountered an exception while processing the ReadRequest.
     */
    public static final class ReadRequestExceptionAuditRequest extends AuditRequest {

        /**
         * The token generated by the palisade-service and supplied as part of a ReadRequest
         */
        public final String token;
        /**
         * The leaf resource requested by the client and supplied as part of a ReadRequest
         */
        public final LeafResource leafResource;
        /**
         * The exception thrown while trying to read the data record or resource
         */
        public final Throwable exception;

        @JsonCreator
        private ReadRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("token") final String token, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("exception") final Throwable exception) {
            super(originalRequestId);
            this.token = requireNonNull(token);
            this.leafResource = requireNonNull(leafResource);
            this.exception = requireNonNull(exception);
        }

        /**
         * Static factory method.
         *
         * @param original request id.
         * @return the {@link ReadRequestExceptionAuditRequest}
         */
        public static IToken create(final RequestId original) {
            return token -> leafResource -> exception -> new ReadRequestExceptionAuditRequest(null, original, token, leafResource, exception);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReadRequestExceptionAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final ReadRequestExceptionAuditRequest that = (ReadRequestExceptionAuditRequest) o;
            return Objects.equals(token, that.token) &&
                    Objects.equals(leafResource, that.leafResource) &&
                    Objects.equals(exception, that.exception);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), token, leafResource, exception);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ReadRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add("token='" + token + "'")
                    .add("leafResource=" + leafResource)
                    .add("exception=" + exception)
                    .add(super.toString())
                    .toString();
        }

        /**
         * Fluid interface requiring a token for this {@link AuditRequest}
         */
        public interface IToken {
            /**
             * Add a token to the request
             * @param token this is the token that is used to retrieve cached information from the palisade service
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ILeafResource withToken(String token);
        }

        /**
         * Fluid interface requiring a {@link LeafResource} for this {@link AuditRequest}
         */
        public interface ILeafResource {
            /**
             * Add a leaf resource to the request
             * @param leafResource {@link LeafResource} is the leafResource for the ReadRequest
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            IThrowable withLeafResource(LeafResource leafResource);
        }

        /**
         * Fluid interface requiring a {@link Throwable} for this {@link AuditRequest}
         */
        public interface IThrowable {
            /**
             * Add an exception to the request
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ReadRequestExceptionAuditRequest withException(Throwable exception);
        }
    }
}
