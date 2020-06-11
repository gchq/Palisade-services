package uk.gov.gchq.palisade.service.resource.response;

import uk.gov.gchq.palisade.service.resource.response.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.resource.response.common.domain.User;

import java.util.Map;


/**
 * This is the message that will be sent from the ResourceService to the PolicyService
 * It is, therefore a Response from the ResourceService and a Request into the PolicyService
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.resource.response.ResourceResponse is the output from the Resource Service
 * uk.gov.gchq.palisade.service.policy.request.ResourceResponse is the input for the Policy Service
 *
 */
public class ResourceResponse {


    private String requestId; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.  Should we change the name to Token
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

    private String userJson;  //represents the user data as a Json string of type User.
    private String resourceID;  //the resource that that is being asked to access
    private Map<String, ResourceMetadata> resources;
    private String contextJson;  // represents the context information as a Json string of a Map<String, String>}
}
