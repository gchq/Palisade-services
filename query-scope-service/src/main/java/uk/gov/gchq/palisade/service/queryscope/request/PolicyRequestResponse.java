package uk.gov.gchq.palisade.service.queryscope.request;

//This is the message that will be sent from the PolicyService to the QueryScopeService
//It is, therefore a Response from the PolicyService and a Request into the QueryScopeService



import uk.gov.gchq.palisade.service.queryscope.requestresponse.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.queryscope.requestresponse.common.domain.Rule;
import uk.gov.gchq.palisade.service.queryscope.requestresponse.common.domain.User;

import java.util.Map;

/**
 * This is the message that will be sent from the PolicyService to the QueryScopeService
 * It is, therefore a Response from the PolicyService and a Request into the QueryScopeService
 * Note there are two classes of this type:
 * uk.gov.gchq.palisade.service.policy.requestResponse.PolicyRequestResponse
 * uk.gov.gchq.palisade.service.queryscope.request.PolicyRequestResponse
 */
public class PolicyRequestResponse {


    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private User user;  //User replaces the user id from the original request.  This is also an indication that the User is in the system.
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, String> context;    // relevant  information about the request.  Was a Context object now a Map.

  //  private Map<String, ResourceMetadata> metadata;  //this is a filtered set as there can be resources that may have been removed from the orignal set
    // from the map in the ResourceRequestResponse by the policy service
    private Map<String, Rule> rules; // holds all of the rules applicable to this request
}
