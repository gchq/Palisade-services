package uk.gov.gchq.palisade.service.palisade.request;

import java.util.Map;

/**
 * Represents the original data that has been sent from the client to access data.
 * Note there is another class in the user service that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.OriginalRequest is the input for the User Service
 *
 */
public class OriginalRequest {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header when needed

    private String userID;  //unique identifier for the user
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.
}
