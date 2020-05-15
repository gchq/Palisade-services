package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "resource-service", primary = false, url = "${web.client.resource-service}")
public interface ConfiguredResourceClient extends ResourceClient { }
