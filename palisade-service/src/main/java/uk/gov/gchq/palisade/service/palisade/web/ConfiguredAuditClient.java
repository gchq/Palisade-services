package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "audit-service", url = "${web.client.audit-service}")
public interface ConfiguredAuditClient extends AuditClient {
}
