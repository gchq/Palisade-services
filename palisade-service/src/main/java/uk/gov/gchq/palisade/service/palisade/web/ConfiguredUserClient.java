package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", primary = false, url = "${web.client.resource-service}")
public interface ConfiguredUserClient extends ResourceClient { }
