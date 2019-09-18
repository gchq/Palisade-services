package uk.gov.gchq.palisade.service.palisade.service;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class ResourceService implements Service {

    private final ResourceClient client;
    private final Executor executor;

    public ResourceService(final ResourceClient resourceClient, final Executor executor) {
        this.client = resourceClient;
        this.executor = executor;
    }

    public CompletionStage<Map<LeafResource, ConnectionDetail>> getResourcesById(final GetResourcesByIdRequest resource) {
        return CompletableFuture.supplyAsync(() -> this.client.getResourcesById(resource), this.executor);
    }
}
