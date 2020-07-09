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
package uk.gov.gchq.palisade.service.queryscope.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Audit information for a request provided by each service.
 * Each individual service sends a record to the Audit Service for every request that it receives.
 * The components of the message will differ depending on which service has sent the data and if the processing was
 * successful or not.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AuditMessage {

    /**
     * Time when the service processed the request.
     */
    public final String timeStamp;

    /**
     * The server IP address for the service
     */
    public final String serverIp;

    /**
     * The server host name for the service
     */
    public final String serverHostname;

    /**
     * The context for the client's request.  This contains the information about the user in the context of the
     * request.
     */
    public final Context context;

    /**
     * The user corresponding to the given user ID
     */
    public final User user;

    /**
     * The resource that is being requested to access
     */
    public final LeafResource resource;

    /**
     * The rules and restrictions that are in place for accessing the resource
     */
    public final Rules<?> rules;

    /**
     * Error message if there was an issue with the request
     */
    public final String errorMessage;

    @SuppressWarnings("checkstyle:parameterNumber")
    @JsonCreator
    private AuditMessage(
            final @JsonProperty("timeStamp") String timeStamp,
            final @JsonProperty("serverIp") String serverIp,
            final @JsonProperty("serverHostname") String serverHostname,
            final @JsonProperty("context") Context context,
            final @JsonProperty("user") User user,
            final @JsonProperty("resource") LeafResource resource,
            final @JsonProperty("rules") Rules<?> rules,
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
        this.resource = resource;
        this.rules = rules;
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
                resource.equals(that.resource) &&
                rules.equals(that.rules) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(timeStamp, serverIp, serverHostname, context, user, resource, rules, errorMessage);
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
                .add("resource=" + resource)
                .add("rules=" + rules)
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
        private User user;
        private LeafResource resource;
        private Rules<?> rules;
        private String errorMessage;


        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditMessage class.
         *
         * @return fully constructed {@link AuditMessage} instance
         */
        public static ITimeStamp create() {
            return timeStamp -> serverIp -> serverHostname -> context -> user -> resource -> rules -> errorMessage ->
                    new AuditMessage(timeStamp, serverIp, serverHostname, context, user, resource, rules, errorMessage);
        }

        /**
         * Adds the timestamp information for the message.
         */
        interface ITimeStamp {

            /**
             * Adds the timestamp for the message.
             *
             * @param timeStamp when the message was created.
             * @return interface  {@link IServerIp} for the next step in the build.
             */
            IServerIp withTimeStamp(String timeStamp);
        }

        /**
         * Adds the server IP information for the message.
         */
        interface IServerIp {

            /**
             * Adds the server IP information to the message.
             *
             * @param serverIp where the message was created.
             * @return interface  {@link IServerHostname} for the next step in the build.
             */
            IServerHostname withServerIp(String serverIp);
        }

        /**
         * Adds the server host name for the message.
         */
        interface IServerHostname {
            /**
             * Adds the server host name for where the message was created.
             *
             * @param serverHostname server host name.
             * @return interface  {@link IContext} for the next step in the build.
             */
            IContext withServerHostname(String serverHostname);
        }

        /**
         * Adds the user context information to the message.
         */
        interface IContext {
            /**
             * Adds the user's information for the request.
             *
             * @param context the user information for this request.
             * @return interface  {@link IUser} for the next step in the build.
             */
            IUser withContext(Context context);

        }

        /**
         * Adds the user information to the message.
         */
        interface IUser {
            /**
             * Adds the user to the message.  This can be null if the user ID is provided.
             *
             * @param user making the request.
             * @return interface {@link IResource} for the next step in the build.
             */
            IResource withUser(User user);
        }


        /**
         * Adds the information about the resource that is being requested to access
         */
        interface IResource {
            /**
             * Adds the resource.  This can be null if the resource ID is provided.
             *
             * @param resource for the request
             * @return interface {@link IRules} for the next step in the build.
             */
            IRules withResource(LeafResource resource);
        }

        /**
         * Adds the restrictions that are to enforced for the resource that is being requested to access
         */
        interface IRules {
            /**
             * Adds the rules for the request.  This can be null instances where the message is for a step before
             * the policy-service.
             *
             * @param rules for the request.
             * @return interface {@link IErrorMessage} for the next step in the build.
             */
            IErrorMessage withRules(Rules<?> rules);
        }

        /**
         * Adds the error message if there was an issue with processing the request
         */
        interface IErrorMessage {
            /**
             * Adds the error message that has been produced processing the request.  This can be null is there was no
             * issue with the processing of the request.
             *
             * @param errorMessage error message.
             * @return class {@link AuditMessage} class this builder is set-up to create.
             */
            AuditMessage withErrorMessage(String errorMessage);
        }

    }

}
