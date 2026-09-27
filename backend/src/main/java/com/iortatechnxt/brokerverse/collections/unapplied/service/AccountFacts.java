package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.InvoiceCategory;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The account fields of the unapplied list resolved from the matched invoice (BRCLXN.036): the
 * collection item when the invoice is listed, else the invoice ledger.
 *
 * @param assuredName name of the assured
 * @param prBalance outstanding premium receivable
 * @param inceptionDate inception
 * @param segment market segment
 * @param salesUnit sales unit
 * @param unitHead unit head (username)
 * @param aoUsername account officer (username)
 * @param insurerCode insurer
 * @param invoiceCategory REGULAR or DIRECT_BILL
 * @param handler collection handler, may be null
 */
public record AccountFacts(
    String assuredName,
    BigDecimal prBalance,
    LocalDate inceptionDate,
    String segment,
    String salesUnit,
    String unitHead,
    String aoUsername,
    String insurerCode,
    String invoiceCategory,
    String handler) {

  /**
   * The facts of a listed collection item.
   *
   * @param i item
   * @return facts
   */
  public static AccountFacts of(CollectionItem i) {
    return new AccountFacts(
        i.getParties().assuredName(),
        i.getFigures().total(),
        i.getClassification().inceptionDate(),
        i.getClassification().segment(),
        i.getClassification().salesUnit(),
        i.getClassification().unitHeadUsername(),
        i.getClassification().aoUsername(),
        i.getParties().insurerCode(),
        i.getClassification().invoiceCategory().name(),
        i.getCurrentHandler());
  }

  /**
   * The facts of a ledger invoice not listed in Collections.
   *
   * @param i invoice
   * @return facts (no unit head, no handler)
   */
  public static AccountFacts of(OpsInvoice i) {
    return new AccountFacts(
        i.getAssuredName(),
        i.premiumBalance(),
        i.getClassification().inceptionDate(),
        i.getClassification().segment(),
        i.getClassification().salesUnit(),
        null,
        i.getClassification().aoUsername(),
        i.getInsurerCode(),
        (i.isDpFlag() ? InvoiceCategory.DIRECT_BILL : InvoiceCategory.REGULAR).name(),
        null);
  }
}
