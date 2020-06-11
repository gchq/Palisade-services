package uk.gov.gchq.palisade.service.policy.response;

//This is the message that will be sent from the PolicyService to the QueryScopeService
//It is, therefore a Response from the PolicyService and a Request into the QueryScopeService

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.policy.response.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.policy.response.common.domain.Rule;

import java.util.Map;

/**
 * This is the message that will be sent from the PolicyService to the QueryScopeService
 * It is, therefore a Response from the PolicyService and a Request into the QueryScopeService
 * Note there are two classes of this type:
 * uk.gov.gchq.palisade.service.policy.response.PolicyResponse
 * uk.gov.gchq.palisade.service.queryscope.request.PolicyResponse
 */
public class PolicyResponse {


    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String userJson;  //represents the user data as a Json string of type User.
    private String contextJson;  // represents the context information as a Json string of a Map<String, String>}

    private Map<String, ResourceMetadata> metadata;  //this is a filtered set as there can be resources that may have been removed from the orignal set
    private Map<String, Rule> rules; // holds all of the rules applicable to this request

}
