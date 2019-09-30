package uk.gov.gchq.palisade.service.resource;

import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.request.*;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.util.Map;

public class DefaultController implements ResourceClient {

    @Override
    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesById(GetResourcesByIdRequest request) {
        return null;
    }

    @Override
    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(GetResourcesByResourceRequest request) {
        return null;
    }

    @Override
    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesByType(GetResourcesByTypeRequest request) {
        return null;
    }

    @Override
    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(GetResourcesBySerialisedFormatRequest request) {
        return null;
    }

    @Override
    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    public Boolean addResource(AddResourceRequest request) {
        return null;
    }
}
