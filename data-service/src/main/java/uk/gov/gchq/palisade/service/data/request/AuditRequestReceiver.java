package uk.gov.gchq.palisade.service.data.request;

import uk.gov.gchq.palisade.reader.common.AuditRequestCompleteReceiver;
import uk.gov.gchq.palisade.reader.request.AuditRequest.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.data.service.AuditService;

public class AuditRequestReceiver implements AuditRequestCompleteReceiver {

    AuditService auditService;

    public AuditRequestReceiver(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void receive(final ReadRequestCompleteAuditRequest readRequestCompleteAuditRequest) {
        auditService.audit(AuditRequest.ReadRequestCompleteAuditRequest.create(readRequestCompleteAuditRequest.getOriginalRequestId())
                .withUser(readRequestCompleteAuditRequest.user)
                .withLeafResource(readRequestCompleteAuditRequest.leafResource)
                .withContext(readRequestCompleteAuditRequest.context)
                .withRulesApplied(readRequestCompleteAuditRequest.rulesApplied)
                .withNumberOfRecordsReturned(readRequestCompleteAuditRequest.numberOfRecordsReturned)
                .withNumberOfRecordsProcessed(readRequestCompleteAuditRequest.numberOfRecordsProcessed));
    }
}
