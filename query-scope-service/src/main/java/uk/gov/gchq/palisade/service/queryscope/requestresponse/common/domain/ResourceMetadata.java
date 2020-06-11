package uk.gov.gchq.palisade.service.queryscope.requestresponse.common.domain;

import java.net.URL;
import java.util.SortedSet;

/**
 * Contains the relevant information about a resource after the policy have been implemented
 * No information about the connector, resource hierarchy, the schema only show what has not been redacted
 */
public class ResourceMetadata {
    private String schema;  // This will be a redacted version of the schema for this resource
    private String resourceId;   //the unique reference for this resource

}
