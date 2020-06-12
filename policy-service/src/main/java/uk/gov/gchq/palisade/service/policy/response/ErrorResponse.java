package uk.gov.gchq.palisade.service.policy.response;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the requests stops.  This messaging is constructed and forwarded to the Results
 * Service skipping any services that have not been preformed.  Results services will forward this message back
 * to client who should be given enough information to correct the problem before tying again.
 */
public class ErrorResponse {

    //This version of the message may not necessarily be sanitised.  It will need to pass information to
    // the Audit Service that may technical details as to the reason for the failure.  This can then be used to
    // resolve the issue if it was technical in natured.
    private String technicalMessage;

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String errorMessage;  //Detailed description of the error in english
}
