package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", fallback = ConfiguredUserClient.class)
public interface EurekaUserClient extends ResourceClient { }
