package com.iortatechnxt.brokerverse.opsintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTagRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CommissionOrService;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules.Basis;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules.Subject;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.service.IncentiveRuleService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The seams wired between the Operations modules in the integration wave: remittance's incentive
 * rules behind the {@code EarlyIncentiveRules} port (PRCID.028, RMTID.023) and the Collection feeds
 * cashiering now takes by flow-in upload ({@code COLLECTION_CWT2307}, {@code
 * COLLECTION_COMMISSION_PAYMENT}), and the tiles every module adds to the Operations home.
 */
@IntegrationTest
class OperationsSeamsIT {

  private static final LocalDate BOOKED = LocalDate.of(2026, 9, 15);
  private static final String SCREENS =
      "^/(cashiering|remittance|prodrecon|adjustment|commission|operations)(/.*|\\?.*)?$";
  private static final Set<String> MINOR_WORDS =
      Set.of("a", "an", "the", "and", "or", "for", "in", "on", "to", "of", "with", "by", "at");

  @Autowired private EarlyIncentiveRules port;
  @Autowired private IncentiveRuleService rules;
  @Autowired private FlowInService flowIn;
  @Autowired private CashFixtures cash;
  @Autowired private CwtTagRepository cwtTags;
  @Autowired private CommissionOrService commissionOrs;
  @Autowired private AsUser as;
  @Autowired private List<OpsWorkCountSource> sources;

  @Test
  void productionReconciliationReadsRemittancesIncentiveRules() {
    String insurer = "INS-T" + BookingFixtures.token();
    Subject subject = new Subject(insurer, "CBG", "MOTOR", BOOKED.plusDays(10), BOOKED);
    assertThat(port.termsFor(cash.company(), subject)).isEmpty();

    EarlyIncentiveRule rule =
        as.run(
            "remittl",
            () ->
                rules.create(
                    cash.company(),
                    new EarlyIncentiveRule.Terms(
                        insurer,
                        "MOTOR",
                        null,
                        new BigDecimal("1.5"),
                        45,
                        IncentiveBasis.INCEPTION,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        true,
                        "Seam test")));
    assertThat(port.termsFor(cash.company(), subject))
        .hasValueSatisfying(
            t -> {
              assertThat(t.ratePercent()).isEqualByComparingTo("1.5");
              assertThat(t.windowDays()).isEqualTo(45);
              assertThat(t.basis()).isEqualTo(Basis.INCEPTION);
              assertThat(t.source()).isEqualTo("REM-RULE-" + rule.getId());
            });
    assertThat(port.termsFor(cash.company(), new Subject(insurer, "CBG", "PROPERTY", null, BOOKED)))
        .isEmpty();
    assertThat(port.termsFor(cash.company(), new Subject(insurer, "CBG", "MOTOR", null, null)))
        .isEmpty();
    assertThat(
            port.termsFor(
                cash.company(),
                new Subject(insurer, "CBG", "MOTOR", null, LocalDate.of(2027, 2, 1))))
        .isEmpty();
  }

  @Test
  void birCertificatesAndCommissionPaymentsArriveThroughTheCollectionFeeds() {
    OpsInvoice invoice = cash.cwtInvoice();
    cash.pay(invoice.getInvoiceNo(), invoice.premiumBalance());
    String cert = "2307-" + BookingFixtures.token();
    String cwtFile =
        "Company,Invoice no,Path,Certificate no,Period from,Period to,Remarks\n"
            + "FVI,"
            + invoice.getInvoiceNo()
            + ",certificate,"
            + cert
            + ",2026-07-01,2026-09-30,From Collection\n"
            + "FVI,NO-SUCH-INVOICE,CASH,,,,Unknown\n"
            + "XXX,"
            + invoice.getInvoiceNo()
            + ",CASH,,,,Unknown company\n";
    FlowInRun cwt = upload("COLLECTION_CWT2307", "cwt.csv", cwtFile);
    assertThat(cwt.getStatus()).isEqualTo(RunStatus.PARTIAL);
    assertThat(cwt.getOkCount()).isEqualTo(1);
    assertThat(cwt.getFailedCount()).isEqualTo(1);
    assertThat(cwt.getDuplicateCount()).isEqualTo(1);
    assertThat(cwtTags.findAll())
        .anySatisfy(
            t -> {
              assertThat(t.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo());
              assertThat(t.getCertificateNo()).isEqualTo(cert);
            });

    String paymentRef = "CHK-" + BookingFixtures.token();
    String commissionFile =
        "Company,Insurer code,Payee name,Certificate ref,Payment ref,Invoice no,Basic commission,"
            + "VAT,WTAX,Payment date\n"
            + "FVI,INS-MGIC,MGIC Insurance,2307-C,"
            + paymentRef
            + ",BI-X,1000.00,120.00,100.00,2026-09-24\n"
            + "FVI,INS-MGIC,MGIC Insurance,,"
            + paymentRef
            + "-B,BI-Y,0,0,0,2026-09-24\n";
    FlowInRun commission =
        upload("COLLECTION_COMMISSION_PAYMENT", "commission.csv", commissionFile);
    assertThat(commission.getOkCount()).isEqualTo(1);
    assertThat(commission.getFailedCount()).isEqualTo(1);
    assertThat(commissionOrs.staged(cash.company()))
        .anySatisfy(l -> assertThat(l.getPaymentRef()).isEqualTo(paymentRef));
  }

  @Test
  void everyHomeTileHasATitleCaseLabelACountAndAScreenToOpen() {
    List<WorkCount> tiles =
        sources.stream().flatMap(source -> source.counts(cash.company()).stream()).toList();
    assertThat(tiles).hasSizeGreaterThan(30);
    assertThat(tiles)
        .allSatisfy(
            t -> {
              assertThat(t.count()).as(t.label()).isNotNegative();
              assertThat(t.link()).as(t.label()).matches(SCREENS);
              assertThat(isTitleCase(t.label())).as(t.label()).isTrue();
            });
    assertThat(tiles).extracting(WorkCount::key).doesNotHaveDuplicates();
  }

  /** Title Case of the BDO UX guideline: every word capitalised except the minor words. */
  static boolean isTitleCase(String label) {
    String[] words = label.split(" ");
    for (int i = 0; i < words.length; i++) {
      String word = words[i].replaceAll("^[(]|[,)]$", "");
      boolean minor = i > 0 && MINOR_WORDS.contains(word);
      char first = word.charAt(0);
      if (minor ? !word.equals(word.toLowerCase(Locale.ROOT)) : Character.isLowerCase(first)) {
        return false;
      }
    }
    return true;
  }

  private FlowInRun upload(String feed, String name, String content) {
    return as.run(
        "admin",
        () -> flowIn.upload(feed, new FlowInFile(name, content.getBytes(StandardCharsets.UTF_8))));
  }
}
