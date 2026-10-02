package com.iortatechnxt.brokerverse.budget.api.dto;

import com.iortatechnxt.brokerverse.budget.domain.Budget;
import com.iortatechnxt.brokerverse.budget.domain.BudgetLine;
import com.iortatechnxt.brokerverse.budget.domain.BudgetStatus;
import com.iortatechnxt.brokerverse.budget.domain.BudgetVersionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Budget version view.
 *
 * @param id id
 * @param companyId company
 * @param fiscalYear fiscal year
 * @param versionNo version number
 * @param versionType ORIGINAL or REVISED
 * @param name description
 * @param currency currency
 * @param status status
 * @param basedOnId source version
 * @param total annual total of all lines
 * @param lineCount number of lines
 * @param createdBy maker
 * @param submittedBy submitter
 * @param submittedAt submission time
 * @param approvedBy approver
 * @param approvedAt approval time
 * @param rejectionReason reason of the last rejection
 * @param lines lines (detail view only)
 */
public record BudgetResponse(
    Long id,
    Long companyId,
    int fiscalYear,
    int versionNo,
    BudgetVersionType versionType,
    String name,
    String currency,
    BudgetStatus status,
    Long basedOnId,
    BigDecimal total,
    int lineCount,
    String createdBy,
    String submittedBy,
    Instant submittedAt,
    String approvedBy,
    Instant approvedAt,
    String rejectionReason,
    List<Line> lines) {

  /**
   * Summary mapping (without lines).
   *
   * @param b budget
   * @return response
   */
  public static BudgetResponse summary(Budget b) {
    return of(b, null);
  }

  /**
   * Detail mapping with lines.
   *
   * @param b budget
   * @return response
   */
  public static BudgetResponse detail(Budget b) {
    return of(b, b.getLines().stream().map(Line::from).toList());
  }

  private static BudgetResponse of(Budget b, List<Line> lines) {
    return new BudgetResponse(
        b.getId(),
        b.getCompanyId(),
        b.getFiscalYear(),
        b.getVersionNo(),
        b.getVersionType(),
        b.getName(),
        b.getCurrency(),
        b.getStatus(),
        b.getBasedOnId(),
        b.total(),
        b.getLines().size(),
        b.getCreatedBy(),
        b.getSubmittedBy(),
        b.getSubmittedAt(),
        b.getApprovedBy(),
        b.getApprovedAt(),
        b.getRejectionReason(),
        lines);
  }

  /**
   * Budget line view.
   *
   * @param accountCode account
   * @param costCenter cost centre
   * @param months twelve monthly amounts
   * @param annual annual total
   */
  public record Line(
      String accountCode, String costCenter, List<BigDecimal> months, BigDecimal annual) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static Line from(BudgetLine l) {
      return new Line(l.getAccountCode(), l.getCostCenter(), l.months(), l.annual());
    }
  }
}
