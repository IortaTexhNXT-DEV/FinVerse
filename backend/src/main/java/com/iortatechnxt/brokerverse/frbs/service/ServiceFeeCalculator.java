package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.frbs.domain.PaidInvoice;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine.LineDraft;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The service-fee computation (FRBS 2.10.0, Appendix A VI): each fully paid invoice takes the rule
 * of its market segment in force on the day it was paid; its base is the commission, net of the
 * insurer's withholding tax when the rule says so; its fee is the base times the rate (2 decimals,
 * half up). Invoices are grouped into one line per service-fee segment, sales unit and currency,
 * paid to the unit's recipient (or the unit itself) and charged to its cost centre.
 */
public final class ServiceFeeCalculator {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int MONEY = 2;

  private ServiceFeeCalculator() {}

  /**
   * The fee of one invoice.
   *
   * @param invoice paid invoice
   * @param rule rule applied
   * @return base and fee
   */
  public static Fee fee(PaidInvoice invoice, ServiceFeeRule rule) {
    BigDecimal base =
        rule.isNetOfWtax() ? invoice.commission().subtract(invoice.wtax()) : invoice.commission();
    base = base.max(BigDecimal.ZERO).setScale(MONEY, RoundingMode.HALF_UP);
    BigDecimal fee = base.multiply(rule.getRate()).divide(HUNDRED, MONEY, RoundingMode.HALF_UP);
    return new Fee(base, fee);
  }

  /**
   * Computes the lines of a run.
   *
   * @param invoices fully paid invoices of the period
   * @param rules service-fee rules
   * @param recipients recipients by sales unit
   * @param units unit names and cost centres by code
   * @return lines with their invoices, and the invoices no rule covers
   */
  public static Computation compute(
      List<PaidInvoice> invoices,
      List<ServiceFeeRule> rules,
      Map<String, ServiceFeeRecipient> recipients,
      Map<String, Unit> units) {
    Map<String, LineBuilder> lines = new LinkedHashMap<>();
    List<PaidInvoice> uncovered = new ArrayList<>();
    for (PaidInvoice invoice : invoices) {
      Optional<ServiceFeeRule> rule =
          rules.stream()
              .filter(r -> r.covers(invoice.marketSegment(), invoice.paidOn()))
              .findFirst();
      if (rule.isEmpty()) {
        uncovered.add(invoice);
        continue;
      }
      String unit = invoice.salesUnit() == null ? "UNASSIGNED" : invoice.salesUnit();
      String key = rule.get().getSegment() + "|" + unit + "|" + invoice.currency();
      lines
          .computeIfAbsent(key, k -> new LineBuilder(rule.get(), unit, invoice))
          .add(invoice, fee(invoice, rule.get()));
    }
    List<ComputedLine> computed =
        lines.values().stream()
            .map(b -> b.build(recipients.get(b.unit), units.get(b.unit)))
            .toList();
    return new Computation(computed, uncovered);
  }

  /** Accumulates the invoices of one line. */
  private static final class LineBuilder {
    private final ServiceFeeRule rule;
    private final String unit;
    private final PaidInvoice first;
    private final List<ItemFee> items = new ArrayList<>();
    private BigDecimal commission = BigDecimal.ZERO;
    private BigDecimal wtax = BigDecimal.ZERO;
    private BigDecimal base = BigDecimal.ZERO;
    private BigDecimal fee = BigDecimal.ZERO;

    LineBuilder(ServiceFeeRule rule, String unit, PaidInvoice first) {
      this.rule = rule;
      this.unit = unit;
      this.first = first;
    }

    void add(PaidInvoice invoice, Fee f) {
      items.add(new ItemFee(invoice, f));
      commission = commission.add(invoice.commission());
      wtax = wtax.add(rule.isNetOfWtax() ? invoice.wtax() : BigDecimal.ZERO);
      base = base.add(f.base());
      fee = fee.add(f.fee());
    }

    ComputedLine build(ServiceFeeRecipient recipient, Unit unitInfo) {
      boolean mapped = recipient != null && recipient.isActive();
      String payeeCode = mapped ? recipient.getPayeeCode() : unit;
      String payeeName =
          mapped ? recipient.getPayeeName() : unitInfo == null ? unit : unitInfo.name();
      String costCenter =
          firstNonNull(
              mapped ? recipient.getCostCenter() : null,
              unitInfo == null ? null : unitInfo.costCenter(),
              first.costCenter());
      LineDraft draft =
          new LineDraft(
              rule.getSegment(),
              unit,
              payeeCode,
              payeeName,
              costCenter,
              first.branchId(),
              first.currency(),
              rule.getRate(),
              items.size(),
              commission,
              wtax,
              base,
              fee);
      return new ComputedLine(draft, List.copyOf(items));
    }
  }

  private static String firstNonNull(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  /**
   * Base and fee of an invoice.
   *
   * @param base commission base
   * @param fee service fee
   */
  public record Fee(BigDecimal base, BigDecimal fee) {}

  /**
   * An invoice and its fee.
   *
   * @param invoice invoice
   * @param fee base and fee
   */
  public record ItemFee(PaidInvoice invoice, Fee fee) {}

  /**
   * A computed line with its invoices.
   *
   * @param draft line content
   * @param items invoices
   */
  public record ComputedLine(LineDraft draft, List<ItemFee> items) {}

  /**
   * What a computation gave.
   *
   * @param lines lines
   * @param uncovered invoices no rule covers
   */
  public record Computation(List<ComputedLine> lines, List<PaidInvoice> uncovered) {

    /**
     * The invoices counted.
     *
     * @return count
     */
    public int invoiceCount() {
      return lines.stream().mapToInt(l -> l.items().size()).sum();
    }

    /**
     * The total fee.
     *
     * @return sum of the lines
     */
    public BigDecimal feeTotal() {
      return lines.stream().map(l -> l.draft().fee()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }

  /**
   * A sales unit.
   *
   * @param name name
   * @param costCenter cost centre
   */
  public record Unit(String name, String costCenter) {}
}
