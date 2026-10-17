package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code DISB_BOB_APPROVED} (DIS 3.26.7): the BDO Business Online Banking approval report; each
 * fully approved transaction tags its online banking payment DEBITED by voucher reference. It is
 * the manual seam until a BOB interface exists ({@code BankChannelPort}, AQ09).
 */
@Component
public class BobApprovedHandler implements BulkImportHandler {

  private static final String VOUCHER = "Voucher reference";
  private static final String AMOUNT = "Amount";
  private static final String BOB = "BOB reference";

  private final InstrumentUploads uploads;

  /**
   * Creates the handler.
   *
   * @param uploads instrument uploads
   */
  public BobApprovedHandler(InstrumentUploads uploads) {
    this.uploads = uploads;
  }

  @Override
  public String code() {
    return "DISB_BOB_APPROVED";
  }

  @Override
  public String title() {
    return "BOB Approvals (Online Banking)";
  }

  @Override
  public String permission() {
    return "DISB_UPLOAD";
  }

  @Override
  public String filledBy() {
    return DisbursementUploadGuide.TREASURY;
  }

  @Override
  public String uploadPath() {
    return DisbursementUploadGuide.path("BOB Approvals");
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(VOUCHER, "DV number entered in online banking", "DV-2026-000001")
            .allowed("DV number of an online banking payment"),
        new BulkColumn(AMOUNT, "Amount approved", true, Type.NUMBER, "12500.00"),
        BulkColumn.optional(BOB, "Transaction reference of online banking", "BOB-778812"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(VOUCHER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return List.of();
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    uploads.bobApproved(row.text(VOUCHER), row.number(AMOUNT), row.text(BOB), context.jobNo());
    return row.text(VOUCHER);
  }
}
