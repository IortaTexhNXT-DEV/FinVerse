package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_PLACEMENT_RESPONSE} (FRRN.030.01 to FRRN.030.04): the insurer's placement
 * response file (uploaded, or received from the insurer's MFT location), each record matched to its
 * renewal account by the renewal reference number; Approved or Rejected with the rejection reason.
 */
@Component
public class PlacementResponseHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_PLACEMENT_RESPONSE";

  private static final String REFERENCE = "Renewal Reference Number";
  private static final String INSURER = "Insurer Code";
  private static final String RESPONSE = "Insurer Response";
  private static final String DATE = "Response Date";
  private static final String REASON = "Rejection Reason";
  private static final String REMARKS = "Insurer Remarks";
  private static final String APPROVED = "Approved";
  private static final String REJECTED = "Rejected";

  private final PlacementResponses responses;

  /**
   * Creates the handler.
   *
   * @param responses placement responses
   */
  public PlacementResponseHandler(PlacementResponses responses) {
    this.responses = responses;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - placement response";
  }

  @Override
  public String permission() {
    return Permission.RNW_PROCESS.name();
  }

  @Override
  public String filledBy() {
    return "The insurer, on the placement slips and placement file it received";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button Placement Response Upload";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            REFERENCE, "Renewal reference number of the account", "RNW-2026-000101"),
        BulkColumn.optional(
            INSURER, "Insurer code, for an account placed with several insurers", ""),
        BulkColumn.required(RESPONSE, "Approved or Rejected", APPROVED).values(APPROVED, REJECTED),
        new BulkColumn(DATE, "Date of the insurer's response", false, BulkColumn.Type.DATE, ""),
        BulkColumn.optional(REASON, "Reason of a rejection", "").lov(PlacementResponses.LOV_REJECT),
        BulkColumn.optional(REMARKS, "Remarks of the insurer", ""));
  }

  @Override
  public Set<String> optionalHeaders() {
    return Set.of(INSURER, DATE, REASON, REMARKS);
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("APPROVED", "REJECTED");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE) + "|" + row.text(INSURER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String response = row.text(RESPONSE);
    if (!APPROVED.equals(response) && !REJECTED.equals(response)) {
      errors.add("Insurer Response must be Approved or Rejected");
      return errors;
    }
    if (row.text(REFERENCE) == null) {
      errors.add("Renewal Reference Number is required");
      return errors;
    }
    String problem = responses.problem(context.companyId(), response(row));
    if (problem != null) {
      errors.add(problem);
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    PlacementResponses.Response r = response(row);
    String status = responses.apply(context.companyId(), r);
    return new BulkOutcome(
        r.renewalRef() + ": " + label(status), r.approved() ? "APPROVED" : "REJECTED");
  }

  private static String label(String status) {
    return switch (status) {
      case CandidatePlacement.BOOKED -> "Booked";
      case CandidatePlacement.REJECTED_PLACEMENT -> "Rejected Placement";
      default -> "For Booking";
    };
  }

  private static PlacementResponses.Response response(BulkRow row) {
    return new PlacementResponses.Response(
        row.text(REFERENCE).strip(),
        row.text(INSURER),
        APPROVED.equals(row.text(RESPONSE)),
        row.date(DATE),
        row.text(REASON),
        row.text(REMARKS));
  }
}
