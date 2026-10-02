package com.iortatechnxt.brokerverse.closing.api.dto;

import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationLine;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRun;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * FX revaluation run view.
 *
 * @param id id
 * @param companyId company
 * @param periodId period
 * @param periodName period name
 * @param revaluationDate revaluation date
 * @param gainLossAccount gain/loss account
 * @param autoReverse auto-reverse flag
 * @param reversalDate reversal date
 * @param status status
 * @param journalBatchNo revaluation journal
 * @param reversalBatchNo reversal journal
 * @param reversalPending whether the reversal still waits for the next period to open
 * @param totalGain total unrealized gain
 * @param totalLoss total unrealized loss
 * @param createdBy user
 * @param createdAt time
 * @param lines revalued balances (detail view)
 */
public record FxRevaluationResponse(
    Long id,
    Long companyId,
    Long periodId,
    String periodName,
    LocalDate revaluationDate,
    String gainLossAccount,
    boolean autoReverse,
    LocalDate reversalDate,
    FxRevaluationStatus status,
    String journalBatchNo,
    String reversalBatchNo,
    boolean reversalPending,
    BigDecimal totalGain,
    BigDecimal totalLoss,
    String createdBy,
    Instant createdAt,
    List<Line> lines) {

  /**
   * Summary mapping.
   *
   * @param r run
   * @return view
   */
  public static FxRevaluationResponse summary(FxRevaluationRun r) {
    return of(r, null);
  }

  /**
   * Detail mapping.
   *
   * @param r run
   * @return view
   */
  public static FxRevaluationResponse detail(FxRevaluationRun r) {
    return of(r, r.getLines().stream().map(Line::from).toList());
  }

  private static FxRevaluationResponse of(FxRevaluationRun r, List<Line> lines) {
    return new FxRevaluationResponse(
        r.getId(),
        r.getCompanyId(),
        r.getPeriodId(),
        r.getPeriodName(),
        r.getRevaluationDate(),
        r.getGainLossAccount(),
        r.isAutoReverse(),
        r.getReversalDate(),
        r.getStatus(),
        r.getJournalBatchNo(),
        r.getReversalBatchNo(),
        r.isReversalPending(),
        r.getTotalGain(),
        r.getTotalLoss(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        lines);
  }

  /**
   * Revalued balance.
   *
   * @param branchId branch
   * @param accountCode account
   * @param currency currency
   * @param fcBalance foreign currency balance
   * @param bookedBase booked base balance
   * @param closingRate closing rate
   * @param revaluedBase revalued base balance
   * @param difference unrealized difference
   * @param posted whether posted
   */
  public record Line(
      Long branchId,
      String accountCode,
      String currency,
      BigDecimal fcBalance,
      BigDecimal bookedBase,
      BigDecimal closingRate,
      BigDecimal revaluedBase,
      BigDecimal difference,
      boolean posted) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static Line from(FxRevaluationLine l) {
      return new Line(
          l.getBranchId(),
          l.getAccountCode(),
          l.getCurrency(),
          l.getFcBalance(),
          l.getBookedBase(),
          l.getClosingRate(),
          l.getRevaluedBase(),
          l.getDifference(),
          l.isPosted());
    }
  }
}
