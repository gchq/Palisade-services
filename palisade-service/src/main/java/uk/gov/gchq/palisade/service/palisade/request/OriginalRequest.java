package uk.gov.gchq.palisade.service.palisade.request;

import java.util.Map;

/**
 * Represents the data that has been sent from the client for a request.
 */
public class OriginalRequest {

    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.  Should we name it token?
    private String userID;  //unique identifier for the user
    private String resourceID;
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now  a Map.



}
