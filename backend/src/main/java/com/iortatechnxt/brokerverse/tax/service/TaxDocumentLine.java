package com.iortatechnxt.brokerverse.tax.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One source document behind a worksheet figure (drill-down row), amounts in base currency.
 *
 * @param section worksheet section: SALES, PURCHASES, WITHHOLDING or PREMIUMS
 * @param sourceType POLICY, ENDORSEMENT, COMMISSION or SUPPLIER_INVOICE
 * @param sourceId policy id (policy documents) or supplier invoice id
 * @param documentNo policy / endorsement / internal invoice number
 * @param documentDate accounting date of the document (approval or invoice date)
 * @param partyCode counterparty (customer, supplier or intermediary)
 * @param partyName counterparty name
 * @param tin counterparty TIN as printed (123-456-789-000)
 * @param taxCode ATC (withholding), VAT class (VATABLE, SERVICES, CAPITAL_GOODS, ZERO_RATED,
 *     EXEMPT) or tax type (premium levies)
 * @param incomeNature nature of the income payment (withholding rows), else null
 * @param businessLine line of business (premium documents), else null
 * @param taxableAmount tax base (taxable sales, income payment, premium)
 * @param exemptAmount exempt sales / purchases
 * @param zeroRatedAmount zero-rated sales / purchases
 * @param taxAmount tax
 * @param rate effective rate in percent (tax / base)
 */
public record TaxDocumentLine(
    String section,
    String sourceType,
    Long sourceId,
    String documentNo,
    LocalDate documentDate,
    String partyCode,
    String partyName,
    String tin,
    String taxCode,
    String incomeNature,
    String businessLine,
    BigDecimal taxableAmount,
    BigDecimal exemptAmount,
    BigDecimal zeroRatedAmount,
    BigDecimal taxAmount,
    BigDecimal rate) {

  /** Sales (output VAT) section. */
  public static final String SALES = "SALES";

  /** Purchases (input VAT) section. */
  public static final String PURCHASES = "PURCHASES";

  /** Income payments subject to withholding. */
  public static final String WITHHOLDING = "WITHHOLDING";

  /** Premiums subject to a premium levy. */
  public static final String PREMIUMS = "PREMIUMS";
}
