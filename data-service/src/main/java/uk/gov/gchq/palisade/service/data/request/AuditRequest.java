/*
 * Copyright 2018 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.request.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is the abstract class that is passed to the audit-service
 * to be able to store an audit record. The default information is
 * when was the audit record created and by what server.
 * The four immutable data subclasses below can be instantiated by static
 * {@code create(RequestId orig)} factory methods which chain construction by fluid interface definitions.
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestExceptionAuditRequest.class)
})
public class AuditRequest extends Request {

    public final Date timestamp;
    public final String serverIp;
    public final String serverHostname;

    private AuditRequest() {
        this.timestamp = null;
        this.serverIp = null;
        this.serverHostname = null;
    }

    private AuditRequest(final RequestId originalRequestId) {
        super.setOriginalRequestId(requireNonNull(originalRequestId));

        this.timestamp = new Date();
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        serverHostname = inetAddress.getHostName();
        serverIp = inetAddress.getHostAddress();
    }

    /**
     * This is one of the objects that is passed to the audit-service to be able to store an audit record. This class extends
     * {@link AuditRequest} This class is used for the indication to the Audit logs that processing has been completed.
     */
    public static class ReadRequestCompleteAuditRequest extends AuditRequest {

        public final User user;
        public final LeafResource leafResource;
        public final Context context;
        public final Rules rulesApplied;
        public final long numberOfRecordsReturned;
        public final long numberOfRecordsProcessed;

        @JsonCreator
        private ReadRequestCompleteAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("context") final Context context,
                                                @JsonProperty("rulesApplied") final Rules rulesApplied, @JsonProperty("numberOfRecordsReturned") final long numberOfRecordsReturned, @JsonProperty("numberOfRecordsProcessed") final long numberOfRecordsProcessed) {
            super(originalRequestId);
            this.user = requireNonNull(user);
            this.leafResource = requireNonNull(leafResource);
            this.context = requireNonNull(context);
            this.rulesApplied = requireNonNull(rulesApplied);
            this.numberOfRecordsReturned = numberOfRecordsReturned;
            this.numberOfRecordsProcessed = numberOfRecordsProcessed;
        }

        public interface IUser {
            /**
             * @param user {@link User} is the user that made the initial registration request to access data
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ILeafResource withUser(final User user);
        }

        public interface ILeafResource {
            /**
             * @param leafResource the {@link LeafResource} which the data has just finished being read
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IContext withLeafResource(final LeafResource leafResource);
        }

        public interface IContext {
            /**
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IRulesApplied withContext(final Context context);
        }

        public interface IRulesApplied {
            /**
             * @param rules {@link Rules} is the rules that are being applied to this resource for this request
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsReturned withRulesApplied(final Rules rules);
        }

        public interface INumberOfRecordsReturned {
            /**
             * @param numberOfRecordsReturned is the number of records that was returned to the user from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsProcessed withNumberOfRecordsReturned(final long numberOfRecordsReturned);
        }

        public interface INumberOfRecordsProcessed {
            /**
             * @param numberOfRecordsProcessed is the number of records that was processed from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ReadRequestCompleteAuditRequest withNumberOfRecordsProcessed(final long numberOfRecordsProcessed);
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return {@link ReadRequestCompleteAuditRequest}
         */
        public static IUser create(final RequestId original) {
            return user -> leafResource -> context -> rulesApplied -> numberOfRecordsReturned -> numberOfRecordsProcessed -> new ReadRequestCompleteAuditRequest(null, original, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ReadRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("user=" + user)
                    .add("leafResource=" + leafResource)
                    .add("context=" + context)
                    .add("rulesApplied=" + rulesApplied)
                    .add("numberOfRecordsReturned=" + numberOfRecordsReturned)
                    .add("numberOfRecordsProcessed=" + numberOfRecordsProcessed)
                    .toString();
        }
    }

    /**
     * This is one of the objects that is passed to the audit-service
     * to be able to store an audit record. This class extends {@link Request} This class
     * is used for the indication to the Audit logs that an exception has been received.
     */
    public static class ReadRequestExceptionAuditRequest extends AuditRequest {

        public final String token;
        public final LeafResource leafResource;
        public final Throwable exception;

        @JsonCreator
        private ReadRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("token") final String token, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("exception") final Throwable exception) {
            super(originalRequestId);
            this.token = requireNonNull(token);
            this.leafResource = requireNonNull(leafResource);
            this.exception = requireNonNull(exception);
        }

        public interface IToken {
            /**
             * @param token this is the token that is used to retrieve cached information from the palisade service
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ILeafResource withToken(final String token);
        }

        public interface ILeafResource {
            /**
             * @param leafResource {@link LeafResource} is the leafResource for the ReadRequest
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            IThrowable withLeafResource(final LeafResource leafResource);
        }

        public interface IThrowable {
            /**
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ReadRequestExceptionAuditRequest withException(final Throwable exception);
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
        public String toString() {
            return new StringJoiner(", ", ReadRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("token='" + token + "'")
                    .add("leafResource=" + leafResource)
                    .add("exception=" + exception)
                    .toString();
        }
    }

    @Override
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
        AuditRequest that = (AuditRequest) o;
        return timestamp.equals(that.timestamp) &&
                serverIp.equals(that.serverIp) &&
                serverHostname.equals(that.serverHostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, serverIp, serverHostname);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AuditRequest.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("timestamp=" + timestamp)
                .add("serverIp='" + serverIp + "'")
                .add("serverHostname='" + serverHostname + "'")
                .toString();
    }
}
