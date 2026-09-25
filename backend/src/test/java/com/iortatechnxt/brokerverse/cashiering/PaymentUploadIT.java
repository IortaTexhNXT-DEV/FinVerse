package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLine;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem;
import com.iortatechnxt.brokerverse.cashiering.service.CommissionOrService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentFileLayouts;
import com.iortatechnxt.brokerverse.cashiering.service.PdcWarehouseService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/**
 * Payment files and uploads (CSHID.007/008, BRQID.006): a pipe-delimited Bills Payment TXT with
 * outcome categories and the duplicate file block, a fixed-width layout from the layout table, the
 * PDC list into the warehouse and its maturity, commission payment details consolidated into ORs,
 * and bulk 2307 tagging.
 */
@IntegrationTest
class PaymentUploadIT {

  @Autowired private CashFixtures fx;
  @Autowired private BulkService bulk;
  @Autowired private PaymentFileLayouts layouts;
  @Autowired private PdcWarehouseService pdcs;
  @Autowired private CommissionOrService commissions;
  @Autowired private AsUser as;

  private BulkJob upload(String user, String handler, String file, String content) {
    return as.run(
        user,
        () ->
            bulk.commit(
                bulk.upload(
                        new BulkUpload(
                            fx.company(),
                            handler,
                            file,
                            content.getBytes(StandardCharsets.UTF_8),
                            Map.of("branchId", fx.ho().toString())))
                    .getId()));
  }

  @Test
  void aBillsPaymentFileIsMatchedRowByRowAndCannotBeUploadedTwice() {
    OpsInvoice invoice = fx.motorInvoice();
    String tag = Long.toString(System.nanoTime());
    String file =
        "Incremental #|Invoice #|Assured's name|Amount|Late deposit (Y/N)|Phone #|Branch code|"
            + "Date of payment|Time of payment\n"
            + tag
            + "1|"
            + invoice.getInvoiceNo()
            + "|Test Assured|"
            + invoice.premiumBalance()
            + "|N|0917|001|2026-09-24|10:15\n"
            + tag
            + "2|NO-MATCH-"
            + tag
            + "|Walk In|120.00|Y|0917|001|2026-09-24|10:20\n";
    BulkJob job = upload("cashier", "PAY_BILLS", "bills-" + tag + ".txt", file);
    assertThat(job.getCommittedRows()).isEqualTo(2);
    assertThat(bulk.outcomes(job.getId()))
        .containsEntry("APPLIED", 1L)
        .containsEntry("UNAPPLIED_NO_MATCH", 1L);
    assertThatThrownBy(() -> upload("cashier", "PAY_BILLS", "again.txt", file))
        .extracting("code")
        .isEqualTo("BULK_DUPLICATE_FILE");
  }

  @Test
  void aFixedWidthLayoutFromTheLayoutTableIsUsedForDirectCredit() {
    as.run(
        "cashtl",
        () ->
            layouts.change(
                "PAY_DIRECT_CREDIT",
                "FIXED_WIDTH",
                null,
                "Transaction date:1:10;Transaction no:11:12;Paid amount:23:10;Payor:33:20;Account ref no:53:20;"
                    + "BP filename:73:1;Payment type:74:1;Assured:75:1;EBIX_RefNo:76:1;Logged by:77:1;Requestor:78:1"));
    try {
      String tag = Long.toString(System.nanoTime() % 1_000_000_000L);
      String line =
          String.format(
              "%-10s%-12s%10s%-20s%-20s%n",
              "2026-09-24", "DC" + tag, "55.00", "Direct Payor", "NOREF" + tag);
      BulkJob job = upload("cashier", "PAY_DIRECT_CREDIT", "dc-" + tag + ".txt", line);
      assertThat(job.getCommittedRows()).isEqualTo(1);
      assertThat(bulk.outcomes(job.getId())).containsEntry("UNAPPLIED_NO_MATCH", 1L);
    } finally {
      as.run("cashtl", () -> layouts.change("PAY_DIRECT_CREDIT", "DELIMITED", "|", null));
    }
    assertThatThrownBy(
            () -> as.run("cashtl", () -> layouts.change("PAY_TRADE", "FIXED_WIDTH", null, "Bad")))
        .extracting("code")
        .isEqualTo("PAYMENT_LAYOUT_FIELDS");
  }

  @Test
  void pdcsAreWarehousedAndMatureIntoPayments() {
    OpsInvoice invoice = fx.motorInvoice();
    String tag = Long.toString(System.nanoTime());
    String csv =
        "Client code,Payor,Reference,Check number,Bank code,Check branch,Maturity date,Amount,Market segment\n"
            + "CL-2026-000001,PDC Payor,"
            + invoice.getInvoiceNo()
            + ",CHK"
            + tag
            + ",BDO,Makati,"
            + LocalDate.now()
            + ",500.00,CBG\n";
    BulkJob job = upload("cashier", "PAY_PDC", "pdc-" + tag + ".csv", csv);
    assertThat(job.getCommittedRows()).isEqualTo(1);
    PdcItem item =
        pdcs.list(fx.company(), PdcStatus.WAREHOUSED, null, null, Pageable.ofSize(500)).stream()
            .filter(p -> p.getCheckNo().equals("CHK" + tag))
            .findFirst()
            .orElseThrow();
    assertThat(item.getWarehouseNo()).startsWith("PDCW-");

    int matured = as.run("cashier", () -> pdcs.mature(LocalDate.now()));
    assertThat(matured).isPositive();
    PdcItem after =
        pdcs
            .list(fx.company(), null, LocalDate.now(), LocalDate.now(), Pageable.ofSize(500))
            .stream()
            .filter(p -> p.getId().equals(item.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(after.getStatus()).isEqualTo(PdcStatus.APPLIED);
    assertThat(after.getReceiptNo()).startsWith("AR-HO-");
  }

  @Test
  void commissionPaymentsAreConsolidatedIntoOneOrPerInsurerCertificateAndPayment() {
    String tag = Long.toString(System.nanoTime());
    String csv =
        "Insurer code,Payee name,Certificate ref,Payment ref,Invoice no,Basic commission,VAT,WTAX,Payment date\n"
            + "INS-MGIC,MGIC Insurance,CERT"
            + tag
            + ",CHK"
            + tag
            + ",BI-A,1000.00,120.00,100.00,2026-09-24\n"
            + "INS-MGIC,MGIC Insurance,CERT"
            + tag
            + ",CHK"
            + tag
            + ",BI-B,500.00,60.00,50.00,2026-09-24\n"
            + "INS-LAC,LAC One,CERTL"
            + tag
            + ",CHKL"
            + tag
            + ",BI-C,300.00,36.00,30.00,2026-09-24\n"
            + "INS-LAC,LAC Two,CERTL"
            + tag
            + ",CHKL"
            + tag
            + ",BI-D,300.00,36.00,30.00,2026-09-24\n";
    BulkJob job = upload("cashier", "COMMISSION_PAYMENT", "comm-" + tag + ".csv", csv);
    assertThat(job.getCommittedRows()).isEqualTo(4);
    assertThat(commissions.staged(fx.company())).hasSizeGreaterThanOrEqualTo(4);

    List<String> ors = as.run("cashier", () -> commissions.issue(fx.company()));
    assertThat(ors).isNotEmpty().allMatch(n -> n.startsWith("OR-HO-"));
    assertThat(commissions.staged(fx.company()))
        .extracting(CommissionLine::getJobNo)
        .doesNotContain(job.getJobNo());
  }

  @Test
  void marketingTagsBir2307InBulk() {
    OpsInvoice invoice = fx.cwtInvoice();
    fx.pay(invoice.getInvoiceNo(), invoice.premiumBalance());
    String csv =
        "Invoice no,Amount,Path,Certificate no,Period from,Period to,Remarks\n"
            + invoice.getInvoiceNo()
            + ",,certificate,C-"
            + System.nanoTime()
            + ",2026-07-01,2026-09-30,bulk\n";
    BulkJob job = upload("mktcoll", "CWT_TAGS", "cwt-" + System.nanoTime() + ".csv", csv);
    assertThat(job.getCommittedRows()).isEqualTo(1);
  }
}
