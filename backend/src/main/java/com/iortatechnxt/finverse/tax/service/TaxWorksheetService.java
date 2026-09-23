package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point of the tax computations: builds the worksheet of a kind for a period from posted
 * documents (see {@link VatWorksheetBuilder}, {@link EwtWorksheetBuilder} and {@link
 * LevyWorksheetBuilder} for the rules and assumptions of each).
 */
@Service
@Transactional(readOnly = true)
public class TaxWorksheetService {

  private final VatWorksheetBuilder vat;
  private final EwtWorksheetBuilder ewt;
  private final LevyWorksheetBuilder levies;

  /**
   * Creates the service.
   *
   * @param vat VAT builder
   * @param ewt withholding builder
   * @param levies premium levy builder
   */
  public TaxWorksheetService(
      VatWorksheetBuilder vat, EwtWorksheetBuilder ewt, LevyWorksheetBuilder levies) {
    this.vat = vat;
    this.ewt = ewt;
    this.levies = levies;
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
              "NO_WORKSHEET", "This form is prepared outside FinVerse and has no worksheet");
      default -> levies.build(companyId, kind.premiumLevy(), period);
    };
  }
}
