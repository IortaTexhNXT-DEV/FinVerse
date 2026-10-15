package com.iortatechnxt.brokerverse.adjustment.seed;

import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItemRepository;
import com.iortatechnxt.brokerverse.adjustment.service.WriteOffService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringSettings;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SEED PROFILE ONLY: a minimal balance written off (conformance gap G03), after the Collections
 * seeds (order 126) so their plans and promises keep their story: the client pays the rest of the
 * motor invoice of ARN-2026-940004 short by PHP 45.50 at the cashier, and Adjustment writes the
 * balance off in its minimal balance file. The minimal balance file report has a row from day one.
 * Idempotent: nothing is done once the invoice is written off.
 */
@Component
@Profile("seed")
@Order(127)
public class MinimalBalanceSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(MinimalBalanceSeedData.class);
  private static final String COMPANY = "FVI";
  private static final String ARN = "ARN-2026-940004";
  private static final String CASHIER = "cashier";
  private static final String PROCESSOR = "adjust";
  private static final String FILE = "SEED-MINBAL-FILE";
  private static final String PHP = "PHP";

  /** Premium left unpaid, within the minimal balance file range (10.00 to 100.00). */
  private static final BigDecimal SHORT_PAID = new BigDecimal("45.50");

  private final CompanyRepository companies;
  private final InvoiceLedgerQueryService ledger;
  private final PaymentIntakeService intake;
  private final CashieringSettings settings;
  private final WriteOffService writeOffs;
  private final MinBalanceItemRepository items;
  private final SeedUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param ledger Operations ledger
   * @param intake payment intake
   * @param settings cashiering branches
   * @param writeOffs minimal balance write-offs
   * @param items write-offs (idempotency)
   * @param users seed sign-in
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public MinimalBalanceSeedData(
      CompanyRepository companies,
      InvoiceLedgerQueryService ledger,
      PaymentIntakeService intake,
      CashieringSettings settings,
      WriteOffService writeOffs,
      MinBalanceItemRepository items,
      SeedUsers users,
      Clock clock) {
    this.companies = companies;
    this.ledger = ledger;
    this.intake = intake;
    this.settings = settings;
    this.writeOffs = writeOffs;
    this.items = items;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    companies.findByCode(COMPANY).map(Company::getId).ifPresent(this::load);
  }

  private void load(Long companyId) {
    Optional<OpsInvoice> invoice =
        ledger.forArn(ARN).stream()
            .filter(i -> i.getEndorsementNo() == null)
            .filter(i -> i.premiumBalance().compareTo(SHORT_PAID) > 0)
            .filter(i -> items.findByInvoiceNo(i.getInvoiceNo()).isEmpty())
            .findFirst();
    invoice.ifPresent(i -> payShortAndWriteOff(companyId, i));
  }

  private void payShortAndWriteOff(Long companyId, OpsInvoice invoice) {
    try {
      Long ho = settings.headOffice(companyId).getId();
      BigDecimal amount = invoice.premiumBalance().subtract(SHORT_PAID);
      users.as(
          CASHIER,
          () ->
              intake.receive(
                  new IntakeTarget(companyId, ho, "OTC", ReceiptSource.OTC),
                  new PaymentIntake(
                      PaymentChannel.OTC,
                      null,
                      "SEED:940004-SHORT",
                      null,
                      invoice.getInvoiceNo(),
                      List.of(),
                      new PaymentIntake.Payor(null, invoice.getAssuredName()),
                      null,
                      new PaymentIntake.Money(amount, PHP, BusinessClock.today(clock)),
                      PaymentIntake.Tender.of(PaymentMode.CASH))));
      MinBalanceItem item =
          users.as(PROCESSOR, () -> writeOffs.writeOff(invoice.getInvoiceNo(), FILE, true));
      LOG.info("Adjustment seed: minimal balance of {} written off", item.getInvoiceNo());
    } catch (RuntimeException ex) {
      LOG.warn("Adjustment seed minimal balance skipped: {}", ex.getMessage());
    }
  }
}
