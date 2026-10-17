package com.iortatechnxt.brokerverse.renewal.epolicy.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_EPOLICY_NUMBERS} (FRRN.033.05): the e-policy numbers of several renewal
 * accounts, each matched on the reference number; a number already held by another active account
 * is refused into the exception report, the others update the account with the upload batch, time
 * and user.
 */
@Component
public class EpolicyNumbersHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_EPOLICY_NUMBERS";

  private static final String REFERENCE = "Reference Number";
  private static final String POLICY = "Policy Number";

  private final RenewalCandidateRepository candidates;
  private final EpolicyEligibility eligibility;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the handler.
   *
   * @param candidates renewals
   * @param eligibility eligibility and duplicates
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public EpolicyNumbersHandler(
      RenewalCandidateRepository candidates,
      EpolicyEligibility eligibility,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.candidates = candidates;
    this.eligibility = eligibility;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - e-policy numbers";
  }

  @Override
  public String permission() {
    return Permission.RNW_PROCESS.name();
  }

  @Override
  public String filledBy() {
    return "The Processing Officer, from the e-policies issued by the insurers";
  }

  @Override
  public String uploadPath() {
    return "Renewal > E-Policies, button Upload E-Policy Numbers";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            REFERENCE, "Renewal reference number of the account", "RNW-2026-000101"),
        BulkColumn.required(POLICY, "E-policy number issued by the insurer", "MC-PC-2026-0012345"));
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("UPDATED");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(POLICY);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text(REFERENCE) == null) {
      errors.add("Reference Number is required");
    }
    if (row.text(POLICY) == null) {
      errors.add("E-Policy Number is required");
    }
    if (!errors.isEmpty()) {
      return errors;
    }
    Optional<RenewalCandidate> c = candidate(row, context);
    if (c.isEmpty()) {
      errors.add("Reference Number does not exist");
      return errors;
    }
    String problem = eligibility.problem(c.get(), row.text(POLICY));
    if (problem != null) {
      errors.add(problem);
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    RenewalCandidate c = candidate(row, context).orElseThrow();
    String no = row.text(POLICY).strip();
    c.getPlacement().epolicy(no, context.jobNo(), currentUser.username(), clock.instant());
    candidates.save(c);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "E-policy number " + no + " (upload " + context.jobNo() + ")");
    return new BulkOutcome(c.getRenewalRef() + ": " + no, "UPDATED");
  }

  private Optional<RenewalCandidate> candidate(BulkRow row, BulkContext context) {
    return candidates.findByCompanyIdAndRenewalRef(
        context.companyId(), row.text(REFERENCE).strip());
  }
}
