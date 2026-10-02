package com.iortatechnxt.brokerverse.reserves.api.dto;

import com.iortatechnxt.brokerverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.brokerverse.reserves.domain.UprDetail;
import com.iortatechnxt.brokerverse.reserves.domain.UprItem;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Policy-level UPR of one premium transaction.
 *
 * @param policyId policy id
 * @param documentNo policy / endorsement number
 * @param kind NEW or endorsement type
 * @param branchId branch
 * @param businessLine line of business
 * @param productCode product
 * @param basis earning basis
 * @param coverFrom cover start
 * @param coverTo cover end
 * @param approvalDate approval date
 * @param totalUnits total units
 * @param earnedUnits earned units
 * @param unearnedUnits unearned units
 * @param premium written premium
 * @param commission written commission
 * @param cededPremium treaty + FAC premium
 * @param upr unearned premium (gross)
 * @param riUpr reinsurers' share of UPR (treaty + FAC)
 * @param dac unearned commission (DAC)
 * @param ucr unearned RI commission (UCR)
 */
public record UprDetailResponse(
    Long policyId,
    String documentNo,
    String kind,
    Long branchId,
    String businessLine,
    String productCode,
    String basis,
    LocalDate coverFrom,
    LocalDate coverTo,
    LocalDate approvalDate,
    int totalUnits,
    int earnedUnits,
    int unearnedUnits,
    BigDecimal premium,
    BigDecimal commission,
    BigDecimal cededPremium,
    BigDecimal upr,
    BigDecimal riUpr,
    BigDecimal dac,
    BigDecimal ucr) {

  /**
   * Maps a stored row.
   *
   * @param d row
   * @return response
   */
  public static UprDetailResponse from(UprDetail d) {
    UprItem i = d.toItem();
    PremiumAmounts w = i.written();
    PremiumAmounts u = i.unearned();
    return new UprDetailResponse(
        i.policyId(),
        i.documentNo(),
        i.kind(),
        i.key().branchId(),
        i.key().businessLine(),
        i.key().productCode(),
        i.basis(),
        i.coverFrom(),
        i.coverTo(),
        i.approvalDate(),
        i.units().total(),
        i.units().earned(),
        i.units().unearned(),
        w.premium(),
        w.commission(),
        w.cededPremium(),
        u.premium(),
        u.cededPremium(),
        u.commission(),
        u.riCommission());
  }
}
