package uk.gov.gchq.palisade.service.queryscope.requestresponse.common;

import java.net.URL;
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
