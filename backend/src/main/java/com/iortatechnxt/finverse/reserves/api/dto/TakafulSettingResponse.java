package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.domain.TakafulSetting;
import com.iortatechnxt.finverse.reserves.domain.TakafulTerms;
import java.math.BigDecimal;

/**
 * Takaful surplus settings.
 *
 * @param companyId company
 * @param configured false when the company has never saved settings
 * @param enabled surplus run enabled
 * @param productCodes takaful product codes
 * @param participantSharePct participants' share %
 * @param taxPct tax %
 * @param costCenter cost centre of the surplus journal
 * @param recordStatus PENDING_AUTHORIZATION or ACTIVE (null when not configured)
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record TakafulSettingResponse(
    Long companyId,
    boolean configured,
    boolean enabled,
    String productCodes,
    BigDecimal participantSharePct,
    BigDecimal taxPct,
    String costCenter,
    String recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps saved settings.
   *
   * @param s settings
   * @return response
   */
  public static TakafulSettingResponse from(TakafulSetting s) {
    TakafulTerms t = s.terms();
    return new TakafulSettingResponse(
        s.getCompanyId(),
        true,
        t.enabled(),
        t.productCodes(),
        t.participantSharePct(),
        t.taxPct(),
        t.costCenter(),
        s.getRecordStatus().name(),
        s.getUpdatedBy() != null ? s.getUpdatedBy() : s.getCreatedBy(),
        s.getAuthorizedBy());
  }

  /**
   * Settings of a company that never configured takaful.
   *
   * @param companyId company
   * @return response (disabled)
   */
  public static TakafulSettingResponse none(Long companyId) {
    return new TakafulSettingResponse(
        companyId, false, false, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null);
  }
}
