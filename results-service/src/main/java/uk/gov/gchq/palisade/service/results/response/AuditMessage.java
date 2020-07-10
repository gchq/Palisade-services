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
package uk.gov.gchq.palisade.service.results.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

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
     * Number of records that are provided for the request Can be zero if this has not been generated yet
     */
    public final long numberOfRecordsReturned;

    /**
     * Number of records that have been processed for the request
     */
    public final long numberOfRecordsProcessed;

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
            final @JsonProperty("numberOfRecordsReturned") long numberOfRecordsReturned,
            final @JsonProperty("numberOfRecordsProcessed") long numberOfRecordsProcessed,
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
        this.numberOfRecordsReturned = numberOfRecordsReturned;
        this.numberOfRecordsProcessed = numberOfRecordsProcessed;
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
        return numberOfRecordsReturned == that.numberOfRecordsReturned &&
                numberOfRecordsProcessed == that.numberOfRecordsProcessed &&
                timeStamp.equals(that.timeStamp) &&
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
        return Objects.hash(timeStamp, serverIp, serverHostname, context, user, resource, rules, numberOfRecordsReturned, numberOfRecordsProcessed, errorMessage);
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
                .add("numberOfRecordsReturned=" + numberOfRecordsReturned)
                .add("numberOfRecordsProcessed=" + numberOfRecordsProcessed)
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
        private long numberOfRecordsReturned;
        private long numberOfRecordsProcessed;
        private String errorMessage;

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditMessage class.
         *
         * @return fully constructed AuditMessage instance.
         */
        public static ITimeStamp create() {
            return timeStamp -> serverIp -> serverHostname -> context -> user -> resource -> rules -> recordsReturned -> recordsApplied -> errorMessage ->
                    new AuditMessage(timeStamp, serverIp, serverHostname, context, user, resource, rules, recordsReturned, recordsApplied, errorMessage);
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
             * Adds the server IP information for the message.
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
             * @return interface {@link IRecordsReturned} for the next step in the build.
             */
            IRecordsReturned withRules(Rules<?> rules);
        }


        /**
         * Adds the number of records that match the request criterion and comply with the restrictions set with
         * the rules.
         */
        interface IRecordsReturned {
            /**
             * Adds the number of records that have been found that meet the criterion.  This can be zero if are no
             * records that meet the criterion or this message is for a step before results-service.
             *
             * @param recordsReturned number of records.
             * @return interface {@link IRecordsProcessed} for the next step in the build.
             */
            IRecordsProcessed withRecordsReturned(long recordsReturned);
        }

        /**
         * Adds the number of records that have been processed
         */
        interface IRecordsProcessed {
            /**
             * Adds the number of records that have been processed.  This can be zero if are no
             * records that have been sent or this message is for a step before results-service
             *
             * @param recordsApplied records processed
             * @return interface {@link IErrorMessage} for the next step in the build.
             */
            IErrorMessage withRecordsProcessed(long recordsApplied);
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
