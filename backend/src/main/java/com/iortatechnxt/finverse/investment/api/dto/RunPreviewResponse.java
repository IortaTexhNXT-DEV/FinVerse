package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.finverse.investment.domain.RunType;
import com.iortatechnxt.finverse.investment.service.InvestmentPostings.Due;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Accrual or amortization of a period: the posted run when it exists, otherwise the proposal.
 *
 * @param runType type
 * @param period period (YYYY-MM)
 * @param posted whether posted
 * @param run posted run (null when not posted)
 * @param total total
 * @param lines amounts per holding
 */
public record RunPreviewResponse(
    RunType runType,
    String period,
    boolean posted,
    InvestmentRunResponse run,
    BigDecimal total,
    List<Line> lines) {

  /** Canonical constructor copying the lines. */
  public RunPreviewResponse {
    lines = List.copyOf(lines);
  }

  /**
   * Builds a response totalling the lines.
   *
   * @param runType type
   * @param period period
   * @param run posted run or null
   * @param lines lines
   * @return response
   */
  public static RunPreviewResponse of(
      RunType runType, String period, InvestmentRunResponse run, List<Line> lines) {
    BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new RunPreviewResponse(runType, period, run != null, run, total, lines);
  }

  /**
   * Amount of one holding.
   *
   * @param holdingId holding
   * @param holdingNo holding number
   * @param description description
   * @param fromDate period start (exclusive)
   * @param toDate period end
   * @param days days
   * @param amount amount
   * @param batchNo journal (posted only)
   */
  public record Line(
      Long holdingId,
      String holdingNo,
      String description,
      LocalDate fromDate,
      LocalDate toDate,
      int days,
      BigDecimal amount,
      String batchNo) {

    /**
     * Maps a proposal.
     *
     * @param h holding
     * @param due amount due
     * @return line
     */
    public static Line of(InvestmentHolding h, Due due) {
      return new Line(
          h.getId(),
          h.getHoldingNo(),
          h.getDescription(),
          due.from(),
          due.to(),
          due.days(),
          due.amount(),
          null);
    }

    /**
     * Maps a posted transaction.
     *
     * @param t transaction
     * @return line
     */
    public static Line of(InvestmentTransaction t) {
      InvestmentHolding h = t.getHolding();
      return new Line(
          h.getId(),
          h.getHoldingNo(),
          h.getDescription(),
          t.getFromDate(),
          t.getTxnDate(),
          t.getDays(),
          t.getAmount(),
          t.getBatchNo());
    }
  }
}
