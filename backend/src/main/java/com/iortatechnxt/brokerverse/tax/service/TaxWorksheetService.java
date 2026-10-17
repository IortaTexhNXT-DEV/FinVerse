package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point of the tax computations: builds the worksheet of a kind for a period from posted
 * documents (see {@link VatWorksheetBuilder} and {@link EwtWorksheetBuilder} for the rules and
 * assumptions of each).
 */
@Service
@Transactional(readOnly = true)
public class TaxWorksheetService {

  private final VatWorksheetBuilder vat;
  private final EwtWorksheetBuilder ewt;

  /**
   * Creates the service.
   *
   * @param vat VAT builder
   * @param ewt withholding builder
   */
  public TaxWorksheetService(VatWorksheetBuilder vat, EwtWorksheetBuilder ewt) {
    this.vat = vat;
    this.ewt = ewt;
  }

  /**
   * Computes a worksheet.
   *
   * @param companyId company
   * @param kind worksheet kind
   * @param period period
   * @return worksheet
   * @throws BusinessRuleException for {@link WorksheetKind#NONE}
   */
  public TaxWorksheet compute(Long companyId, WorksheetKind kind, TaxPeriod period) {
    return switch (kind) {
      case VAT -> vat.build(companyId, period);
      case EWT -> ewt.build(companyId, period);
      case NONE ->
          throw new BusinessRuleException(
              "NO_WORKSHEET",
              "This form is prepared outside " + BrandAssets.SYSTEM_NAME + " and has no worksheet");
    };
  }
}
