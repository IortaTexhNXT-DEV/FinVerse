package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDashboardService;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDashboardService.Group;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDashboardService.Item;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.cashiering.service.ReversalRecordService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Cashiering dashboard (FRS.CSH.01.03): the count and the amount per currency of the records
 * For Posting per receipt type, the Returned list, the unapplied payments for disposition and the
 * payment files, each with the list it opens, counted within the 5 seconds of the BRD.
 */
@IntegrationTest
class CashieringDashboardIT {

  @Autowired private CashFixtures fx;
  @Autowired private RecordFixtures rf;
  @Autowired private CashieringDashboardService dashboard;
  @Autowired private ReceiptRecordService records;
  @Autowired private ReversalRecordService reversals;
  @Autowired private ReceiptRecordPoster poster;
  @Autowired private AsUser as;

  private Item item(String group, String code) {
    return dashboard.dashboard(fx.company()).stream()
        .filter(g -> g.code().equals(group))
        .flatMap(g -> g.items().stream())
        .filter(i -> i.code().equals(code))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void theDashboardCountsAndTotalsTheRecordsForPostingPerReceiptType() {
    Item before = item("ISSUANCE", "ISSUANCE_AR_PREMIUM");
    rf.submitted(rf.ar(new BigDecimal("1500.00")));
    rf.submitted(rf.ar(new BigDecimal("250.50")));
    Item after = item("ISSUANCE", "ISSUANCE_AR_PREMIUM");
    assertThat(after.count()).isEqualTo(before.count() + 2);
    assertThat(after.amounts().get("PHP"))
        .isEqualByComparingTo(
            before.amounts().getOrDefault("PHP", BigDecimal.ZERO).add(new BigDecimal("1750.50")));
    assertThat(after.link())
        .contains("kind=CREATION")
        .contains("type=PREMIUM")
        .contains("stage=FOR_POSTING");

    ReceiptRecord returned = rf.submitted(rf.ar(new BigDecimal("80.00")));
    long returnedBefore = item("RETURNED", "RETURNED_AR_CREATION").count();
    as.run("cashtl", () -> poster.returnToCreator(returned.getId(), "Wrong account"));
    assertThat(item("RETURNED", "RETURNED_AR_CREATION").count()).isEqualTo(returnedBefore + 1);
  }

  @Test
  void anOrCommissionCancellationForPostingIsCountedUnderItsTypeAndOpensItsList() {
    OpsInvoice invoice = fx.motorInvoice();
    ReceiptRecord created =
        rf.submitted(
            rf.or(
                "COMMISSION",
                invoice.getInsurerCode(),
                new BigDecimal("1120.00"),
                new BigDecimal("120.00"),
                BigDecimal.ZERO));
    rf.post(created.getId());
    Long orId = records.get(created.getId()).getReceiptId();
    long before = item("CANCELLATION", "CANCELLATION_OR_COMMISSION").count();
    ReceiptRecord cancel =
        as.run(
                "cashier",
                () ->
                    reversals.cancel(
                        List.of(orId),
                        new RecordReason(
                            "COM_INCORRECT_DETAILS", null, null, null, null, null, null)))
            .get(0);
    as.run("cashier", () -> records.submit(cancel.getId()));
    Item after = item("CANCELLATION", "CANCELLATION_OR_COMMISSION");
    assertThat(after.count()).isEqualTo(before + 1);
    assertThat(after.link())
        .isEqualTo(
            "/cashiering/posting?kind=CANCELLATION&receiptKind=OR&type=COMMISSION&stage=FOR_POSTING");
  }

  @Test
  void everyGroupOfBdoiFrsIsCountedWithinFiveSeconds() {
    long start = System.nanoTime();
    List<Group> groups = dashboard.dashboard(fx.company());
    long millis = (System.nanoTime() - start) / 1_000_000;
    assertThat(millis).isLessThan(5000);
    assertThat(groups)
        .extracting(Group::code)
        .containsExactly(
            "ISSUANCE", "CANCELLATION", "REINSTATEMENT", "RETURNED", "UNAPPLIED", "FILES");
    assertThat(groups.get(0).items()).hasSize(9);
    assertThat(groups.get(3).items()).hasSize(6);
    assertThat(groups.get(4).items())
        .extracting(Item::label)
        .containsExactly(
            "Pre-Booked",
            "Excess",
            "Refund to Client",
            "Refund to Insurer",
            "Unbooked/Unmatched",
            "Unapplied",
            "Unapplied Commission");
    assertThat(groups.get(5).items())
        .extracting(Item::label)
        .containsExactly("PDC", "Bills Payment", "CLPC", "Trade", "Direct Credit");
  }
}
