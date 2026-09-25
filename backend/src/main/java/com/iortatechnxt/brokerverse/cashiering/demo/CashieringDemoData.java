package com.iortatechnxt.brokerverse.cashiering.demo;

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
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Demo storyline of Cashiering (demo profile only), posted through the real services after the
 * Operations ledger replay (order 90): a full and a partial premium payment, a payment without a
 * match (with a refund disposition waiting for approval), an excess, a payment received before
 * booking (pre-booked queue), a post-dated check in the warehouse, a Head Office service fee OR, a
 * cancellation waiting for approval and a BIR 2307 tag. Idempotent.
 */
@Component
@Profile("demo")
@Order(95)
public class CashieringDemoData implements ApplicationRunner {

  private static final int PDC_DAYS = 30;
  private static final LocalDate CWT_PERIOD_FROM = LocalDate.parse("2026-07-01");
  private static final LocalDate CWT_PERIOD_TO = LocalDate.parse("2026-09-30");

  private static final Logger LOG = LoggerFactory.getLogger(CashieringDemoData.class);
  private static final String MARKER = "DEMO:940001";
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
  private final UserDetailsService users;
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
   * @param users users
   * @param clock clock
   */
  public CashieringDemoData(
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
      UserDetailsService users,
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
      as("cashier", () -> story(companyId, ho));
      as("mktcoll", () -> tag(companyId));
      LOG.info("Cashiering demo data posted");
    } catch (RuntimeException ex) {
      LOG.warn("Cashiering demo data skipped: {}", ex.getMessage());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private boolean story(Long companyId, Long ho) {
    first("ARN-2026-940001")
        .ifPresent(
            i ->
                pay(
                    companyId,
                    ho,
                    MARKER,
                    i.getInvoiceNo(),
                    i.premiumBalance(),
                    i.getAssuredName()));
    first("ARN-2026-940004")
        .ifPresent(
            i ->
                pay(
                    companyId,
                    ho,
                    "DEMO:940004",
                    i.getInvoiceNo(),
                    new BigDecimal("50000.00"),
                    i.getAssuredName()));
    IntakeResult unmatched =
        pay(
            companyId,
            ho,
            "DEMO:UNMATCHED",
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
        "DEMO:940005",
        "ARN-2026-940005",
        new BigDecimal("17027.86"),
        "Jose Miguel Reyes");
    IntakeResult small =
        pay(
            companyId,
            ho,
            "DEMO:SMALL",
            "ARN-2026-940001",
            new BigDecimal("500.00"),
            "Maria Clara Santos");
    actions.requestCancel(small.payment().getReceiptId(), new Reason("GEN_ISSUANCE_ERROR", null));
    return checksAndOr(companyId, ho);
  }

  private boolean checksAndOr(Long companyId, Long ho) {
    LocalDate today = LocalDate.now(clock);
    pdcs.warehouse(
        companyId,
        ho,
        new PdcCheck(
            "DEMO",
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
                    "DEMO:OR-SERVICE-FEE",
                    "Demo service fee"),
                false));
    LOG.info("Cashiering demo OR {}", or.getReceiptNo());
    return true;
  }

  private boolean tag(Long companyId) {
    first("ARN-2026-940004")
        .ifPresent(
            i ->
                cwt.tag(
                    companyId,
                    i.getInvoiceNo(),
                    new CwtDetails(
                        new BigDecimal("1000.00"),
                        CwtPath.CERTIFICATE,
                        "2307-DEMO-0001",
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
            new PaymentIntake.Money(amount, PHP, LocalDate.now(clock)),
            PaymentIntake.Tender.of(PaymentMode.CASH)));
  }

  private Optional<OpsInvoice> first(String arn) {
    return ledger.forArn(arn).stream().filter(i -> i.premiumBalance().signum() > 0).findFirst();
  }

  private void as(String username, Supplier<Boolean> work) {
    UserDetails details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    work.get();
  }
}
