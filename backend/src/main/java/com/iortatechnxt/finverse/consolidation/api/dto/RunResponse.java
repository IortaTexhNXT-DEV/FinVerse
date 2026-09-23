package com.iortatechnxt.finverse.consolidation.api.dto;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunLine;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunStatus;
import com.iortatechnxt.finverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.finverse.consolidation.service.ConsolidationRunService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Consolidation run view.
 *
 * @param id id
 * @param groupId group
 * @param runNo run number
 * @param asOfDate as-of date
 * @param currency consolidation currency
 * @param status status
 * @param totalDebit consolidated total debit
 * @param totalCredit consolidated total credit
 * @param createdBy user
 * @param createdAt time
 * @param finalizedBy finalizing user
 * @param finalizedAt finalization time
 * @param trialBalance consolidated trial balance (detail view)
 * @param lines consolidated ledger lines (detail view)
 */
public record RunResponse(
    Long id,
    Long groupId,
    String runNo,
    LocalDate asOfDate,
    String currency,
    ConsolidationRunStatus status,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    String createdBy,
    Instant createdAt,
    String finalizedBy,
    Instant finalizedAt,
    List<TbLine> trialBalance,
    List<Line> lines) {

  /**
   * Summary mapping.
   *
   * @param r run
   * @return view
   */
  public static RunResponse summary(ConsolidationRun r) {
    return of(r, null, null);
  }

  /**
   * Detail mapping with trial balance and ledger lines.
   *
   * @param r run
   * @return view
   */
  public static RunResponse detail(ConsolidationRun r) {
    return of(
        r,
        ConsolidationRunService.trialBalance(r).values().stream().map(TbLine::from).toList(),
        r.getLines().stream().map(Line::from).toList());
  }

  private static RunResponse of(ConsolidationRun r, List<TbLine> tb, List<Line> lines) {
    return new RunResponse(
        r.getId(),
        r.getGroupId(),
        r.getRunNo(),
        r.getAsOfDate(),
        r.getCurrency(),
        r.getStatus(),
        r.getTotalDebit(),
        r.getTotalCredit(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        r.getFinalizedBy(),
        r.getFinalizedAt(),
        tb,
        lines);
  }

  /**
   * Consolidated trial balance line (net debit).
   *
   * @param accountCode account
   * @param accountName name
   * @param accountClass class
   * @param aggregated translated members plus CTA
   * @param eliminations eliminations
   * @param consolidated consolidated balance
   */
  public record TbLine(
      String accountCode,
      String accountName,
      AccountClass accountClass,
      BigDecimal aggregated,
      BigDecimal eliminations,
      BigDecimal consolidated) {

    /**
     * Maps a balance.
     *
     * @param b balance
     * @return view
     */
    public static TbLine from(ConsolidatedBalance b) {
      return new TbLine(
          b.accountCode(),
          b.accountName(),
          b.accountClass(),
          b.aggregated(),
          b.eliminations(),
          b.consolidated());
    }
  }

  /**
   * Consolidated ledger line.
   *
   * @param lineNo line number
   * @param type line type
   * @param ruleCode elimination rule
   * @param companyId member company
   * @param accountCode account
   * @param accountName name
   * @param localAmount local currency amount
   * @param rate rate
   * @param amount consolidation currency amount (net debit)
   * @param description description
   */
  public record Line(
      int lineNo,
      ConsolidationLineType type,
      String ruleCode,
      Long companyId,
      String accountCode,
      String accountName,
      BigDecimal localAmount,
      BigDecimal rate,
      BigDecimal amount,
      String description) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static Line from(ConsolidationRunLine l) {
      return new Line(
          l.getLineNo(),
          l.getType(),
          l.getRuleCode(),
          l.getCompanyId(),
          l.getAccountCode(),
          l.getAccountName(),
          l.getLocalAmount(),
          l.getRate(),
          l.getAmount(),
          l.getDescription());
    }
  }
}
