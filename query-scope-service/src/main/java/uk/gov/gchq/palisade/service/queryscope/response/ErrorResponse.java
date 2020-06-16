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
package uk.gov.gchq.palisade.service.queryscope.response;

import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the requests stops.  This messaging is constructed and forwarded to the Results
 * Service skipping any services that have not processed the request.  The Results Services will forward this
 * message back to the client.  This should be enough information to explain the issue and possibly suggest
 * what is needed before tying again.
 * This message will be sent to the Results Service where it will be de-seralised into a
 * uk.gov.gchq.palisade.service.results.request.ErrorRequest.  This will then be the starting point for sending an
 * error message back to the client as the response to their request.
 **/
public class ErrorResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final String errorMessage;  //Detailed description of the error in English

    public ErrorResponse(final String token, final String errorMessage) {
        this.token = token;
        this.errorMessage = errorMessage;
    }


    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorResponse)) {
            return false;
        }
        ErrorResponse that = (ErrorResponse) o;
        return token.equals(that.token) &&
                errorMessage.equals(that.errorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, errorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ErrorResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("errorMessage='" + errorMessage + "'")
                .add(super.toString())
                .toString();
    }


    public static class Builder {
        private String token;
        private String errorMessage;

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ErrorResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(errorMessage, "Resources cannot be null");

            return new ErrorResponse(token, errorMessage);
        }
    }

}
