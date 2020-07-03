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
package uk.gov.gchq.palisade.service.resource.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Audit information for a request provided by each service.
 * Each individual service sends a record to the Audit Service for every request that it receives.
 * The components of the message will differ depending on which service has sent the data and if the processing was
 * successful or not.
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    private static final ObjectMapper MAPPER = new ObjectMapper();



    public final String timeStamp; //when the service processed the request
    public final String serverIp;   //server for the service that processed the request
    public final String serverHostname;  //server host name for the service

    public final Context context;  //Context of the client's request

    public final User user;        //User  for the client.  Can be null if there is a userId

    public final String resourceId;  //Resource Id for the client.  Can be null if there is a Resource
    public final LeafResource resource; //Resource for the client.  Can be null if there is a Resource Id

    public final String errorMessage;  //Error message that occurred during thge processing of the request.  Will be null if there was no issue.

    @SuppressWarnings("checkstyle:parameterNumber")
    @JsonCreator
    private AuditMessage(
            final @JsonProperty("timeStamp") String timeStamp,
            final @JsonProperty("serverIp") String serverIp,
            final @JsonProperty("serverHostname") String serverHostname,
            final @JsonProperty("context") Context context,
            final @JsonProperty("user") User user,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("resource") LeafResource resource,
            final @JsonProperty("errorMessage") String errorMessage) {

        //required parameters
        Assert.notNull(timeStamp, "TimeStamp cannot be null");
        Assert.notNull(serverIp, "Server IP cannot be null");
        Assert.notNull(serverHostname, "Server Host Name cannot be null");
        Assert.notNull(context, "Context cannot be null");

        this.timeStamp = timeStamp;
        this.serverIp = serverIp;
        this.serverHostname = serverHostname;
        this.context = context;


        //Optional and depends on which service this originated from and if the request was successful or  caused an error.
        this.user = user;
        this.resourceId = resourceId;
        this.resource = resource;
        this.errorMessage = errorMessage;

    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditMessage)) {
            return false;
        }
        AuditMessage that = (AuditMessage) o;
        return timeStamp.equals(that.timeStamp) &&
                serverIp.equals(that.serverIp) &&
                serverHostname.equals(that.serverHostname) &&
                context.equals(that.context) &&
                user.equals(that.user) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(timeStamp, serverIp, serverHostname, context, user, resourceId, resource, errorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditMessage.class.getSimpleName() + "[", "]")
                .add("timeStamp='" + timeStamp + "'")
                .add("serverIp='" + serverIp + "'")
                .add("serverHostname='" + serverHostname + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add("resourceId='" + resourceId + "'")
                .add("resource=" + resource)
                .add("errorMessage='" + errorMessage + "'")
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the AuditMessage.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        private String timeStamp;
        private String serverIp;
        private String serverHostname;
        private Context context;
        private String userId;
        private User user;
        private String resourceId;
        private LeafResource resource;
        private String errorMessage;


        public static ITimeStamp create() {
            return timeStamp -> serverIp -> serverHostname -> context ->  user -> resourceId -> resource  -> errorMessage ->
                    new AuditMessage(timeStamp, serverIp, serverHostname, context,  user, resourceId, resource,  errorMessage);
        }

        interface ITimeStamp {
            IServerIp withTimeStamp(String timeStamp);
        }

        interface IServerIp {
            IServerHostname withServerIp(String serverIp);
        }

        interface IServerHostname {
            IContext withServerHostname(String serverHostname);
        }

        interface IContext {
            IUser withContext(Context context);

        }


        interface IUser {
            IResourceId withUser(User user);
        }

        interface IResourceId {
            IResource withResourceId(String resourceId);
        }

        interface IResource {
            IErrorMessage withResource(LeafResource resource);
        }

        interface IErrorMessage {
            AuditMessage withErrorMessage(String errorMessage);
        }

    }

}
