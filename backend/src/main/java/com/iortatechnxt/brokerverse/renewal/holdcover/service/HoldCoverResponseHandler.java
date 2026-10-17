package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAskRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_HOLD_COVER_RESPONSE} (FRRN.037.01 to FRRN.037.04): the insurer's response
 * file, each record matched to its request and renewal account by the reference number; Approved
 * confirms the hold cover, Rejected returns the account to its user with the insurer's remarks.
 */
@Component
public class HoldCoverResponseHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_HOLD_COVER_RESPONSE";

  private static final String REFERENCE = "Reference Number";
  private static final String RESPONSE = "Insurer Response";
  private static final String END = "Hold Cover End Date";
  private static final String INSURER_REF = "Insurer Reference";
  private static final String REMARKS = "Insurer Remarks";
  private static final String APPROVED = "Approved";
  private static final String REJECTED = "Rejected";

  private final HoldCoverAskRepository asks;
  private final HoldCoverRequests requests;

  /**
   * Creates the handler.
   *
   * @param asks requests
   * @param requests responses
   */
  public HoldCoverResponseHandler(HoldCoverAskRepository asks, HoldCoverRequests requests) {
    this.asks = asks;
    this.requests = requests;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - hold cover response";
  }

  @Override
  public String permission() {
    return Permission.RNW_PROCESS.name();
  }

  @Override
  public String filledBy() {
    return "The insurer, on the hold cover request file it received";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button Hold Cover Response Upload";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            REFERENCE, "Reference number of the hold cover request", "HCR-2026-000001"),
        BulkColumn.required(RESPONSE, "Approved or Rejected", APPROVED).values(APPROVED, REJECTED),
        new BulkColumn(END, "Last day of the hold cover approved", false, BulkColumn.Type.DATE, ""),
        BulkColumn.optional(INSURER_REF, "Reference of the insurer", "HC-55012"),
        BulkColumn.optional(REMARKS, "Remarks of the insurer, required for a rejection", ""));
  }

  @Override
  public Set<String> optionalHeaders() {
    return Set.of(END, INSURER_REF, REMARKS);
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("MATCHED", "UNMATCHED");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String response = row.text(RESPONSE);
    if (!APPROVED.equals(response) && !REJECTED.equals(response)) {
      errors.add("Insurer Response must be Approved or Rejected");
    }
    if (REJECTED.equals(response) && row.text(REMARKS) == null) {
      errors.add("Give the insurer remarks of a rejection");
    }
    Optional<HoldCoverAsk> ask = ask(row, context);
    if (ask.isEmpty()) {
      errors.add("Record not found: no hold cover request " + row.text(REFERENCE));
    } else if (!HoldCoverAsk.REQUESTED.equals(ask.get().getStatus())) {
      errors.add("Hold cover request " + row.text(REFERENCE) + " is " + ask.get().getStatus());
    } else if (row.date(END) != null && row.date(END).isBefore(ask.get().getStartDate())) {
      errors.add("Hold Cover End Date cannot be before the start of the hold cover");
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    boolean approved = APPROVED.equals(row.text(RESPONSE));
    HoldCoverAsk a =
        requests.respond(
            context.companyId(),
            row.text(REFERENCE),
            new HoldCoverRequests.Response(
                approved, row.date(END), row.text(INSURER_REF), row.text(REMARKS)));
    return new BulkOutcome(a.getRequestNo() + ": " + (approved ? APPROVED : REJECTED), "MATCHED");
  }

  private Optional<HoldCoverAsk> ask(BulkRow row, BulkContext context) {
    String ref = row.text(REFERENCE);
    return ref == null
        ? Optional.empty()
        : asks.findByCompanyIdAndRequestNo(context.companyId(), ref.strip());
  }
}
