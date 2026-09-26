package com.iortatechnxt.brokerverse.cashiering.seed;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.CwtDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem.PdcCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.Reason;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringSettings;
import com.iortatechnxt.brokerverse.cashiering.service.CwtService;
import com.iortatechnxt.brokerverse.cashiering.service.DispositionService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.cashiering.service.PdcWarehouseService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptActionService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed storyline of Cashiering (seed profile only), posted through the real services after the
 * Operations ledger replay (order 90) and before remittance (order 92), which extracts what is paid
 * here: the booking and the endorsement invoices of ARN-2026-940001 paid in full and a partial
 * payment of ARN-2026-940004 (valued a week back, past the check holding period), a payment without
 * a match (with a refund disposition waiting for approval), an excess, a payment received before
 * booking (pre-booked queue), a post-dated check in the warehouse, a Head Office service fee OR, a
 * cancellation waiting for approval and a BIR 2307 tag. Signed in as cashier (mktcoll for the tag).
 * Idempotent.
 */
@Component
@Profile("seed")
@Order(91)
public class CashieringSeedData implements ApplicationRunner {

  private static final int PDC_DAYS = 30;

  /** Age of the seed payments: past the check holding period, so remittance can extract them. */
  private static final int PAYMENT_AGE_DAYS = 7;

  private static final LocalDate CWT_PERIOD_FROM = LocalDate.parse("2026-07-01");
  private static final LocalDate CWT_PERIOD_TO = LocalDate.parse("2026-09-30");

  private static final Logger LOG = LoggerFactory.getLogger(CashieringSeedData.class);
  private static final String MARKER = "SEED:940001";
  private static final String PHP = "PHP";

  private final PaymentIntakeService intake;
  private final PaymentRepository payments;
  private final CashReceiptService receipts;
  private final ReceiptActionService actions;
  private final DispositionService dispositions;
  private final PdcWarehouseService pdcs;
  private final CwtService cwt;
  private final InvoiceLedgerQueryService ledger;
  private final CompanyRepository companies;
  private final CashieringSettings settings;
  private final SeedUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param intake payment intake
   * @param payments payments (idempotency)
   * @param receipts receipts
   * @param actions cancellations
   * @param dispositions dispositions
   * @param pdcs PDC warehouse
   * @param cwt BIR 2307
   * @param ledger invoice ledger
   * @param companies companies
   * @param settings settings
   * @param users seed sign-in
   * @param clock clock
   */
  public CashieringSeedData(
      PaymentIntakeService intake,
      PaymentRepository payments,
      CashReceiptService receipts,
      ReceiptActionService actions,
      DispositionService dispositions,
      PdcWarehouseService pdcs,
      CwtService cwt,
      InvoiceLedgerQueryService ledger,
      CompanyRepository companies,
      CashieringSettings settings,
      SeedUsers users,
      Clock clock) {
    this.intake = intake;
    this.payments = payments;
    this.receipts = receipts;
    this.actions = actions;
    this.dispositions = dispositions;
    this.pdcs = pdcs;
    this.cwt = cwt;
    this.ledger = ledger;
    this.companies = companies;
    this.settings = settings;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Long> company = companies.findByCode("FVI").map(c -> c.getId());
    if (company.isEmpty()
        || payments
            .findByCompanyIdAndChannelAndSourceKey(company.get(), PaymentChannel.OTC, MARKER)
            .isPresent()) {
      return;
    }
    Long companyId = company.get();
    Long ho = settings.headOffice(companyId).getId();
    try {
      users.as("cashier", () -> story(companyId, ho));
      users.as("mktcoll", () -> tag(companyId));
      LOG.info("Cashiering seed data posted");
    } catch (RuntimeException ex) {
      LOG.warn("Cashiering seed data skipped: {}", ex.getMessage());
    }
  }

  private boolean story(Long companyId, Long ho) {
    paidInFull(companyId, ho);
    original("ARN-2026-940004")
        .ifPresent(
            i ->
                pay(
                    companyId,
                    ho,
                    "SEED:940004",
                    i.getInvoiceNo(),
                    new BigDecimal("50000.00"),
                    i.getAssuredName()));
    IntakeResult unmatched =
        pay(
            companyId,
            ho,
            "SEED:UNMATCHED",
            "BI-UNKNOWN-0001",
            new BigDecimal("1500.00"),
            "Walk-in Payor");
    if (unmatched.unapplied() != null) {
      Long item = unmatched.unapplied().getId();
      dispositions.assign(
          item,
          "REFUND",
          new DispositionDetails(
              new BigDecimal("1500.00"), null, null, null, "Walk-in Payor", "No policy found"));
      dispositions.submit(item);
    }
    pay(
        companyId,
        ho,
        "SEED:940005",
        "ARN-2026-940005",
        new BigDecimal("17027.86"),
        "Jose Miguel Reyes");
    IntakeResult small =
        pay(
            companyId,
            ho,
            "SEED:SMALL",
            "ARN-2026-940001",
            new BigDecimal("500.00"),
            "Maria Clara Santos");
    actions.requestCancel(small.payment().getReceiptId(), new Reason("GEN_ISSUANCE_ERROR", null));
    return checksAndOr(companyId, ho);
  }

  private void paidInFull(Long companyId, Long ho) {
    original("ARN-2026-940001")
        .ifPresent(
            i ->
                pay(
                    companyId,
                    ho,
                    MARKER,
                    i.getInvoiceNo(),
                    i.premiumBalance(),
                    i.getAssuredName()));
    endorsement("ARN-2026-940001")
        .ifPresent(
            i ->
                pay(
                    companyId,
                    ho,
                    "SEED:940001-EN",
                    i.getInvoiceNo(),
                    i.premiumBalance(),
                    i.getAssuredName()));
  }

  private boolean checksAndOr(Long companyId, Long ho) {
    LocalDate today = LocalDate.now(clock);
    pdcs.warehouse(
        companyId,
        ho,
        new PdcCheck(
            "SEED",
            "CL-2026-000003",
            "Pacific Harbor Logistics Inc.",
            "ARN-2026-940004",
            "0045671",
            "BDO",
            "Ortigas",
            today.plusDays(PDC_DAYS),
            new BigDecimal("28281.25"),
            PHP,
            "CORBANK"));
    Receipt or =
        receipts.issueOr(
            new OrIssue(
                companyId,
                ho,
                "SERVICE_FEE",
                today,
                "CL-2026-000003",
                "Pacific Harbor Logistics Inc.",
                PHP,
                List.of(
                    CashReceiptService.line(
                        null,
                        null,
                        new OrAmounts(
                            new BigDecimal("25000.00"),
                            new BigDecimal("3000.00"),
                            new BigDecimal("500.00")),
                        "Risk management consultancy fee")),
                new ReceiptTender(
                    PaymentMode.CHECK,
                    "0098812",
                    "BPI",
                    today,
                    null,
                    ReceiptSource.OTC,
                    CashieringSettings.MODULE,
                    "SEED:OR-SERVICE-FEE",
                    "Seed service fee"),
                false));
    LOG.info("Cashiering seed OR {}", or.getReceiptNo());
    return true;
  }

  private boolean tag(Long companyId) {
    original("ARN-2026-940004")
        .ifPresent(
            i ->
                cwt.tag(
                    companyId,
                    i.getInvoiceNo(),
                    new CwtDetails(
                        new BigDecimal("1000.00"),
                        CwtPath.CERTIFICATE,
                        "2307-2026-900001",
                        CWT_PERIOD_FROM,
                        CWT_PERIOD_TO,
                        "Client certificate received")));
    return true;
  }

  private IntakeResult pay(
      Long companyId, Long branch, String key, String reference, BigDecimal amount, String payor) {
    return intake.receive(
        new IntakeTarget(companyId, branch, "OTC", ReceiptSource.OTC),
        new PaymentIntake(
            PaymentChannel.OTC,
            null,
            key,
            null,
            reference,
            List.of(),
            new PaymentIntake.Payor(null, payor),
            null,
            new PaymentIntake.Money(amount, PHP, LocalDate.now(clock).minusDays(PAYMENT_AGE_DAYS)),
            PaymentIntake.Tender.of(PaymentMode.CASH)));
  }

  private Optional<OpsInvoice> original(String arn) {
    return outstanding(arn).filter(i -> i.getEndorsementNo() == null).findFirst();
  }

  private Optional<OpsInvoice> endorsement(String arn) {
    return outstanding(arn).filter(i -> i.getEndorsementNo() != null).findFirst();
  }

  private Stream<OpsInvoice> outstanding(String arn) {
    return ledger.forArn(arn).stream().filter(i -> i.premiumBalance().signum() > 0);
  }
}
