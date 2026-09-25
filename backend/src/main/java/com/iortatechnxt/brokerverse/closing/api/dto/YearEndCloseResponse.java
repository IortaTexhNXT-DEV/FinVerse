package com.iortatechnxt.brokerverse.closing.api.dto;

import com.iortatechnxt.brokerverse.closing.domain.YearEndClose;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Year-end close record view.
 *
 * @param id id
 * @param companyId company
 * @param fiscalYearId year
 * @param yearCode year code
 * @param closingDate closing date
 * @param netResult profit (positive) or loss transferred
 * @param retainedEarningsAccount retained earnings account
 * @param closingBatches closing journals
 * @param nextYearCode next fiscal year
 * @param status status
 * @param closedBy user
 * @param closedAt time
 * @param nominalBalance nominal balances as of the year end after the close (FRBS 2.7.1)
 * @param tbDifference trial balance difference as of the year end
 * @param verified whether both are zero
 * @param verifiedAt verification time
 */
public record YearEndCloseResponse(
    Long id,
    Long companyId,
    Long fiscalYearId,
    int yearCode,
    LocalDate closingDate,
    BigDecimal netResult,
    String retainedEarningsAccount,
    String closingBatches,
    Integer nextYearCode,
    String status,
    String closedBy,
    Instant closedAt,
    BigDecimal nominalBalance,
    BigDecimal tbDifference,
    Boolean verified,
    Instant verifiedAt) {

  /**
   * Maps an entity.
   *
   * @param c record
   * @return view
   */
  public static YearEndCloseResponse from(YearEndClose c) {
    return new YearEndCloseResponse(
        c.getId(),
        c.getCompanyId(),
        c.getFiscalYearId(),
        c.getYearCode(),
        c.getClosingDate(),
        c.getNetResult(),
        c.getRetainedEarningsAccount(),
        c.getClosingBatches(),
        c.getNextYearCode(),
        c.getStatus(),
        c.getCreatedBy(),
        c.getCreatedAt(),
        c.getNominalBalance(),
        c.getTbDifference(),
        c.getVerified(),
        c.getVerifiedAt());
  }
}
