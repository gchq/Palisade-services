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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to wrap all the information that the user needs to supply
 * to the palisade service to register the data access request.
 */
@JsonIgnoreProperties(value = {"originalRequestId"})
public class RegisterDataRequest extends Request {
    private UserId userId;
    private Context context;
    private String resourceId;

    public RegisterDataRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param userId an identifier for the user requesting the data
     * @return the {@link RegisterDataRequest}
     */
    @Generated
    public RegisterDataRequest userId(final UserId userId) {
        requireNonNull(userId, "The user id cannot be set to null.");
        this.setUserId(userId);
        return this;
    }

    /**
     * @param resourceId an identifier for the resource or data set to access
     * @return the {@link RegisterDataRequest}
     */
    @Generated
    public RegisterDataRequest resourceId(final String resourceId) {
        requireNonNull(resourceId, "The resource id cannot be set to null.");
        this.setResourceId(resourceId);
        return this;
    }

    /**
     * @param context the contextual information required for this request such as the reason why the user wants access to the data
     * @return the {@link RegisterDataRequest}
     */
    @Generated
    public RegisterDataRequest context(final Context context) {
        requireNonNull(context, "The context cannot be set to null.");
        this.setContext(context);
        return this;
    }

    @Generated
    public UserId getUserId() {
        return userId;
    }

    @Generated
    public void setUserId(final UserId userId) {
        requireNonNull(userId);
        this.userId = userId;
    }

    @Generated
    public Context getContext() {
        return context;
    }

    @Generated
    public void setContext(final Context context) {
        requireNonNull(context);
        this.context = context;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public void setResourceId(final String resourceId) {
        requireNonNull(resourceId);
        this.resourceId = resourceId;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegisterDataRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final RegisterDataRequest that = (RegisterDataRequest) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(context, that.context) &&
                Objects.equals(resourceId, that.resourceId);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, context, resourceId);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", RegisterDataRequest.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("context=" + context)
                .add("resourceId='" + resourceId + "'")
                .toString();
    }
}
