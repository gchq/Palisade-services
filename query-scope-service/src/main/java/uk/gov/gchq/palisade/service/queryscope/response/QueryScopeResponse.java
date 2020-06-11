package uk.gov.gchq.palisade.service.queryscope.response;

import uk.gov.gchq.palisade.service.queryscope.response.common.domain.ResourceMetadata;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.User;

import java.util.Map;

/**
 * This is the message that will be sent from the QueryScopeService to the Results Service
 * It is, therefore a Response from the QueryScopeService and a Request into the Results Service
 * Note there are two classes of this type:
 * uk.gov.gchq.palisade.service.queryscope.requestResponse.QueryScopeResponse
 * uk.gov.gchq.palisade.service.results.request.QueryScopeResponse
 */
public class QueryScopeResponse {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private User user;  //User replaces the user id from the original request.  This is also an indication that the User is in the system.
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.

    private Map<String, ResourceMetadata> metadata;  //this is a redacted list of metadata
}
