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
package uk.gov.gchq.palisade.service.results.response;

import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * The data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the response frm the Results Service
 * This message will be sent back to the client.
 * From the client's perspective, this is the response to their initial request sent to the Palisade Service
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest.
 * This will provide the necessary information for them to query the data service.
 */
public final class ResultsResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final String queuePointer; //reference to where the data is located

    private ResultsResponse(final String token, final String queuePointer) {
        this.token = token;
        this.queuePointer = queuePointer;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getQueuePointer() {
        return queuePointer;
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
        return token.equals(that.token) &&
                queuePointer.equals(that.queuePointer);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, queuePointer);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResultsResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("queuePointer='" + queuePointer + "'")
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the ResultsRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the ResultsRequest class.
     */
    public static class Builder {
        private String token;
        private String queuePointer;

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder queuePointer(final String queuePointer) {
            this.queuePointer = queuePointer;
            return this;
        }

        public ResultsResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(queuePointer, "Resources cannot be null");
            return new ResultsResponse(token, queuePointer);
        }
    }
}
