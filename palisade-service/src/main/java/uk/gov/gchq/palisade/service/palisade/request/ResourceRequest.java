package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.request.common.domain.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the request with the User, and the resource metadata
 */
public class ResourceRequest {

    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.  Should we change the name to Token
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private User user;  //User replaces the user id from the original request.  This is also an indication that the User is in the system.
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.


//new stuff
    private String type;
    private String serialisedFormat;
    private ConnectionDetail connectionDetail;
    //private ParentResource parent;
    //sorted collection of parent resource id
    Collection<String> parent;
    private Map<String, Object> attributes = new HashMap();


}
