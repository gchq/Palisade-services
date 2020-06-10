package uk.gov.gchq.palisade.service.user.request;

import java.util.Map;

/**
 * Represents the original data that has been sent from the client for a request.
 * Note there are two classes of the same type:
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest
 * uk.gov.gchq.palisade.service.user.request.OriginalRequest
 * This is the request message than is sent from PalisadeEntryPointService to UserService
 */
public class OriginalRequest {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String userID;  //unique identifier for the user
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.
}