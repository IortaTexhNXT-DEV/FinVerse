package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.AcceptanceMethod;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.UploadValues;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_ACCEPTANCE} (FR-RN-084): acceptances received by e-mail list or paid, one
 * row per renewal with the evidence reference (e-mail subject and date, or payment reference).
 */
@Component
public class AcceptanceBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_ACCEPTANCE";

  private static final String REFERENCE = "Renewal Reference";
  private static final String METHOD = "Method";
  private static final String EVIDENCE = "Evidence Reference";
  private static final String ACCEPTED_ON = "Accepted On";
  private static final String REMARKS = "Remarks";

  private final RenewalCandidateRepository candidates;
  private final AcceptanceService acceptances;

  /**
   * Creates the handler.
   *
   * @param candidates renewals
   * @param acceptances acceptances
   */
  public AcceptanceBulkHandler(
      RenewalCandidateRepository candidates, AcceptanceService acceptances) {
    this.candidates = candidates;
    this.acceptances = acceptances;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - client acceptances";
  }

  @Override
  public String permission() {
    return Permission.RNW_ACCEPT.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(REFERENCE, "Renewal reference", "RNW-2027-000001"),
        BulkColumn.required(METHOD, "E-mail or Payment", "Payment"),
        BulkColumn.required(EVIDENCE, "E-mail reference or payment reference", "OR-2027-001122"),
        new BulkColumn(ACCEPTED_ON, "Date accepted", false, BulkColumn.Type.DATE, "2027-08-20"),
        BulkColumn.optional(REMARKS, "Remarks", ""));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    Optional<RenewalCandidate> c = candidate(row, context);
    if (c.isEmpty()) {
      errors.add("Renewal " + row.text(REFERENCE) + " does not exist");
    } else if (c.get().getStage() != RenewalStage.RA_SENT) {
      errors.add("Renewal " + c.get().getRenewalRef() + " is not awaiting a response");
    }
    if (method(row.text(METHOD)) == null) {
      errors.add("Method " + row.text(METHOD) + " is not valid (E-mail or Payment)");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    RenewalCandidate c = candidate(row, context).orElseThrow();
    AcceptanceMethod method = method(row.text(METHOD));
    acceptances.accept(
        context.companyId(),
        c.getRenewalRef(),
        new AcceptanceService.Input(
            method, null, row.text(EVIDENCE), row.date(ACCEPTED_ON), row.text(REMARKS), false),
        "UPLOAD");
    return c.getRenewalRef();
  }

  private Optional<RenewalCandidate> candidate(BulkRow row, BulkContext context) {
    String ref = row.text(REFERENCE);
    return ref == null
        ? Optional.empty()
        : candidates.findByCompanyIdAndRenewalRef(context.companyId(), ref.strip());
  }

  /** Bulk acceptances carry a reference: e-mail (by reference) or payment. */
  private static AcceptanceMethod method(String value) {
    if (UploadValues.names(value, "PAYMENT", "Payment")) {
      return AcceptanceMethod.PAYMENT;
    }
    return UploadValues.names(value, "EMAIL", "E-mail") ? AcceptanceMethod.EMAIL : null;
  }
}
