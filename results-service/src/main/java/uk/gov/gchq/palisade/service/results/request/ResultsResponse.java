/*
 * Copyright 2020 Crown Copyright
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
package uk.gov.gchq.palisade.service.results.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the output for results-service which is the response to the original request from the client.
 * This is the last in the sequence.   It will contain the response that is to be sent back to the client based on the
 * original request that was sent into palisade-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ResultsResponse {

    /**
     * {@link String} reference to where the data is located
     */
    public final String queuePointer;

    private ResultsResponse(final @JsonProperty("queuePointer") String queuePointer) {
        this.queuePointer = queuePointer;
    }



    /**
     * Builder class for the creation of instances of the ResultsResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * ResultsResponse class.
         *
         * @return interface  {@link IQueuePointer} for the next step in the build.
         */
        public static IQueuePointer create() {
            return ResultsResponse::new;
        }

        /**
         * Adds the queue pointer, a reference for the results for the request.
         */
        interface IQueuePointer {

            /**
             * Adds the queue pointer to the message.
             *
             * @param queuePointer reference to the results for the request.
             * @return class {@link ResultsResponse} for the completed class from the builder.
             */
            ResultsResponse withQueuePointer(String queuePointer);

        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResultsResponse)) {
            return false;
        }
        ResultsResponse that = (ResultsResponse) o;
        return queuePointer.equals(that.queuePointer);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(queuePointer);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResultsResponse.class.getSimpleName() + "[", "]")
                .add("queuePointer='" + queuePointer + "'")
                .add(super.toString())
                .toString();
    }
}
