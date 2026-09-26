package com.iortatechnxt.brokerverse.tax.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Worksheet documents totalled per counterparty — the rows of the Summary Lists of Sales and
 * Purchases.
 *
 * @param partyCode counterparty
 * @param partyName registered name
 * @param tin TIN as printed
 * @param exempt exempt amount
 * @param zeroRated zero-rated amount
 * @param services taxable sales, or purchases of services
 * @param capitalGoods purchases of capital goods (purchases only)
 * @param tax output or input VAT
 */
public record PartySummary(
    String partyCode,
    String partyName,
    String tin,
    BigDecimal exempt,
    BigDecimal zeroRated,
    BigDecimal services,
    BigDecimal capitalGoods,
    BigDecimal tax) {

  private static final String CAPITAL_GOODS = "CAPITAL_GOODS";

  /**
   * Totals documents per counterparty, in party code order.
   *
   * @param documents documents of one worksheet section
   * @return one summary per party
   */
  public static List<PartySummary> of(List<TaxDocumentLine> documents) {
    Map<String, List<TaxDocumentLine>> byParty = new TreeMap<>();
    documents.forEach(d -> byParty.computeIfAbsent(d.partyCode(), k -> new ArrayList<>()).add(d));
    List<PartySummary> out = new ArrayList<>();
    byParty.forEach(
        (code, docs) -> {
          TaxDocumentLine first = docs.get(0);
          List<TaxDocumentLine> capital =
              docs.stream().filter(d -> CAPITAL_GOODS.equals(d.taxCode())).toList();
          List<TaxDocumentLine> other =
              docs.stream().filter(d -> !CAPITAL_GOODS.equals(d.taxCode())).toList();
          out.add(
              new PartySummary(
                  code,
                  first.partyName(),
                  first.tin(),
                  WorksheetSupport.sum(docs, TaxDocumentLine::exemptAmount),
                  WorksheetSupport.sum(docs, TaxDocumentLine::zeroRatedAmount),
                  WorksheetSupport.sum(other, TaxDocumentLine::taxableAmount),
                  WorksheetSupport.sum(capital, TaxDocumentLine::taxableAmount),
                  WorksheetSupport.sum(docs, TaxDocumentLine::taxAmount)));
        });
    return out;
  }
}
