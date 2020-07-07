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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the request stops.  This message is constructed and forwarded to the results-service
 * skipping any other services that have not been preformed.  The results-service will forward this message back
 * to client who should be given enough information to correct the problem before tying again.
 * The technical message will contain information that may help in understanding the issue and so may contain
 * information about the service which should not be made public such as the stack trace of the error.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ErrorResponse {

    /**
     * Technical detail about where/when the error occurred.
     */
    public final String technicalMessage;

    /**
     * Detailed description of the error in english
     */
    public final String errorMessage;

    @JsonCreator
    private ErrorResponse(
            final @JsonProperty("technicalMessage") String technicalMessage,
            final @JsonProperty("errorMessage") String errorMessage) {

        Assert.notNull(technicalMessage, "TechnicalMessage cannot be null");
        Assert.notNull(errorMessage, "ErrorMessage cannot be null");

        this.technicalMessage = technicalMessage;
        this.errorMessage = errorMessage;
    }


    /**
     * Builder class for the creation of instances of the ErrorResponse.  This is a variant of the Fluent Builder
     * which will use the String for the components in the build.
     */
    public static class Builder {
        public static ITechMessage create() {
            return techMessage -> errorMessage ->
                    new ErrorResponse(techMessage, errorMessage);
        }

        /**
         * Adds the technical information about the issue to the creation of the message.
         */
        interface ITechMessage {
            /**
             * Adds the technical information about the issue to the message.
             *
             * @param technicalMessage technical error information
             * @return interface {@link IErrorMessage} for the next step in the build.
             */
            IErrorMessage withTechnicalMessage(String technicalMessage);
        }

        /**
         * Adds the human readable information about the issue to the message.
         */
        interface IErrorMessage {
            /**
             * Adds the error message.
             *
             * @param errorMessage error message
             * @return class {@link ErrorResponse} for the completed build.
             */
            ErrorResponse withErrorMessage(String errorMessage);
        }
    }
}
