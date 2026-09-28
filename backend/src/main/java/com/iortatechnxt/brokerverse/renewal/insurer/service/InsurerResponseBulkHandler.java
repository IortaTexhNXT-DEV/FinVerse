package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.UploadValues;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_INSURER_RESPONSE} (FR-RN-071): the insurer's dispositioned file, one row
 * per renewal with the renewal reference and the expiring policy number. A row whose renewal is
 * unknown or not with the insurer is rejected; a row whose policy number differs is kept as a
 * mismatch that never progresses the renewal; two rows of the same renewal are refused as
 * conflicting.
 */
@Component
public class InsurerResponseBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_INSURER_RESPONSE";

  private static final String REFERENCE = "Renewal Reference";
  private static final String POLICY = "Policy No";
  private static final String RESPONSE = "Response";
  private static final String INSURER_REF = "Insurer Reference";
  private static final String PREMIUM = "Revised Premium";
  private static final String SUM_INSURED = "Revised Sum Insured";
  private static final String RATE = "Revised Rate";
  private static final String TERMS = "Terms";
  private static final String RECEIVED = "Received On";
  private static final String REJECT_ACTION = "Reject Action";
  private static final String REMARKS = "Remarks";
  private static final String REMARKET = "REMARKET";

  private final RenewalCandidateRepository candidates;
  private final RenewalInsurerResponseService responses;

  /**
   * Creates the handler.
   *
   * @param candidates renewals
   * @param responses responses
   */
  public InsurerResponseBulkHandler(
      RenewalCandidateRepository candidates, RenewalInsurerResponseService responses) {
    this.candidates = candidates;
    this.responses = responses;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - insurer responses";
  }

  @Override
  public String permission() {
    return Permission.RNW_INSURER.name();
  }

  @Override
  public String filledBy() {
    return "The renewal team, from the insurers' renewal responses";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Insurer Batches, button Upload Insurer Responses";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(REFERENCE, "Renewal reference", "RNW-2027-000001").master("renewal"),
        BulkColumn.optional(POLICY, "Expiring policy number", "MC-2026-000123"),
        BulkColumn.required(RESPONSE, "Response of the insurer", "Renew As Is")
            .values("Renew As Is", "Revise", "Reject"),
        BulkColumn.optional(INSURER_REF, "Insurer reference", "REN-55012"),
        new BulkColumn(PREMIUM, "Revised gross premium", false, BulkColumn.Type.NUMBER, "")
            .when("Response is Revise"),
        new BulkColumn(SUM_INSURED, "Revised sum insured", false, BulkColumn.Type.NUMBER, "")
            .when("Response is Revise"),
        new BulkColumn(RATE, "Revised rate in percent", false, BulkColumn.Type.NUMBER, "")
            .when("Response is Revise"),
        BulkColumn.optional(TERMS, "Revised terms", "").when("Response is Revise"),
        new BulkColumn(RECEIVED, "Date received", false, BulkColumn.Type.DATE, "2027-08-15"),
        BulkColumn.optional(
                REJECT_ACTION, "What follows a rejection; blank for Not for renewal", "")
            .when("Response is Reject")
            .codes("NOT_FOR_RENEWAL", REMARKET),
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
    InsurerResponseCode code = code(row.text(RESPONSE));
    if (c.isEmpty()) {
      errors.add("Renewal " + row.text(REFERENCE) + " does not exist");
    } else if (code == null) {
      errors.add("Response " + row.text(RESPONSE) + " is not valid");
    } else {
      errors.addAll(responses.problems(c.get(), content(row, code)));
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    RenewalCandidate c = candidate(row, context).orElseThrow();
    InsurerResponseCode code = code(row.text(RESPONSE));
    String action = row.text(REJECT_ACTION);
    InsurerResponse r =
        responses.record(
            c,
            content(row, code),
            new RenewalInsurerResponseService.Source(
                "UPLOAD", context.jobNo(), row.rowNo(), row.text(POLICY)),
            UploadValues.names(action, REMARKET, null));
    return c.getRenewalRef() + (r.isLatestValid() ? "" : " (not applied)");
  }

  private Optional<RenewalCandidate> candidate(BulkRow row, BulkContext context) {
    String ref = row.text(REFERENCE);
    return ref == null
        ? Optional.empty()
        : candidates.findByCompanyIdAndRenewalRef(context.companyId(), ref.strip());
  }

  private static InsurerResponse.Content content(BulkRow row, InsurerResponseCode code) {
    return new InsurerResponse.Content(
        code,
        row.text(INSURER_REF),
        row.number(PREMIUM),
        row.number(SUM_INSURED),
        row.number(RATE),
        row.text(TERMS),
        row.date(RECEIVED),
        row.text(REMARKS));
  }

  private static InsurerResponseCode code(String value) {
    if (value == null) {
      return null;
    }
    if (UploadValues.names(value, "APPROVE", null) || UploadValues.names(value, "RENEW", null)) {
      return InsurerResponseCode.RENEW_AS_IS;
    }
    for (InsurerResponseCode c : InsurerResponseCode.values()) {
      if (UploadValues.names(value, c.name(), null)) {
        return c;
      }
    }
    return null;
  }
}
