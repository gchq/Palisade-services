package uk.gov.gchq.palisade.service.palisade.request;

import java.util.Map;

/**
 * Contains the request with the User, and the resource metadata
 */
public class ResourceRequest {

   // private User user;
    private String requestId;           // unique identifier for this specific request.  Was a RequestId object now  a String
    private Map context;    // relevant  information about the request .  Was a Context object now is a Map.
    private Map resources;  //   ??  How does that fit with LeafResources?

}
