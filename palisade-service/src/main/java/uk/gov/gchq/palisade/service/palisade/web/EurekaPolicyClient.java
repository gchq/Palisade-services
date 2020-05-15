package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "policy-service", fallback = ConfiguredAuditClient.class)
public interface EurekaPolicyClient extends PolicyClient {
}
