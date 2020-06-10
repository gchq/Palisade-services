package uk.gov.gchq.palisade.service.palisade.request;

import java.util.Map;

/**
 * Represents the original data that has been sent from the client for a request.
 * Note there is another class of the same type in the user service
 * uk.gov.gchq.palisade.service.user.request.OriginalRequest
 */
public class OriginalRequest {

    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.  Should we change the name to Token
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String userID;  //unique identifier for the user
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.



}
