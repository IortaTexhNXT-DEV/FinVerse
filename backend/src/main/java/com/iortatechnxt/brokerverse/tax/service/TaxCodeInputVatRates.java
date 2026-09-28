package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payables.service.InputVatRates;
import com.iortatechnxt.brokerverse.tax.domain.TaxCode;
import com.iortatechnxt.brokerverse.tax.domain.TaxCodeRepository;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The input VAT rate of supplier invoices from the tax code master: the active VAT_INPUT code of
 * the company effective on the invoice date (the most recent one when several are).
 */
@Service
@Transactional(readOnly = true)
public class TaxCodeInputVatRates implements InputVatRates {

  private final TaxCodeRepository codes;

  /**
   * Creates the adapter.
   *
   * @param codes tax codes
   */
  public TaxCodeInputVatRates(TaxCodeRepository codes) {
    this.codes = codes;
  }

  @Override
  public BigDecimal ratePercent(Long companyId, LocalDate date) {
    return codes.findByCompanyIdOrderByTaxTypeAscCodeAsc(companyId).stream()
        .filter(c -> c.getTaxType() == TaxType.VAT_INPUT && c.isActive() && c.isEffectiveOn(date))
        .max(Comparator.comparing(TaxCode::getEffectiveFrom))
        .map(TaxCode::getRate)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_INPUT_VAT_CODE",
                    "The company has no active input VAT tax code on "
                        + date
                        + "; maintain it under Tax codes"));
  }
}
