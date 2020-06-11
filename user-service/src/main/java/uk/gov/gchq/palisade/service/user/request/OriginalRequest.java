package uk.gov.gchq.palisade.service.user.request;

import java.util.Map;

/**
 * Represents the original data that has been sent from the client to access data.
 * This is being used as input for the User Service
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.OriginalRequest is the input for the User Service
 */
public class OriginalRequest {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String userID;  //unique identifier for the user
    private String resourceID;  //the resource that that is being asked to access
    private String contextJson;  // represents the context information as a Json string of a Map<String, String>
    // This needs to be passed on but it is not used in this service so it remains the Json string that was given
    // to the User Service
}