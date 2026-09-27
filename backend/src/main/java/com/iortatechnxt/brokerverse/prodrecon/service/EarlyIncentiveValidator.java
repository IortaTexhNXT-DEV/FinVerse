package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules.Basis;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules.Subject;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules.Terms;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Early incentive validation (PRCID.028): for each booked invoice of a cycle, the rule that applies
 * (seam {@link EarlyIncentiveRules}), the first remittance to the insurer from the ledger, the days
 * from the basis date, the expected incentive and the incentive the insurer reports. Negative
 * outcomes: late remittance, missing insurer data, not remitted, no rule.
 */
@Service
@Transactional(readOnly = true)
public class EarlyIncentiveValidator {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final ReconItemRepository items;
  private final InvoiceLedgerQueryService ledger;
  private final EarlyIncentiveRules rules;

  /**
   * Creates the validator.
   *
   * @param items items
   * @param ledger Operations ledger
   * @param rules incentive rule seam
   */
  public EarlyIncentiveValidator(
      ReconItemRepository items, InvoiceLedgerQueryService ledger, EarlyIncentiveRules rules) {
    this.items = items;
    this.ledger = ledger;
    this.rules = rules;
  }

  /** Outcome of a line. */
  public enum Outcome {
    /** Remitted within the window: the incentive is due. */
    ELIGIBLE,
    /** Remitted after the window. */
    LATE,
    /** Not remitted yet. */
    NOT_REMITTED,
    /** The insurer did not report the account. */
    NO_INSURER_DATA,
    /** No incentive rule applies or is known (OQ23). */
    NO_RULE
  }

  /**
   * Validates the booked invoices of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return one line per booked invoice
   */
  public List<Line> validate(Long companyId, Long cycleId) {
    List<Line> out = new ArrayList<>();
    for (ReconItem item : items.findByCycleIdOrderByIdAsc(cycleId)) {
      if (item.getInvoiceNo() != null) {
        ledger.find(item.getInvoiceNo()).ifPresent(i -> out.add(line(companyId, item, i)));
      }
    }
    return out;
  }

  private Line line(Long companyId, ReconItem item, OpsInvoice invoice) {
    var c = invoice.getClassification();
    Optional<Terms> terms =
        rules.termsFor(
            companyId,
            new Subject(
                invoice.getInsurerCode(),
                c.segment(),
                c.productLine(),
                c.inceptionDate(),
                c.bookingDate()));
    LocalDate remitted = firstRemittance(ledger.movements(invoice.getInvoiceNo()));
    BigDecimal basic = item.getBdoi() == null ? BigDecimal.ZERO : item.getBdoi().basicPremium();
    Long days = null;
    BigDecimal expected = null;
    Outcome outcome;
    if (terms.isEmpty()) {
      outcome = Outcome.NO_RULE;
    } else {
      Terms t = terms.get();
      LocalDate basis = t.basis() == Basis.BOOKING ? c.bookingDate() : c.inceptionDate();
      days = remitted == null ? null : ChronoUnit.DAYS.between(basis, remitted);
      expected = basic.multiply(t.ratePercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
      outcome = outcome(item, days, t.windowDays());
    }
    return new Line(
        invoice.getInvoiceNo(),
        invoice.getAssuredName(),
        c.segment(),
        c.productLine(),
        c.inceptionDate(),
        remitted,
        days,
        terms.map(Terms::ratePercent).orElse(null),
        expected,
        item.getInsurerIncentive(),
        outcome);
  }

  private static Outcome outcome(ReconItem item, Long days, int window) {
    if (!item.isPaired()) {
      return Outcome.NO_INSURER_DATA;
    }
    if (days == null) {
      return Outcome.NOT_REMITTED;
    }
    return days <= window ? Outcome.ELIGIBLE : Outcome.LATE;
  }

  private static LocalDate firstRemittance(List<OpsInvoiceMovement> movements) {
    return movements.stream()
        .filter(m -> m.getMovementType() == MovementType.REMITTED && m.getAmount().signum() > 0)
        .map(OpsInvoiceMovement::getValueDate)
        .min(LocalDate::compareTo)
        .orElse(null);
  }

  /**
   * One validated invoice.
   *
   * @param invoiceNo invoice
   * @param assuredName assured
   * @param segment market segment
   * @param productLine product line
   * @param inceptionDate inception
   * @param remittedOn first remittance, null when not remitted
   * @param days days from the basis date to the remittance
   * @param ratePercent rate of the rule
   * @param expected expected incentive
   * @param insurerIncentive incentive the insurer reports
   * @param outcome outcome
   */
  public record Line(
      String invoiceNo,
      String assuredName,
      String segment,
      String productLine,
      LocalDate inceptionDate,
      LocalDate remittedOn,
      Long days,
      BigDecimal ratePercent,
      BigDecimal expected,
      BigDecimal insurerIncentive,
      Outcome outcome) {}
}
