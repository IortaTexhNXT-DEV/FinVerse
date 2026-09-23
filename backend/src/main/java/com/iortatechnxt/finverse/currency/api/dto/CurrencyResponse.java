package com.iortatechnxt.finverse.currency.api.dto;

import com.iortatechnxt.finverse.currency.domain.Currency;

/**
 * Currency view.
 *
 * @param code code
 * @param name name
 * @param symbol symbol
 * @param decimalPlaces decimal places
 * @param active active flag
 */
public record CurrencyResponse(
    String code, String name, String symbol, int decimalPlaces, boolean active) {

  /**
   * Maps an entity.
   *
   * @param c currency
   * @return response
   */
  public static CurrencyResponse from(Currency c) {
    return new CurrencyResponse(
        c.getCode(), c.getName(), c.getSymbol(), c.getDecimalPlaces(), c.isActive());
  }
}
