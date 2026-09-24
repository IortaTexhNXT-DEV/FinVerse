package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Differences between two versions of a quotation (BRNB.020): changed terms, items added, removed
 * or changed, and the premium delta. Items are matched by their risk (plate, address, person or
 * description).
 *
 * @param fromVersion older version
 * @param toVersion newer version
 * @param fields changed terms
 * @param items item changes
 * @param grossFrom gross premium of the older version
 * @param grossTo gross premium of the newer version
 * @param grossDelta gross premium difference (newer minus older), null when either is unrated
 */
public record QuotationDiff(
    int fromVersion,
    int toVersion,
    List<FieldChange> fields,
    List<ItemChange> items,
    BigDecimal grossFrom,
    BigDecimal grossTo,
    BigDecimal grossDelta) {

  /** Defensive copies. */
  public QuotationDiff {
    fields = List.copyOf(fields);
    items = List.copyOf(items);
  }

  /**
   * Compares two versions.
   *
   * @param fromVersion older version number
   * @param from older content
   * @param toVersion newer version number
   * @param to newer content
   * @return differences
   */
  public static QuotationDiff between(
      int fromVersion, QuotationContent from, int toVersion, QuotationContent to) {
    List<FieldChange> fields = new ArrayList<>();
    field(fields, "Insurer", from, to, QuotationContent::insurerCode);
    field(fields, "Insurer branch", from, to, QuotationContent::insurerBranch);
    field(fields, "Period from", from, to, QuotationContent::periodFrom);
    field(fields, "Period to", from, to, QuotationContent::periodTo);
    field(fields, "Valid until", from, to, QuotationContent::validUntil);
    field(fields, "Direct payment", from, to, QuotationContent::directPayment);
    field(fields, "Rating basis", from, to, QuotationContent::ratingBasis);
    field(fields, "Remarks", from, to, QuotationContent::remarks);
    BigDecimal grossFrom = from.premium().grossPremium();
    BigDecimal grossTo = to.premium().grossPremium();
    return new QuotationDiff(
        fromVersion,
        toVersion,
        fields,
        items(from, to),
        grossFrom,
        grossTo,
        grossFrom == null || grossTo == null ? null : grossTo.subtract(grossFrom));
  }

  private static void field(
      List<FieldChange> changes,
      String name,
      QuotationContent from,
      QuotationContent to,
      Function<QuotationContent, Object> value) {
    Object before = value.apply(from);
    Object after = value.apply(to);
    if (!Objects.equals(before, after)) {
      changes.add(new FieldChange(name, text(before), text(after)));
    }
  }

  private static List<ItemChange> items(QuotationContent from, QuotationContent to) {
    Map<String, QuotationItem> before = byRisk(from);
    Map<String, QuotationItem> after = byRisk(to);
    List<ItemChange> changes = new ArrayList<>();
    before.forEach(
        (key, old) -> {
          QuotationItem now = after.get(key);
          if (now == null) {
            changes.add(ItemChange.of(Change.REMOVED, key, old, null));
          } else if (changed(old, now)) {
            changes.add(ItemChange.of(Change.CHANGED, key, old, now));
          }
        });
    after.forEach(
        (key, now) -> {
          if (!before.containsKey(key)) {
            changes.add(ItemChange.of(Change.ADDED, key, null, now));
          }
        });
    return changes;
  }

  private static boolean changed(QuotationItem a, QuotationItem b) {
    return a.riskGroup() != b.riskGroup()
        || !Objects.equals(a.data(), b.data())
        || !same(a.premium(), b.premium());
  }

  private static boolean same(BigDecimal a, BigDecimal b) {
    return a == null ? b == null : b != null && a.compareTo(b) == 0;
  }

  private static Map<String, QuotationItem> byRisk(QuotationContent content) {
    Map<String, QuotationItem> map = new LinkedHashMap<>();
    for (QuotationItem item : content.items()) {
      String label = QuotationPricing.label(item.data());
      String key = label;
      int n = 2;
      while (map.containsKey(key)) {
        key = label + " (" + n++ + ")";
      }
      map.put(key, item);
    }
    return map;
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }

  /** Kind of item change. */
  public enum Change {
    /** In the newer version only. */
    ADDED,
    /** In the older version only. */
    REMOVED,
    /** In both, with different data, group or premium. */
    CHANGED
  }

  /**
   * A changed term.
   *
   * @param field field name
   * @param from older value
   * @param to newer value
   */
  public record FieldChange(String field, String from, String to) {}

  /**
   * A changed item.
   *
   * @param change added, removed or changed
   * @param risk risk label
   * @param fromGroup older risk group
   * @param toGroup newer risk group
   * @param fromSumInsured older sum insured
   * @param toSumInsured newer sum insured
   * @param fromPremium older premium
   * @param toPremium newer premium
   */
  public record ItemChange(
      Change change,
      String risk,
      Integer fromGroup,
      Integer toGroup,
      BigDecimal fromSumInsured,
      BigDecimal toSumInsured,
      BigDecimal fromPremium,
      BigDecimal toPremium) {

    static ItemChange of(Change change, String risk, QuotationItem from, QuotationItem to) {
      return new ItemChange(
          change,
          risk,
          from == null ? null : from.riskGroup(),
          to == null ? null : to.riskGroup(),
          from == null ? null : QuotationPricing.sumInsured(from.data()),
          to == null ? null : QuotationPricing.sumInsured(to.data()),
          from == null ? null : from.premium(),
          to == null ? null : to.premium());
    }
  }
}
