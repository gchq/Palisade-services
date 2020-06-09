package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.service.palisade.request.common.domain.User;

import java.util.Map;

/**
 * Contains the information for a request and the User has been indetified in the system.
 */
public class UserRequest {

    private User user;  //User replaces the user id from the original request
    private String resourceID;
    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.  Should we name it token?
    //the concept of the unique identifier for the request at each step is to be replaced by data that can be pulled from the header.  Is that correct?

  private Map<String, String> context;    // relevant  information about the request.  Was a Context object now  a Map.




}
