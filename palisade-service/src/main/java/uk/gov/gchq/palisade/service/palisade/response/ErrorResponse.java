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
package uk.gov.gchq.palisade.service.palisade.response;


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

    private final String errorMessage;  //Detailed description of the error in English

    public ErrorResponse( final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Generated
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorResponse)) {
            return false;
        }
        ErrorResponse that = (ErrorResponse) o;
        return errorMessage.equals(that.errorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(errorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ErrorResponse.class.getSimpleName() + "[", "]")
                .add("errorMessage='" + errorMessage + "'")
                .add(super.toString())
                .toString();
    }
}
