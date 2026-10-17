package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.tax.domain.FilingFrequency;
import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.TaxForm;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.time.LocalDate;

/**
 * Tax form of the filing calendar.
 *
 * @param id id
 * @param companyId company
 * @param code form code
 * @param name name
 * @param authority authority
 * @param frequency filing frequency
 * @param worksheet computing worksheet
 * @param dueMonthsAfter months after the period-end month
 * @param dueDay due day
 * @param payableAccountCode tax payable account
 * @param creditAccountCode credit account
 * @param trackFiling returns and alerts managed in BrokerVerse
 * @param effectiveFrom first period tracked
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 */
public record TaxFormResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    TaxAuthority authority,
    FilingFrequency frequency,
    WorksheetKind worksheet,
    int dueMonthsAfter,
    int dueDay,
    String payableAccountCode,
    String creditAccountCode,
    boolean trackFiling,
    LocalDate effectiveFrom,
    RecordStatus recordStatus,
    String maker) {

  /**
   * Maps an entity.
   *
   * @param f form
   * @return response
   */
  public static TaxFormResponse from(TaxForm f) {
    return new TaxFormResponse(
        f.getId(),
        f.getCompanyId(),
        f.getCode(),
        f.getName(),
        f.getAuthority(),
        f.getFrequency(),
        f.getWorksheet(),
        f.getDueMonthsAfter(),
        f.getDueDay(),
        f.getPayableAccountCode(),
        f.getCreditAccountCode(),
        f.isTrackFiling(),
        f.getEffectiveFrom(),
        f.getRecordStatus(),
        f.getMaker());
  }
}
