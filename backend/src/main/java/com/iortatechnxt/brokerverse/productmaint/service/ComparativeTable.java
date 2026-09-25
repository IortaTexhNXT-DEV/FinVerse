package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The comparative table of a package request round (BRPM.014, PMADD03): the insurers approached
 * side by side with their outcome, rate, minimum premium, coverages, deductibles, conditions,
 * validity and remarks, compiled automatically from the responses. Answered insurers come first,
 * lowest rate first; the lowest rate is flagged. A client output shows a selection of the fields
 * and insurers of a master and never changes a value.
 *
 * @param roundNo round compiled
 * @param fields fields shown (catalogue {@link #FIELDS}), in catalogue order
 * @param rows one row per insurer
 */
public record ComparativeTable(int roundNo, List<String> fields, List<Row> rows) {

  /** Field catalogue of the comparative outputs (PQ06), with the column labels. */
  public static final Map<String, String> FIELDS = fieldCatalogue();

  private static final Set<String> NOT_OFFERED = Set.of("PENDING", "DECLINED", "NO_RESPONSE");

  /** Defensive copies. */
  public ComparativeTable {
    fields = List.copyOf(fields);
    rows = List.copyOf(rows);
  }

  private static Map<String, String> fieldCatalogue() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("OUTCOME", "Outcome");
    m.put("RATE", "Rate %");
    m.put("MINIMUM_PREMIUM", "Minimum Premium");
    m.put("COVERAGES", "Coverages");
    m.put("DEDUCTIBLES", "Deductibles");
    m.put("CONDITIONS", "Conditions / Warranties");
    m.put("VALID_UNTIL", "Valid Until");
    m.put("REMARKS", "Remarks");
    return Collections.unmodifiableMap(m);
  }

  /**
   * Compiles the table of a round from its responses.
   *
   * @param roundNo round
   * @param responses responses of the round
   * @param terms reads the coverage terms of a response (JSON)
   * @return table with every field
   */
  public static ComparativeTable compile(
      int roundNo,
      List<PackageInsurerResponse> responses,
      Function<String, List<CoverageTerm>> terms) {
    BigDecimal lowest =
        responses.stream()
            .filter(r -> offered(r.getOutcome()))
            .map(PackageInsurerResponse::getRate)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    List<Row> rows =
        responses.stream()
            .sorted(
                Comparator.comparing((PackageInsurerResponse r) -> !offered(r.getOutcome()))
                    .thenComparing(
                        PackageInsurerResponse::getRate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .map(r -> row(r, terms.apply(r.getTerms()), lowest))
            .toList();
    return new ComparativeTable(roundNo, List.copyOf(FIELDS.keySet()), rows);
  }

  private static Row row(PackageInsurerResponse r, List<CoverageTerm> terms, BigDecimal lowest) {
    return new Row(
        r.getInsurerCode(),
        r.getInsurerName(),
        r.getOutcome(),
        r.getRate(),
        r.getMinimumPremium(),
        terms.stream()
            .filter(CoverageTerm::included)
            .map(ComparativeTable::coverage)
            .collect(Collectors.joining("; ")),
        terms.stream()
            .filter(t -> !deductible(t).isEmpty())
            .map(t -> t.coverageCode() + ": " + deductible(t))
            .collect(Collectors.joining("; ")),
        r.getConditions(),
        r.getValidUntil(),
        r.getRemarks(),
        lowest != null
            && r.getRate() != null
            && offered(r.getOutcome())
            && r.getRate().compareTo(lowest) == 0);
  }

  /**
   * Whether an outcome is an offer (not pending, declined or unanswered).
   *
   * @param outcome outcome code
   * @return true when terms were offered
   */
  public static boolean offered(String outcome) {
    return outcome != null && !NOT_OFFERED.contains(outcome);
  }

  private static String coverage(CoverageTerm t) {
    return t.limitAmount() == null
        ? t.coverageCode()
        : t.coverageCode() + " (" + PackageDocuments.money(t.limitAmount()) + ")";
  }

  /**
   * A deductible as text.
   *
   * @param c coverage term
   * @return wording, amount and / or percent; empty when none
   */
  static String deductible(CoverageTerm c) {
    List<String> parts = new ArrayList<>();
    if (c.deductibleText() != null && !c.deductibleText().isBlank()) {
      parts.add(c.deductibleText().strip());
    }
    if (c.deductibleAmount() != null) {
      parts.add(PackageDocuments.money(c.deductibleAmount()));
    }
    if (c.deductiblePercent() != null) {
      parts.add(c.deductiblePercent().stripTrailingZeros().toPlainString() + "%");
    }
    return String.join(" / ", parts);
  }

  /**
   * A selection of this table (client output): the chosen fields in catalogue order and the chosen
   * insurers; empty lists keep everything.
   *
   * @param selection fields and insurers
   * @return selected table
   */
  public ComparativeTable select(Selection selection) {
    List<String> unknown = selection.fields().stream().filter(f -> !FIELDS.containsKey(f)).toList();
    if (!unknown.isEmpty()) {
      throw new BusinessRuleException(
          "COMPARATIVE_FIELD_UNKNOWN", "Unknown comparative field(s): " + unknown);
    }
    List<String> keptFields =
        selection.fields().isEmpty()
            ? fields
            : fields.stream().filter(selection.fields()::contains).toList();
    List<Row> keptRows =
        selection.insurers().isEmpty()
            ? rows
            : rows.stream().filter(r -> selection.insurers().contains(r.insurerCode())).toList();
    return new ComparativeTable(roundNo, keptFields, keptRows);
  }

  /**
   * Column headers: the insurer, then the fields shown.
   *
   * @return headers
   */
  public List<String> headers() {
    List<String> headers = new ArrayList<>();
    headers.add("Insurer");
    fields.forEach(f -> headers.add(FIELDS.get(f)));
    return headers;
  }

  /**
   * Cells as text, one list per insurer, in header order.
   *
   * @return cells
   */
  public List<List<String>> cells() {
    return rows.stream()
        .map(
            r -> {
              List<String> cells = new ArrayList<>();
              cells.add(r.insurerName() + (r.lowest() ? " (lowest rate)" : ""));
              fields.forEach(f -> cells.add(r.value(f)));
              return cells;
            })
        .toList();
  }

  /**
   * Terms of one insurer in the round.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param outcome outcome code
   * @param rate rate in percent
   * @param minimumPremium minimum premium
   * @param coverages included coverages with limits
   * @param deductibles deductibles per coverage
   * @param conditions conditions and warranties
   * @param validUntil validity
   * @param remarks remarks
   * @param lowest lowest rate offered
   */
  public record Row(
      String insurerCode,
      String insurerName,
      String outcome,
      BigDecimal rate,
      BigDecimal minimumPremium,
      String coverages,
      String deductibles,
      String conditions,
      LocalDate validUntil,
      String remarks,
      boolean lowest) {

    /**
     * The text of one field.
     *
     * @param field field code
     * @return text
     */
    public String value(String field) {
      return switch (field) {
        case "OUTCOME" -> outcome;
        case "RATE" -> PackageDocuments.text(rate);
        case "MINIMUM_PREMIUM" -> PackageDocuments.money(minimumPremium);
        case "COVERAGES" -> PackageDocuments.text(coverages);
        case "DEDUCTIBLES" -> PackageDocuments.text(deductibles);
        case "CONDITIONS" -> PackageDocuments.text(conditions);
        case "VALID_UNTIL" -> PackageDocuments.text(validUntil);
        default -> PackageDocuments.text(remarks);
      };
    }
  }

  /**
   * Fields and insurers of an output.
   *
   * @param fields field codes, empty for all
   * @param insurers insurer codes, empty for all
   */
  public record Selection(List<String> fields, List<String> insurers) {

    /** Everything (audit master). */
    public static final Selection ALL = new Selection(List.of(), List.of());

    /** Defensive copies. */
    public Selection {
      fields = fields == null ? List.of() : List.copyOf(fields);
      insurers = insurers == null ? List.of() : List.copyOf(insurers);
    }
  }
}
