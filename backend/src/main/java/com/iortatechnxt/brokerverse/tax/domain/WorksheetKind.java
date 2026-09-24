package com.iortatechnxt.brokerverse.tax.domain;

/**
 * Computation worksheet behind a tax form. {@link #NONE} marks forms prepared outside BrokerVerse
 * (e.g. 1601-C from the payroll system) that appear on the calendar as reminders only.
 */
public enum WorksheetKind {
  VAT(null),
  EWT(null),
  DST(TaxType.DST),
  PREMIUM_TAX(TaxType.PREMIUM_TAX),
  LGT(TaxType.LGT),
  FST(TaxType.FST),
  NONE(null);

  private final TaxType premiumLevy;

  WorksheetKind(TaxType premiumLevy) {
    this.premiumLevy = premiumLevy;
  }

  /**
   * Premium levy computed by this worksheet.
   *
   * @return tax type for the DST, premium tax, LGT and FST worksheets, else null
   */
  public TaxType premiumLevy() {
    return premiumLevy;
  }

  /**
   * Worksheet of a premium levy.
   *
   * @param type DST, PREMIUM_TAX, LGT or FST
   * @return worksheet kind
   */
  public static WorksheetKind ofLevy(TaxType type) {
    for (WorksheetKind k : values()) {
      if (type != null && k.premiumLevy == type) {
        return k;
      }
    }
    throw new IllegalArgumentException("Not a premium levy: " + type);
  }
}
