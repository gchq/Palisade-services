package uk.gov.gchq.palisade.service.user.response;


import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import java.util.Map;


/**
 * Contains the information for a request where the User has been identified in the system.
 * This will only be created if the user does exists.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.user.response.UserResponse  is the output of the User Service
 * uk.gov.gchq.palisade.service.resource.request.UserResponse is the input of the Resource Service
 */
public class UserResponse {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private User user;  //User replaces the user id from the original request.  This is also an indication that the User is in the system.

    private String resourceID;  //the resource that that is being asked to access
    private String contextJson;  // represents the context information as a Json string of a Map<String, String>
    // This needs to be passed on but it is not used in this service so it remains the Json string that was given
    // to the User Service
}