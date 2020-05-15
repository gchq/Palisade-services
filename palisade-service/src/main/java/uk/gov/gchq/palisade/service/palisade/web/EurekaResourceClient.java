package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "resource-service", fallback = ConfiguredResourceClient.class)
public interface EurekaResourceClient extends ResourceClient { }
