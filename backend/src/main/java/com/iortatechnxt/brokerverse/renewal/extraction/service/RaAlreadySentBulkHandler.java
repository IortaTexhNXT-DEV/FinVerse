package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.RaSentRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_RA_ALREADY_SENT} (DMQ37, DMQ38; FR-RN-016): the Excel trackers of the
 * Renewal Advices sent by hand before go-live - legacy policy reference, RA date, RA reference,
 * channel and recipient. A matched row records the RA as sent, never to be sent again; an unmatched
 * or duplicate row is rejected to the upload result and is corrected and approved by the Renewal
 * processing team (maker-checker) on the Expiry List.
 */
@Component
public class RaAlreadySentBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_RA_ALREADY_SENT";

  private static final String LEGACY_REF = "Legacy Policy Reference";
  private static final String RA_DATE = "RA Date";
  private static final String RA_REF = "RA Reference";
  private static final String CHANNEL = "Channel";
  private static final String RECIPIENT = "Recipient";

  private final RaAlreadySentService service;

  /**
   * Creates the handler.
   *
   * @param service Renewal Advices already sent
   */
  public RaAlreadySentBulkHandler(RaAlreadySentService service) {
    this.service = service;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - Renewal Advices sent before go-live";
  }

  @Override
  public String permission() {
    return Permission.RNW_RA_SEND.name();
  }

  @Override
  public String instructions() {
    return "One row per migrated policy whose Renewal Advice was sent before go-live. Rejected"
        + " rows are corrected and approved by a second team member on the Expiry List.";
  }

  @Override
  public String filledBy() {
    return "The renewal team, from its Renewal Advice tracker of the legacy systems";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Setup, tab Go-live, button Upload RAs Already Sent";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(LEGACY_REF, "Policy reference in the legacy system", "QPS-FI-0012345"),
        new BulkColumn(
            RA_DATE, "Date the Renewal Advice was sent", true, BulkColumn.Type.DATE, "2027-12-10"),
        BulkColumn.optional(RA_REF, "Reference of the Renewal Advice in the tracker", "RA-1207"),
        BulkColumn.optional(CHANNEL, "How the Renewal Advice was sent", "E-mail")
            .values("E-mail", "Courier", "Hand", "Other"),
        BulkColumn.optional(RECIPIENT, "Who received it (name or e-mail)", "client@example.ph"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LEGACY_REF);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return service.problems(context.companyId(), row(row));
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return service.applyRow(context.companyId(), row(row));
  }

  private static RaSentRequest.Row row(BulkRow row) {
    return new RaSentRequest.Row(
        row.text(LEGACY_REF),
        row.date(RA_DATE),
        row.text(RA_REF),
        row.text(CHANNEL),
        row.text(RECIPIENT));
  }
}
