package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.service.palisade.request.common.domain.User;

import java.util.Map;

/**
 * Contains the information for a request where the User has been identified in the system.  This weill only be created if the user does exists
 */
public class UserRequest {


    //Th
    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the term Token in the diagram Logical view of Palisade.  Should we change the name to Token
    //the concept of a unique identifier for each transaction is to pulled from the header

    private User user;  //User replaces the user id from the original request.  This is also an indication that the User is in the system.
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.





}
