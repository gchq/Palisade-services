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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is one of the objects that is passed to the audit-service to be able to store an audit record. This class extends
 * {@link AuditRequest} This class is used for the indication to the Audit logs that processing has been completed.
 */
public class ReadRequestCompleteAuditRequest extends AuditRequest {

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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReadRequestCompleteAuditRequest that = (ReadRequestCompleteAuditRequest) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(user, that.user)
                .append(context, that.context)
                .append(leafResource, that.leafResource)
                .append(rulesApplied, that.rulesApplied)
                .append(numberOfRecordsReturned, that.numberOfRecordsReturned)
                .append(numberOfRecordsProcessed, that.numberOfRecordsProcessed)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(29, 37)
                .appendSuper(super.hashCode())
                .append(user)
                .append(context)
                .append(leafResource)
                .append(rulesApplied)
                .append(numberOfRecordsReturned)
                .append(numberOfRecordsProcessed)
                .toHashCode();
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
}