package com.iortatechnxt.finverse.reinsurance.api.dto;

import com.iortatechnxt.finverse.reinsurance.domain.Soa;
import com.iortatechnxt.finverse.reinsurance.domain.SoaStatus;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import com.iortatechnxt.finverse.reinsurance.service.SoaLayout;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Statement of account with its printed layout.
 *
 * @param id id
 * @param soaNo statement number
 * @param treatyCode treaty code
 * @param treatyName treaty name
 * @param treatyType treaty type
 * @param businessLine line of business
 * @param reinsurerCode participant code
 * @param reinsurerName participant name
 * @param year year
 * @param quarter quarter
 * @param periodFrom quarter start
 * @param periodTo quarter end
 * @param statementDate statement date
 * @param currency currency
 * @param balance balance (positive = due to the reinsurer)
 * @param status status
 * @param layout income / outgo layout with the balance on the smaller side
 * @param preparedBy maker
 * @param approvedBy checker
 * @param approvedAt approval time
 * @param adjustmentBatchNo journal of the statement adjustments
 * @param settlementDate settlement date
 * @param settlementBatchNo journal of the settlement
 * @param bankAccountCode bank account of the settlement
 */
public record SoaResponse(
    Long id,
    String soaNo,
    String treatyCode,
    String treatyName,
    TreatyType treatyType,
    String businessLine,
    String reinsurerCode,
    String reinsurerName,
    int year,
    int quarter,
    LocalDate periodFrom,
    LocalDate periodTo,
    LocalDate statementDate,
    String currency,
    BigDecimal balance,
    SoaStatus status,
    SoaLayout layout,
    String preparedBy,
    String approvedBy,
    Instant approvedAt,
    String adjustmentBatchNo,
    LocalDate settlementDate,
    String settlementBatchNo,
    String bankAccountCode) {

  /**
   * Maps a statement.
   *
   * @param s statement
   * @param layout printed layout
   * @return response
   */
  public static SoaResponse from(Soa s, SoaLayout layout) {
    return new SoaResponse(
        s.getId(),
        s.getSoaNo(),
        s.getTreaty().getCode(),
        s.getTreaty().getName(),
        s.getTreaty().getTreatyType(),
        s.getTreaty().getBusinessLine(),
        s.getParty().getCode(),
        s.getParty().getName(),
        s.getSoaYear(),
        s.getQuarter(),
        s.getPeriodFrom(),
        s.getPeriodTo(),
        s.getStatementDate(),
        s.getCurrency(),
        s.getBalance(),
        s.getStatus(),
        layout,
        s.getCreatedBy(),
        s.getApprovedBy(),
        s.getApprovedAt(),
        s.getAdjustmentBatchNo(),
        s.getSettlementDate(),
        s.getSettlementBatchNo(),
        s.getBankAccountCode());
  }
}
