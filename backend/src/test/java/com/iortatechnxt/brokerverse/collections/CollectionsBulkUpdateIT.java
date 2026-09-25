package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkUpdateHandler;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The bulk update handler {@code CLX_BULK_UPDATE} (BRCLXN.051): per-row validation (open account,
 * something to do, promise and escalation fields, worklist columns while the worklist port has no
 * implementation) and a commit that records the promise and the escalation with the upload number.
 */
@IntegrationTest
class CollectionsBulkUpdateIT {

  private static final String USER = "mkttl";

  @Autowired private CollectionsFixtures fx;
  @Autowired private CollectionsBulkUpdateHandler handler;
  @Autowired private PromiseService promises;
  @Autowired private EscalationService escalations;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;

  private BulkContext context(String jobNo) {
    return new BulkContext(fx.company(), jobNo, LocalDate.now(), Map.of());
  }

  private static BulkRow row(String... pairs) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int k = 0; k < pairs.length; k += 2) {
      values.put(pairs[k], pairs[k + 1]);
    }
    return new BulkRow(2, values);
  }

  private List<String> validate(BulkRow row) {
    return as.run(USER, () -> tx.execute(s -> handler.validate(row, context("BLK-V"))));
  }

  @Test
  void theHandlerDescribesItsTemplate() {
    assertThat(handler.code()).isEqualTo("CLX_BULK_UPDATE");
    assertThat(handler.permission()).isEqualTo("CLX_BULK_UPDATE");
    assertThat(handler.title()).isNotBlank();
    assertThat(handler.instructions()).contains("One row per invoice");
    assertThat(handler.columns()).hasSize(11);
    assertThat(handler.duplicateKey(row("Invoice No", "BI-1"))).isEqualTo("BI-1");
  }

  @Test
  void rowsAreValidatedOneByOne() {
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    assertThat(validate(row("Invoice No", "BI-NONE", "Escalate", "N")))
        .anyMatch(e -> e.contains("not in the ledger"))
        .anyMatch(e -> e.contains("Fill a promise"));
    assertThat(validate(row("Invoice No", no, "Promise Amount", "10")))
        .anyMatch(e -> e.contains("promise date"));
    assertThat(validate(row("Invoice No", no, "Escalate", "Y", "Escalation Level", "BOSS")))
        .anyMatch(e -> e.contains("Escalation Level"))
        .anyMatch(e -> e.contains("Escalation Reason"));
    assertThat(
            validate(
                row(
                    "Invoice No",
                    no,
                    "Escalate",
                    "Y",
                    "Escalation Level",
                    "USER",
                    "Escalation Reason",
                    "AGING")))
        .anyMatch(e -> e.contains("Name the user"));
    assertThat(
            validate(
                row(
                    "Invoice No",
                    no,
                    "Escalate",
                    "Y",
                    "Escalate To",
                    "ao",
                    "Escalation Reason",
                    "AGING")))
        .anyMatch(e -> e.contains("does not handle escalations"));
    assertThat(validate(row("Invoice No", no, "Disposition", "COORDINATE_FURTHER")))
        .anyMatch(e -> e.contains("worklist"));
    OpsInvoice direct = fx.directPaymentInvoice();
    assertThat(validate(row("Invoice No", direct.getInvoiceNo(), "Promise Date", "2026-12-01")))
        .anyMatch(e -> e.contains("nothing to collect"));
    assertThat(
            validate(
                row(
                    "Invoice No",
                    no,
                    "Promise Date",
                    LocalDate.now().plusDays(5).toString(),
                    "Escalate",
                    "Y",
                    "Escalation Reason",
                    "NO_COMMITMENT")))
        .isEmpty();
    List<String> aoErrors =
        as.run(
            "ao",
            () ->
                tx.execute(
                    s ->
                        handler.validate(
                            row("Invoice No", no, "Escalate", "Y", "Escalation Reason", "AGING"),
                            context("BLK-AO"))));
    assertThat(aoErrors).isEmpty();
  }

  @Test
  void aRowRecordsThePromiseAndTheEscalationWithTheUploadNumber() {
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    String jobNo = "BLK-" + CollectionsFixtures.token();
    BulkRow row =
        row(
            "Invoice No", no,
            "Promise Date", LocalDate.now().plusDays(5).toString(),
            "Promise Amount", "250.00",
            "Escalate", "Y",
            "Escalation Level", "tl",
            "Escalation Reason", "NO_COMMITMENT",
            "Remarks", "Uploaded");
    String reference = as.run(USER, () -> tx.execute(s -> handler.commit(row, context(jobNo))));
    assertThat(reference).startsWith(no + ": promise, ESC-");
    PaymentPromise promise = promises.forInvoice(no).get(0);
    assertThat(promise.getBulkRef()).isEqualTo(jobNo);
    assertThat(promise.getPromisedAmount()).isEqualByComparingTo("250.00");
    Escalation escalation = escalations.forInvoice(no).get(0);
    assertThat(escalation.getBulkRef()).isEqualTo(jobNo);
    assertThat(escalation.getRemarks()).isEqualTo("Uploaded");
  }
}
