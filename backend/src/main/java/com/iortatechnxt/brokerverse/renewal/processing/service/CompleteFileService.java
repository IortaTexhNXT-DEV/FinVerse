package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.domain.UploadScope;
import com.iortatechnxt.brokerverse.renewal.domain.UploadScopeRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import java.time.Clock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The complete-file scope of a dispositioned upload (FR-RN-060 R2; BRD 3.004.4): when the user
 * declares the file complete for an expiry range and a unit, the renewals of that range and unit
 * that are not in the file are tagged Not for Renewal with reason "Non-renewable Accounts" (source
 * UPLOAD) and kept out of processing. The tag is applied once per upload and can be re-opened.
 */
@Service
@Transactional
public class CompleteFileService {

  private static final int MAX_ROWS = 100_000;
  private static final Set<RenewalStage> TAGGABLE =
      EnumSet.of(RenewalStage.UNASSIGNED, RenewalStage.FOR_DISPOSITION);

  private final BulkService bulk;
  private final UploadScopeRepository scopes;
  private final RenewalCandidateRepository candidates;
  private final RenewalDispositions dispositions;
  private final RenewalFlow flow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param bulk bulk uploads
   * @param scopes declared scopes
   * @param candidates renewals
   * @param dispositions disposition history
   * @param flow workflow
   * @param audit audit trail
   * @param clock clock
   */
  public CompleteFileService(
      BulkService bulk,
      UploadScopeRepository scopes,
      RenewalCandidateRepository candidates,
      RenewalDispositions dispositions,
      RenewalFlow flow,
      AuditTrailService audit,
      Clock clock) {
    this.bulk = bulk;
    this.scopes = scopes;
    this.candidates = candidates;
    this.dispositions = dispositions;
    this.flow = flow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Applies the complete-file scope of a committed dispositioned upload.
   *
   * @param companyId company
   * @param jobId upload job
   * @param range expiry range and unit
   * @return the scope with the number of renewals tagged
   */
  public UploadScope apply(Long companyId, Long jobId, UploadScope.Range range) {
    BulkJob job = bulk.job(jobId);
    if (!Objects.equals(job.getCompanyId(), companyId)
        || !DispositionUploadHandler.CODE.equals(job.getHandlerCode())) {
      throw new BusinessRuleException(
          "RNW_SCOPE_JOB", "Upload " + job.getJobNo() + " is not a dispositioned file");
    }
    if (job.getStatus() != BulkJobStatus.COMPLETED) {
      throw new BusinessRuleException(
          "RNW_SCOPE_JOB", "Upload " + job.getJobNo() + " is not committed yet");
    }
    if (range.from() == null
        || range.to() == null
        || range.unit() == null
        || range.unit().isBlank()) {
      throw new BusinessRuleException(
          "RNW_SCOPE_RANGE", "Enter the expiry range and the unit of the complete file");
    }
    if (scopes.findByJobNo(job.getJobNo()).isPresent()) {
      throw new BusinessRuleException(
          "RNW_SCOPE_APPLIED", "The scope of upload " + job.getJobNo() + " is already applied");
    }
    Set<String> inFile = referencesOf(job);
    List<RenewalCandidate> missing =
        candidates.findByCompanyIdAndOwnerUnitAndStageIn(companyId, range.unit(), TAGGABLE).stream()
            .filter(c -> !c.getExpiryDate().isBefore(range.from()))
            .filter(c -> !c.getExpiryDate().isAfter(range.to()))
            .filter(c -> !inFile.contains(c.getRenewalRef()))
            .toList();
    String note = "Not in the complete dispositioned file " + job.getJobNo();
    for (RenewalCandidate c : missing) {
      dispositions.record(
          c,
          new CurrentDisposition(
              RenewalDisposition.NOT_FOR_RENEWAL,
              RenewalCodes.REASON_NON_RENEWABLE,
              DispositionSource.UPLOAD,
              note,
              null),
          null,
          null);
      flow.system(c, "system_not_for_renewal", note);
    }
    UploadScope scope =
        scopes.save(
            new UploadScope(companyId, job.getJobNo(), UploadScope.DISPOSITION, range, true));
    scope.applied(missing.size(), clock.instant());
    audit.record(
        RenewalCodes.ENTITY_UPLOAD,
        job.getJobNo(),
        AuditAction.UPDATE,
        "Complete file for "
            + range.unit()
            + " "
            + range.from()
            + " to "
            + range.to()
            + ": "
            + missing.size()
            + " renewal(s) tagged Not for Renewal");
    return scope;
  }

  private Set<String> referencesOf(BulkJob job) {
    Set<String> refs = new HashSet<>();
    for (BulkRowRecord row : bulk.rows(job.getId(), null, PageRequest.of(0, MAX_ROWS))) {
      String ref = bulk.values(row).get(DispositionUploadHandler.REFERENCE);
      if (ref != null) {
        refs.add(ref.strip());
      }
      if (row.getResultRef() != null) {
        refs.add(row.getResultRef());
      }
    }
    return refs;
  }
}
