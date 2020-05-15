package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "audit-service", fallback = ConfiguredAuditClient.class)
public interface EurekaAuditClient extends AuditClient {
}
