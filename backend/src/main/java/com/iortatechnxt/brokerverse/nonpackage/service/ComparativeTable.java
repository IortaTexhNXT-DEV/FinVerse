package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The comparative table of a PRF (BRNB.010): the insurers approached side by side with their terms,
 * compiled automatically from the responses; received terms first, cheapest first, with the
 * recommended insurer and the lowest premium flagged.
 *
 * @param rows one row per insurer
 * @param recommendedInsurer insurer flagged as recommended, may be null
 */
public record ComparativeTable(List<Row> rows, String recommendedInsurer) {

  /** Defensive copy. */
  public ComparativeTable {
    rows = List.copyOf(rows);
  }

  /**
   * Compiles the table from the responses.
   *
   * @param responses insurer responses
   * @return table
   */
  public static ComparativeTable of(List<InsurerResponse> responses) {
    BigDecimal lowest =
        responses.stream()
            .filter(r -> r.getStatus() == ResponseStatus.RECEIVED)
            .map(InsurerResponse::getPremium)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    List<Row> rows =
        responses.stream()
            .sorted(
                Comparator.comparing(
                        (InsurerResponse r) -> r.getStatus() != ResponseStatus.RECEIVED)
                    .thenComparing(
                        InsurerResponse::getPremium,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .map(
                r ->
                    new Row(
                        r.getInsurerCode(),
                        r.getInsurerName(),
                        r.getStatus(),
                        r.getPremium(),
                        r.getRate(),
                        r.getDeductibles(),
                        r.getConditions(),
                        r.getValidUntil(),
                        r.getRemarks(),
                        r.isRecommended(),
                        lowest != null
                            && r.getPremium() != null
                            && r.getStatus() == ResponseStatus.RECEIVED
                            && r.getPremium().compareTo(lowest) == 0))
            .toList();
    return new ComparativeTable(
        rows,
        responses.stream()
            .filter(InsurerResponse::isRecommended)
            .map(InsurerResponse::getInsurerCode)
            .findFirst()
            .orElse(null));
  }

  /**
   * Terms of one insurer.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param status response status
   * @param premium premium
   * @param rate rate in percent
   * @param deductibles deductibles
   * @param conditions conditions
   * @param validUntil validity
   * @param remarks remarks
   * @param recommended recommended by TSU
   * @param lowest lowest premium received
   */
  public record Row(
      String insurerCode,
      String insurerName,
      ResponseStatus status,
      BigDecimal premium,
      BigDecimal rate,
      String deductibles,
      String conditions,
      LocalDate validUntil,
      String remarks,
      boolean recommended,
      boolean lowest) {}
}
