package com.iortatechnxt.brokerverse.collections.installment.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Allocates what was settled on each invoice of a plan to its installments, oldest due first
 * (BRCLXN.054), and flags the overdue ones (BRCLXN.053). What an invoice's installments received is
 * their total less the invoice's outstanding premium in the ledger, so the allocation follows every
 * settlement the ledger knows (payments, reversals, 2307, direct payment reversal, write-off).
 * Policy years booked since the plan was made are linked to their invoice first.
 */
@Component
public class PlanAllocation {

  private final LedgerBalances ledger;
  private final BookingQueryService booking;
  private final Clock clock;

  /**
   * Creates the allocator.
   *
   * @param ledger ledger reads
   * @param booking policy years of multi-year accounts
   * @param clock clock
   */
  public PlanAllocation(LedgerBalances ledger, BookingQueryService booking, Clock clock) {
    this.ledger = ledger;
    this.booking = booking;
    this.clock = clock;
  }

  /**
   * Allocates a plan as of a date (inside the caller's transaction).
   *
   * @param plan plan with its installments
   * @param asOf business date
   */
  public void allocate(InstallmentPlan plan, LocalDate asOf) {
    if (plan.getSource() == PlanSource.POLICY_YEARS) {
      linkBookedYears(plan);
    }
    Map<String, List<Installment>> byInvoice = new LinkedHashMap<>();
    for (Installment i : plan.getInstallments()) {
      if (i.getInvoiceNo() == null) {
        i.allocate(BigDecimal.ZERO, asOf);
      } else {
        byInvoice.computeIfAbsent(i.getInvoiceNo(), k -> new ArrayList<>()).add(i);
      }
    }
    byInvoice.forEach((invoiceNo, list) -> allocateInvoice(invoiceNo, list, asOf));
    plan.refreshed(clock.instant());
  }

  private void allocateInvoice(String invoiceNo, List<Installment> list, LocalDate asOf) {
    BigDecimal total =
        list.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal outstanding = ledger.outstanding(invoiceNo).orElse(total);
    List<BigDecimal> shares =
        InstallmentSchedule.allocate(
            list.stream().map(Installment::getAmount).toList(), total.subtract(outstanding));
    for (int k = 0; k < list.size(); k++) {
      list.get(k).allocate(shares.get(k), asOf);
    }
  }

  private void linkBookedYears(InstallmentPlan plan) {
    if (plan.getInstallments().stream().allMatch(i -> i.getInvoiceNo() != null)) {
      return;
    }
    Map<Integer, String> booked =
        booking.schedule(plan.getArn()).stream()
            .filter(i -> i.getKind() == InvoiceKind.BOOKING && i.isBooked())
            .collect(
                Collectors.toMap(
                    BookedInvoice::getPolicyYear, BookedInvoice::getInvoiceNo, (a, b) -> a));
    plan.getInstallments().stream()
        .filter(i -> i.getInvoiceNo() == null && booked.containsKey(i.getPolicyYear()))
        .forEach(i -> i.linkInvoice(booked.get(i.getPolicyYear())));
  }
}
