package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure remittance rules on an invoice's ledger position: what may be remitted now and why not
 * (RMTID.003/006/014/017/020/022/031), and the amounts of a remittance line (OPERATIONS_DESIGN 5
 * row 12, RMTID.023).
 *
 * <p>Commission, VAT and withholding tax are realized in proportion to the DTIP remitted so far
 * (cumulative, so rounding never drifts); the remittance that clears the DTIP takes the remaining
 * balances.
 */
public final class RemittanceRules {

  /** Reason: the invoice is on hold (RMTID.020/031). */
  public static final String ON_HOLD = "ON_HOLD";

  /** Reason: a negative adjustment is pending (RMTID.020). */
  public static final String PENDING_NEG_ADJ = "PENDING_NEG_ADJ";

  /** Reason: written off (RMTID.022). */
  public static final String WRITTEN_OFF = "WRITTEN_OFF";

  /** Reason: a payment is within the check holding period (RMTID.017/018). */
  public static final String CHECK_HOLDING = "CHECK_HOLDING";

  /** Reason: paid AR above the DTIP balance (RMTID.014). */
  public static final String PAID_AR_OVER_DTIP = "PAID_AR_OVER_DTIP";

  /** Reason: other (locked by another team). */
  public static final String OTHERS = "OTHERS";

  private static final int RATE_SCALE = 10;

  private RemittanceRules() {}

  /**
   * Decides whether an invoice is extracted (RMTID.003).
   *
   * @param facts ledger position, flags and rule settings
   * @return tag, reasons, remarks and the amount to remit
   */
  public static Decision decide(Facts facts) {
    Position p = facts.position();
    BigDecimal remittable = p.paidAr().subtract(p.dtip().remitted());
    if (remittable.signum() <= 0) {
      return new Decision(
          ExtractionTag.UNEXTRACTED_NOT_DUE, List.of(), "No paid AR to remit", Money.zero(), false);
    }
    List<String> reasons = blockingReasons(facts);
    String remarks = facts.lockedBy() == null ? null : "Locked by " + facts.lockedBy();
    boolean over = remittable.compareTo(p.dtip().balance()) > 0;
    if (over) {
      if (facts.capOverDtip() && p.dtip().balance().signum() > 0) {
        remittable = p.dtip().balance();
        remarks = "Paid AR capped at the DTIP balance " + p.dtip().balance();
      } else {
        reasons.add(PAID_AR_OVER_DTIP);
      }
    }
    ExtractionTag tag = reasons.isEmpty() ? ExtractionTag.EXTRACTED : ExtractionTag.UNEXTRACTED_DUE;
    return new Decision(tag, List.copyOf(reasons), remarks, remittable, over);
  }

  private static List<String> blockingReasons(Facts facts) {
    List<String> reasons = new ArrayList<>();
    if (facts.hold()) {
      reasons.add(ON_HOLD);
    }
    if (facts.pendingNegativeAdjustment()) {
      reasons.add(PENDING_NEG_ADJ);
    }
    if (facts.writtenOff()) {
      reasons.add(WRITTEN_OFF);
    }
    if (facts.checkHolding()) {
      reasons.add(CHECK_HOLDING);
    }
    if (facts.lockedBy() != null) {
      reasons.add(OTHERS);
    }
    return reasons;
  }

  /**
   * The amounts of a remittance line.
   *
   * @param p ledger position
   * @param amount paid AR to remit (at most the DTIP balance)
   * @param incentiveRate early-remittance incentive in percent of the basic premium, null for none
   * @return amounts and the basic premium part
   */
  public static Line amounts(Position p, BigDecimal amount, BigDecimal incentiveRate) {
    BigDecimal cumulative = p.dtip().remitted().add(amount);
    boolean clears = cumulative.compareTo(p.dtip().due()) >= 0;
    BigDecimal ratio =
        p.dtip().due().signum() == 0
            ? BigDecimal.ONE
            : cumulative.divide(p.dtip().due(), RATE_SCALE, RoundingMode.HALF_EVEN);
    BigDecimal commission = realized(p.commission(), ratio, clears);
    BigDecimal vat = realized(p.commissionVat(), ratio, clears);
    BigDecimal wtax = realized(p.wtax(), ratio, clears);
    BigDecimal basic =
        p.premiumDue().signum() == 0
            ? Money.zero()
            : Money.round(
                p.basicDue()
                    .multiply(amount)
                    .divide(p.premiumDue(), RATE_SCALE, RoundingMode.HALF_EVEN));
    BigDecimal incentive =
        incentiveRate == null
            ? Money.zero()
            : Money.round(basic.multiply(incentiveRate).movePointLeft(2));
    BigDecimal incentiveVat =
        p.commission().due().signum() == 0
            ? Money.zero()
            : Money.round(
                incentive
                    .multiply(p.commissionVat().due())
                    .divide(p.commission().due(), RATE_SCALE, RoundingMode.HALF_EVEN));
    BigDecimal paid = Money.round(amount);
    BigDecimal net = paid.add(wtax).subtract(commission).subtract(vat);
    return new Line(
        new RemittanceAmounts(paid, commission, vat, wtax, paid, incentive, incentiveVat, net),
        basic);
  }

  private static BigDecimal realized(Part part, BigDecimal ratio, boolean clears) {
    BigDecimal cumulative = clears ? part.due() : Money.round(part.due().multiply(ratio));
    BigDecimal line = Money.round(cumulative.subtract(part.remitted()));
    return line.signum() < 0 ? Money.zero() : line;
  }

  /**
   * One component of the position.
   *
   * @param due booked plus adjusted
   * @param remitted remitted so far
   * @param balance outstanding balance
   */
  public record Part(BigDecimal due, BigDecimal remitted, BigDecimal balance) {}

  /**
   * An invoice's ledger position.
   *
   * @param premiumDue premium receivable due (all PR components)
   * @param paidAr premium applied, net of reversals
   * @param basicDue basic premium due
   * @param dtip due to insurer
   * @param commission commission
   * @param commissionVat VAT on commission
   * @param wtax withholding tax on commission
   */
  public record Position(
      BigDecimal premiumDue,
      BigDecimal paidAr,
      BigDecimal basicDue,
      Part dtip,
      Part commission,
      Part commissionVat,
      Part wtax) {}

  /**
   * What the extraction knows of an invoice.
   *
   * @param position ledger position
   * @param hold on hold
   * @param pendingNegativeAdjustment negative adjustment pending
   * @param writtenOff written off
   * @param checkHolding a payment is within the holding period
   * @param lockedBy module holding the invoice's lock other than remittance, null when none
   * @param capOverDtip {@code REMIT_PAIDAR_OVER_DTIP_MODE} = CAP (OQ19)
   */
  public record Facts(
      Position position,
      boolean hold,
      boolean pendingNegativeAdjustment,
      boolean writtenOff,
      boolean checkHolding,
      String lockedBy,
      boolean capOverDtip) {}

  /**
   * An extraction decision.
   *
   * @param tag tag
   * @param reasons exclusion reason codes
   * @param remarks details, may be null
   * @param remittable paid AR to remit
   * @param overDtip paid AR exceeded the DTIP balance (RMTID.015 list)
   */
  public record Decision(
      ExtractionTag tag,
      List<String> reasons,
      String remarks,
      BigDecimal remittable,
      boolean overDtip) {}

  /**
   * Amounts of a line.
   *
   * @param amounts amounts
   * @param basicPremium basic premium part of the paid AR
   */
  public record Line(RemittanceAmounts amounts, BigDecimal basicPremium) {}
}
