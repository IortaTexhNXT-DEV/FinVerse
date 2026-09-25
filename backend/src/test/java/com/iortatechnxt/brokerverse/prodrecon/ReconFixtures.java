package com.iortatechnxt.brokerverse.prodrecon;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService.ExtractRequest;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService.UploadResult;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Production reconciliation test data: booked motor invoices of INS-MGIC (September 2026), their
 * register extracted into the open cycle, and insurer production files in the register layout.
 */
@Component
public class ReconFixtures {

  /** Insurer of the booking fixtures. */
  public static final String INSURER = "INS-MGIC";

  /** Header of the insurer files. */
  public static final String HEADER =
      "Month of Production,Insurer,Invoice Number,Policy No.,Assured Name,Gross Premium,"
          + "Gross Commission,Basic Premium,Incentive,Remarks";

  private final OpsLedgerFixtures ledger;
  private final ProductionExtractService extracts;
  private final ReconUploadService uploads;
  private final ReconItemRepository items;
  private final AsUser as;

  ReconFixtures(
      OpsLedgerFixtures ledger,
      ProductionExtractService extracts,
      ReconUploadService uploads,
      ReconItemRepository items,
      AsUser as) {
    this.ledger = ledger;
    this.extracts = extracts;
    this.uploads = uploads;
    this.items = items;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return ledger.company();
  }

  /** A newly booked motor invoice of INS-MGIC. */
  public OpsInvoice invoice() {
    return ledger.motorInvoice();
  }

  /** Extracts the register of the booking date of the fixtures. */
  public ReconExtract extract() {
    return as.run(
        "recon",
        () ->
            extracts.extract(
                new ExtractRequest(
                    company(), INSURER, BookingFixtures.BOOKED_ON, BookingFixtures.BOOKED_ON),
                ExtractTrigger.MANUAL));
  }

  /** The item of an invoice in a cycle. */
  public ReconItem item(Long cycleId, String invoiceNo) {
    return items.findByCycleIdAndInvoiceNo(cycleId, invoiceNo).orElseThrow();
  }

  /** An insurer line agreeing with a booked invoice, gross premium changed by a delta. */
  public static String line(OpsInvoice i, BigDecimal grossDelta) {
    return row(
        i.getInvoiceNo(),
        i.getPolicyNo(),
        i.getAssuredName(),
        i.getGrossPremium().add(grossDelta),
        i.getCommission());
  }

  /** An insurer line. */
  public static String row(
      String ref, String policy, String assured, BigDecimal gross, BigDecimal commission) {
    return String.join(
        ",",
        "2026-09",
        INSURER,
        quote(ref),
        quote(policy),
        quote(assured),
        gross == null ? "" : gross.toPlainString(),
        commission == null ? "" : commission.toPlainString(),
        "",
        "",
        "\"Checked, see remarks\"");
  }

  private static String quote(String v) {
    return v == null ? "" : "\"" + v.replace("\"", "\"\"") + "\"";
  }

  /** A file of lines. */
  public static byte[] file(List<String> lines) {
    return (HEADER + "\n" + String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  /** Uploads insurer lines as the reconciliation handler. */
  public UploadResult upload(List<String> lines) {
    return as.run(
        "recon",
        () -> uploads.upload(company(), "mgic-" + BookingFixtures.token() + ".csv", file(lines)));
  }
}
