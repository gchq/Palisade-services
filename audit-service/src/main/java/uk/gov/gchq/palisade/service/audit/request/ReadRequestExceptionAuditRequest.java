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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is one of the objects that is passed to the audit-service
 * to be able to store an audit record. This class extends {@link AuditRequest} This class
 * is used for the indication to the Audit logs that an exception has been received.
 */
public class ReadRequestExceptionAuditRequest extends AuditRequest {

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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReadRequestExceptionAuditRequest that = (ReadRequestExceptionAuditRequest) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(token, that.token)
                .append(leafResource, that.leafResource)
                .append(exception, that.exception)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 39)
                .appendSuper(super.hashCode())
                .append(token)
                .append(leafResource)
                .append(exception)
                .toHashCode();
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