package uk.gov.gchq.palisade.service.results.response;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the requests stops.  This messaging is constructed and forwarded to the Results
 * Service skipping any services that have not been preformed.  Results services will forward this message back
 * to client who should be given enough information to correct the problem before tying again.
 */
public class ErrorResponse {

    //This version of the message is sanitised.  It will have a reference that can correlate an entry in the logs,
    // but  all technical information and any sensitive information is removed.

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String errorMessage;  //Detailed description of the error in english
}
