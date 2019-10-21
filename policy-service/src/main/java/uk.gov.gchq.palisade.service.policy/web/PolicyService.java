package uk.gov.gchq.palisade.service.policy.web;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

@FeignClient("policy-service")
public interface PolicyService {

    @PostMapping(value = "/canAccess", consumes = "application/json", produces = "application/json")
    CanAccessResponse registerDataRequestSync(@RequestBody final CanAccessRequest request);

    @GetMapping(value = "/ping")
    String ping();

    @PostMapping(value = "/getPolicySync", consumes = "application/json", produces = "application/json")
    MultiPolicy getPolicySync(@RequestBody final GetPolicyRequest request);

    @PutMapping(value = "/setResourcePolicySync", consumes = "application/json", produces = "application/json")
    void setResourcePolicySync(@RequestBody final SetResourcePolicyRequest request);

    @PutMapping(value = "/setResourcePolicyAsync", consumes = "application/json", produces = "application/json")
    void setResourcePolicyAsync(final SetResourcePolicyRequest request);

    @PutMapping(value = "/setTypePolicyAsync", consumes = "application/json", produces = "application/json")
    void setTypePolicyAsync(final SetTypePolicyRequest request);

}
