package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "policy-service", url = "${web.client.policy-service}")
public interface ConfiguredPolicyClient extends PolicyClient {
}
