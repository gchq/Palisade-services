package uk.gov.gchq.palisade.service.resource.requestResponse.common;

import uk.gov.gchq.palisade.service.ConnectionDetail;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * Contains the relevant information about a resource.
 */
public class ResourceMetadata {
    private String schema;  // The schema for this resource
    private String resourceId;   //the unique reference for this resource
    URL urlDataService;  //replaces the ConnectionDetail this specifies the connection for the resource.
    SortedSet<String> parent;  //contains the hierarchy of resources relevant to the this resource





}
