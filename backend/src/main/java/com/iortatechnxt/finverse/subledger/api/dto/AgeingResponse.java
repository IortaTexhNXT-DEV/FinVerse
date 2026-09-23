package com.iortatechnxt.finverse.subledger.api.dto;

import com.iortatechnxt.finverse.subledger.service.AgeingBucket;
import com.iortatechnxt.finverse.subledger.service.AgeingService.AgeingRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ageing of outstanding open items by party (base currency, receivables positive).
 *
 * @param asOf as-of date
 * @param buckets bucket labels in column order
 * @param rows one row per party
 */
public record AgeingResponse(LocalDate asOf, List<String> buckets, List<Row> rows) {

  /**
   * Maps service rows.
   *
   * @param asOf as-of date
   * @param buckets buckets
   * @param rows rows
   * @return response
   */
  public static AgeingResponse from(
      LocalDate asOf, List<AgeingBucket> buckets, List<AgeingRow> rows) {
    return new AgeingResponse(
        asOf,
        buckets.stream().map(AgeingBucket::label).toList(),
        rows.stream().map(r -> new Row(r.partyCode(), r.buckets(), r.total())).toList());
  }

  /**
   * One party.
   *
   * @param partyCode party
   * @param amounts amount per bucket
   * @param total net outstanding
   */
  public record Row(String partyCode, List<BigDecimal> amounts, BigDecimal total) {}
}
