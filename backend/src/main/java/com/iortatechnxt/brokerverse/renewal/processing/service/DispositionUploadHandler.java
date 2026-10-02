package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPath;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.UploadValues;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_DISPOSITION_UPLOAD} (FR-RN-060; BRRN.018): dispositions made outside the
 * system, keyed by the renewal reference. Each valid row writes a disposition of source UPLOAD and
 * moves the renewal on by its disposition - For Renewal to processing, For Quotation and For
 * Proposal to the New Business path, Not for Renewal to the letter step, Lost Business to closure.
 * The complete-file tag of the renewals missing from the file is applied afterwards from the
 * Processing Worklist.
 */
@Component
public class DispositionUploadHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_DISPOSITION_UPLOAD";

  /** Column of the renewal reference. */
  public static final String REFERENCE = "Renewal Reference";

  private static final String DISPOSITION = "Disposition";
  private static final String REASON = "Reason";
  private static final String NEW_INVOICE = "New Invoice No";
  private static final String REMARKS = "Remarks";
  private static final int MAX_REMARKS = 200;
  private static final Set<RenewalStage> UPDATABLE =
      EnumSet.of(RenewalStage.UNASSIGNED, RenewalStage.FOR_DISPOSITION, RenewalStage.FOR_TL_REVIEW);

  private final RenewalCandidateRepository candidates;
  private final RenewalDispositions dispositions;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final OpsInvoiceRepository invoices;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the handler.
   *
   * @param candidates renewals
   * @param dispositions disposition history
   * @param flow workflow
   * @param parameters parameters
   * @param invoices ledger invoices
   * @param lovs lists of values
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public DispositionUploadHandler(
      RenewalCandidateRepository candidates,
      RenewalDispositions dispositions,
      RenewalFlow flow,
      RenewalParameters parameters,
      OpsInvoiceRepository invoices,
      LovService lovs,
      Clock clock) {
    this.candidates = candidates;
    this.dispositions = dispositions;
    this.flow = flow;
    this.parameters = parameters;
    this.invoices = invoices;
    this.lovs = lovs;
    this.clock = clock;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - dispositioned file";
  }

  @Override
  public String permission() {
    return Permission.RNW_UPLOAD.name();
  }

  @Override
  public String filledBy() {
    return "Account officers dispositioning their renewals";
  }

  @Override
  public String uploadPath() {
    return "Renewal > My Dispositions, button Upload Dispositions";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(REFERENCE, "Renewal reference", "RNW-2027-000001").master("renewal"),
        BulkColumn.required(DISPOSITION, "Disposition of the renewal", "For Renewal")
            .values(
                Arrays.stream(RenewalDisposition.values())
                    .map(RenewalDisposition::label)
                    .toArray(String[]::new)),
        BulkColumn.optional(REASON, "Reason for Not for Renewal (code or label)", "")
            .when("Disposition is Not for Renewal")
            .lov(RenewalCodes.LOV_NONRENEWAL_REASON),
        BulkColumn.optional(NEW_INVOICE, "New invoice number", "")
            .when("the reason is Booked to New Invoice"),
        BulkColumn.optional(REMARKS, "Remarks", "Client confirmed")
            .format("Text, at most 200 characters"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    Optional<RenewalCandidate> found = candidate(row, context);
    if (found.isEmpty()) {
      errors.add("Renewal " + row.text(REFERENCE) + " does not exist");
      return errors;
    }
    RenewalCandidate c = found.get();
    if (!UPDATABLE.contains(c.getStage()) || c.getMarketingLockedAt() != null) {
      errors.add(
          "Renewal " + c.getRenewalRef() + " cannot be updated in stage " + c.getStage().label());
    }
    RenewalDisposition code = disposition(row.text(DISPOSITION));
    if (code == null) {
      errors.add("Disposition " + row.text(DISPOSITION) + " is not valid");
    }
    errors.addAll(reasonErrors(row, code));
    String remarks = row.text(REMARKS);
    if (remarks != null && remarks.length() > MAX_REMARKS) {
      errors.add("Remarks may have at most 200 characters");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    RenewalCandidate c = candidate(row, context).orElseThrow();
    RenewalDisposition code = disposition(row.text(DISPOSITION));
    String reason = code == RenewalDisposition.NOT_FOR_RENEWAL ? reason(row.text(REASON)) : null;
    String invoice =
        reason != null && parameters.invoiceNoReason(reason) ? row.text(NEW_INVOICE) : null;
    dispositions.record(
        c,
        new CurrentDisposition(code, reason, DispositionSource.UPLOAD, row.text(REMARKS), invoice),
        null,
        null);
    String note = "Dispositioned file " + context.jobNo();
    if (c.getStage() == RenewalStage.UNASSIGNED) {
      flow.system(c, "assign", note);
    }
    if (c.getStage() == RenewalStage.FOR_DISPOSITION) {
      flow.system(c, "push", note);
    }
    post(c, code, note);
    return c.getRenewalRef();
  }

  private void post(RenewalCandidate c, RenewalDisposition code, String note) {
    switch (code) {
      case FOR_RENEWAL -> flow.system(c, "post_processing", note);
      case FOR_QUOTATION, FOR_PROPOSAL -> {
        c.takePath(RenewalPath.NB_PATH);
        flow.system(c, "post_nb_path", note);
      }
      case NOT_FOR_RENEWAL -> flow.system(c, "post_letter", note);
      default -> {
        flow.system(c, "post_close", note);
        c.close(ClosedAs.LOST, null, clock.instant());
      }
    }
  }

  private List<String> reasonErrors(BulkRow row, RenewalDisposition code) {
    if (code != RenewalDisposition.NOT_FOR_RENEWAL) {
      return List.of();
    }
    String reason = reason(row.text(REASON));
    String error = null;
    if (reason == null) {
      error = "Select the reason for Not for Renewal";
    } else if (RenewalCodes.REASON_TRANSFER.equals(reason)) {
      error = "Transfers to another unit are requested on the record, not by upload";
    } else if (parameters.invoiceNoReason(reason)) {
      error = invoiceError(row.text(NEW_INVOICE));
    }
    return error == null ? List.of() : List.of(error);
  }

  private String invoiceError(String invoice) {
    if (invoice == null) {
      return "Enter the new invoice number";
    }
    return invoices.existsByInvoiceNo(invoice) ? null : "Invoice " + invoice + " does not exist";
  }

  private Optional<RenewalCandidate> candidate(BulkRow row, BulkContext context) {
    String ref = row.text(REFERENCE);
    return ref == null
        ? Optional.empty()
        : candidates.findByCompanyIdAndRenewalRef(context.companyId(), ref.strip());
  }

  private RenewalDisposition disposition(String value) {
    if (value == null) {
      return null;
    }
    for (RenewalDisposition d : RenewalDisposition.values()) {
      if (UploadValues.names(value, d.name(), d.label())) {
        return d;
      }
    }
    return null;
  }

  /** A reason given by code or label; null when unknown. */
  private String reason(String value) {
    if (value == null) {
      return null;
    }
    return lovs
        .activeValues(RenewalCodes.LOV_NONRENEWAL_REASON, BusinessClock.today(clock))
        .stream()
        .filter(v -> UploadValues.names(value, v.getCode(), v.getLabel()))
        .map(LovValue::getCode)
        .findFirst()
        .orElse(null);
  }
}
