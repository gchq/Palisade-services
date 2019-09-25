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
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.request.Request;

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

    // no-args constructor required
    public RegisterDataRequest() {
    }

    /**
     * @param userId an identifier for the user requesting the data
     * @return the {@link RegisterDataRequest}
     */
    public RegisterDataRequest userId(final UserId userId) {
        requireNonNull(userId, "The user id cannot be set to null.");
        this.userId = userId;
        return this;
    }

    /**
     * @param resourceId an identifier for the resource or data set to access
     * @return the {@link RegisterDataRequest}
     */
    public RegisterDataRequest resourceId(final String resourceId) {
        requireNonNull(resourceId, "The resource id cannot be set to null.");
        this.resourceId = resourceId;
        return this;
    }

    /**
     * @param context the contextual information required for this request such as the reason why the user wants access to the data
     * @return the {@link RegisterDataRequest}
     */
    public RegisterDataRequest context(final Context context) {
        requireNonNull(context, "The context cannot be set to null.");
        this.context = context;
        return this;
    }

    public String getResourceId() {
        requireNonNull(resourceId, "The resource id has not been set.");
        return resourceId;
    }

    public void setResourceId(final String resourceId) {
        resourceId(resourceId);
    }

    public UserId getUserId() {
        requireNonNull(userId, "The user id has not been set.");
        return userId;
    }

    public void setUserId(final UserId userId) {
        userId(userId);
    }

    public Context getContext() {
        requireNonNull(context, "The context has not been set");
        return context;
    }

    public void setContext(final Context context) {
        context(context);
    }


    @Override
    public void setOriginalRequestId(final RequestId originalRequestId) {
        throw new ForbiddenException("Should not call RegisterDataRequest.setOriginalRequestId()");
    }

    @Override
    public RequestId getOriginalRequestId() {
        throw new ForbiddenException("Should not call RegisterDataRequest.getOriginalRequestId()");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegisterDataRequest)) return false;
        if (!super.equals(o)) return false;

        RegisterDataRequest that = (RegisterDataRequest) o;

        if (!getUserId().equals(that.getUserId())) return false;
        if (!getContext().equals(that.getContext())) return false;
        return getResourceId().equals(that.getResourceId());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getUserId().hashCode();
        result = 31 * result + getContext().hashCode();
        result = 31 * result + getResourceId().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RegisterDataRequest{" +
                "userId=" + userId +
                ", context=" + context +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }
}
