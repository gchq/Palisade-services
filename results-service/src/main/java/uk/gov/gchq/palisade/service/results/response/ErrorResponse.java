package uk.gov.gchq.palisade.service.results.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the requests stops.  This messaging is constructed and forwarded to the Results
 * Service skipping any services that have not been preformed.  Results services will forward this message back
 * to client who should be given enough information to correct the problem before tying again.
 * The technical information will contain information that may help in understanding the issue and so may contain
 * information about the service which should not be made public such as the stack trace of the error.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ErrorResponse {


    public final String technicalMessage; //Technical detail about where/when the error occurred.

    public final String errorMessage;  //Detailed description of the error in english

    @JsonCreator
    private ErrorResponse(
            final @JsonProperty("technicalMessage") String technicalMessage,
            final @JsonProperty("errorMessage") String errorMessage) {

        Assert.notNull(technicalMessage, "TechnicalMessage cannot be null");
        Assert.notNull(errorMessage, "ErrorMessage cannot be null");

        this.technicalMessage = technicalMessage;
        this.errorMessage = errorMessage;
    }



    public static class Builder {
        public static ITechMessage create() {
            return techMessage -> errorMessage ->
                    new ErrorResponse(techMessage, errorMessage);
        }

        interface ITechMessage {
            IErrorMessage withTechnicalMessage(String technicalMessage);
        }

        interface IErrorMessage {
            ErrorResponse withErrorMessage(String errorMessage);
        }
    }
}