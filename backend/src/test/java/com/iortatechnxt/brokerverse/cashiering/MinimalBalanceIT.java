package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.MinimalBalanceService;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedService;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Minimal balances (CSHID.016, Cashiering summary 5.f): a premium balance of PHP 10.00 or less is
 * reversed once (MIN_BAL movement and OPS_MINIMAL_BALANCE_REVERSAL), a balance equal to the whole
 * premium is left alone, and a small excess is moved to AP overages.
 */
@IntegrationTest
class MinimalBalanceIT {

  @Autowired private CashFixtures fx;
  @Autowired private MinimalBalanceService minimal;
  @Autowired private UnappliedService unapplied;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private long logged(String kind, String subject) {
    Long n =
        jdbc.queryForObject(
            "select count(*) from csh_minimal_balance where kind = ? and subject_ref = ?",
            Long.class,
            kind,
            subject);
    return n == null ? 0 : n;
  }

  @Test
  void smallPremiumBalancesAreReversedOnceAndSmallExcessGoesToOverages() {
    OpsInvoice invoice = fx.motorInvoice();
    fx.pay(invoice.getInvoiceNo(), invoice.premiumBalance().subtract(new BigDecimal("7.35")));
    Unapplied excess =
        fx.pay("UNKNOWN-MIN-" + System.nanoTime(), new BigDecimal("4.50")).unapplied();

    MinimalBalanceService.Sweep sweep = as.run("cashtl", () -> minimal.sweep(LocalDate.now()));
    assertThat(sweep.premium()).isPositive();
    assertThat(sweep.excess()).isPositive();

    OpsInvoice after = fx.invoice(invoice.getInvoiceNo());
    assertThat(after.premiumBalance()).isZero();
    assertThat(after.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(ledger.movementsOf("CASHIERING", "MINP:" + invoice.getInvoiceNo()))
        .isNotEmpty()
        .allMatch(m -> m.getMovementType() == MovementType.MIN_BAL);
    assertThat(logged("PREMIUM", invoice.getInvoiceNo())).isEqualTo(1);

    Unapplied swept = unapplied.get(excess.getId());
    assertThat(swept.getStage()).isEqualTo("CLOSED");
    assertThat(swept.getBalance()).isZero();
    assertThat(logged("EXCESS", excess.getReference())).isEqualTo(1);

    as.run("cashtl", () -> minimal.sweep(LocalDate.now()));
    assertThat(logged("PREMIUM", invoice.getInvoiceNo())).isEqualTo(1);
    assertThat(minimal.rules()).hasSize(3);
  }

  @Test
  void aSmallInvoiceStillFullyUnpaidIsNotReversed() {
    OpsInvoice invoice = fx.motorInvoice();
    as.run("cashtl", () -> minimal.sweep(LocalDate.now()));
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.UNPAID);
    assertThat(logged("PREMIUM", invoice.getInvoiceNo())).isZero();
  }
}
