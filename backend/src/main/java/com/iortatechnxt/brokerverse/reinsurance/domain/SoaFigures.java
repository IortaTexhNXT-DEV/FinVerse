package com.iortatechnxt.brokerverse.reinsurance.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;

/**
 * Lines of a reinsurer's quarterly statement of account, from the reinsurer's point of view (base
 * currency, all amounts positive except where a correction makes them negative).
 *
 * <ul>
 *   <li>Income (credit to the reinsurer): premium ceded, premium reserve released, interest on
 *       reserves, outstanding loss reserve released, salvage recoveries shared back.
 *   <li>Outgo (debit to the reinsurer): commission, levy, losses paid (recoveries due from the
 *       reinsurer), premium reserve retained, outstanding loss reserve retained.
 * </ul>
 *
 * @param premium premium ceded
 * @param commission ceding commission
 * @param levy premium tax / levy withheld
 * @param lossesPaid reinsurer's share of claims paid
 * @param recoveries reinsurer's share of salvage and subrogation recovered
 * @param premiumReserveRetained premium reserve withheld this quarter
 * @param premiumReserveReleased premium reserve withheld a year ago, released
 * @param interest interest on reserves held
 * @param lossReserveRetained outstanding loss reserve withheld at quarter end
 * @param lossReserveReleased outstanding loss reserve of the previous quarter, released
 */
public record SoaFigures(
    BigDecimal premium,
    BigDecimal commission,
    BigDecimal levy,
    BigDecimal lossesPaid,
    BigDecimal recoveries,
    BigDecimal premiumReserveRetained,
    BigDecimal premiumReserveReleased,
    BigDecimal interest,
    BigDecimal lossReserveRetained,
    BigDecimal lossReserveReleased) {

  /**
   * Sum of the income lines.
   *
   * @return income
   */
  public BigDecimal income() {
    return premium
        .add(premiumReserveReleased)
        .add(interest)
        .add(lossReserveReleased)
        .add(recoveries);
  }

  /**
   * Sum of the outgo lines.
   *
   * @return outgo
   */
  public BigDecimal outgo() {
    return commission
        .add(levy)
        .add(lossesPaid)
        .add(premiumReserveRetained)
        .add(lossReserveRetained);
  }

  /**
   * Balance of the statement: positive = due to the reinsurer, negative = due from the reinsurer.
   *
   * @return income - outgo
   */
  public BigDecimal balance() {
    return Money.round(income().subtract(outgo()));
  }

  /**
   * Net amount withheld from the reinsurer on this statement and accounted for on approval (levy,
   * reserves retained net of released, less interest credited).
   *
   * @return levy + premium reserve + loss reserve - interest
   */
  public BigDecimal adjustments() {
    return levy.add(premiumReserveNet()).add(lossReserveNet()).subtract(interest);
  }

  /**
   * Premium reserve retained net of released.
   *
   * @return retained - released
   */
  public BigDecimal premiumReserveNet() {
    return premiumReserveRetained.subtract(premiumReserveReleased);
  }

  /**
   * Loss reserve retained net of released.
   *
   * @return retained - released
   */
  public BigDecimal lossReserveNet() {
    return lossReserveRetained.subtract(lossReserveReleased);
  }
}
