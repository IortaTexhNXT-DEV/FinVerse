package com.iortatechnxt.brokerverse.cashiering.demo;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringSettings;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
 * Demo payments without a matching account for the unapplied-payments storyline of Collections
 * (wave C1-C, demo profile only, idempotent): the cashier receives four over-the-counter payments
 * whose reference matches no invoice, so each becomes an unapplied item that the collectors dispose
 * of ({@code collections.demo.UnappliedDemoData}) and Cashiering then applies or refunds ({@link
 * CollectorRequestDemoData}).
 */
@Component
@Profile("demo")
@Order(125)
public class UnappliedDemoPayments implements ApplicationRunner {

  /** Payors of the demo payments, in the order the storyline uses them. */
  public static final List<String> PAYORS =
      List.of("Grace Villanueva", "Mega Traders Inc.", "Juan Dela Cruz", "Liza Manalo");

  private static final Logger LOG = LoggerFactory.getLogger(UnappliedDemoPayments.class);
  private static final String MARKER = "DEMO:UPP-";
  private static final int AGE_DAYS = 3;
  private static final List<String> AMOUNTS = List.of("1000.00", "2500.00", "750.00", "1200.00");

  private final PaymentIntakeService intake;
  private final PaymentRepository payments;
  private final CompanyRepository companies;
  private final CashieringSettings settings;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param intake payment intake
   * @param payments payments (idempotency)
   * @param companies companies
   * @param settings settings (head office)
   * @param users demo sign-in
   * @param clock clock
   */
  public UnappliedDemoPayments(
      PaymentIntakeService intake,
      PaymentRepository payments,
      CompanyRepository companies,
      CashieringSettings settings,
      DemoUsers users,
      Clock clock) {
    this.intake = intake;
    this.payments = payments;
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
            .findByCompanyIdAndChannelAndSourceKey(company.get(), PaymentChannel.OTC, MARKER + 1)
            .isPresent()) {
      return;
    }
    try {
      users.run("cashier", () -> receive(company.get()));
      LOG.info("Unapplied demo payments received");
    } catch (RuntimeException ex) {
      LOG.warn("Unapplied demo payments skipped: {}", ex.getMessage());
    }
  }

  private void receive(Long companyId) {
    Long ho = settings.headOffice(companyId).getId();
    for (int i = 0; i < PAYORS.size(); i++) {
      intake.receive(
          new IntakeTarget(companyId, ho, "OTC", ReceiptSource.OTC),
          new PaymentIntake(
              PaymentChannel.OTC,
              null,
              MARKER + (i + 1),
              null,
              "NO-POLICY-REF-" + (i + 1),
              List.of(),
              new PaymentIntake.Payor(null, PAYORS.get(i)),
              null,
              new PaymentIntake.Money(
                  new BigDecimal(AMOUNTS.get(i)), "PHP", LocalDate.now(clock).minusDays(AGE_DAYS)),
              PaymentIntake.Tender.of(PaymentMode.CASH)));
    }
  }
}
