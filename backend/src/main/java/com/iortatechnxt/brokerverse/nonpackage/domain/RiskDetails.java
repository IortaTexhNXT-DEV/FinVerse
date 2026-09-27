package com.iortatechnxt.brokerverse.nonpackage.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import java.util.List;

/**
 * Risk details of a PRF (BRNB.005): free-form sections (occupancy, loss history, requested cover,
 * special conditions...) and the risk items, each in a risk group that becomes one account.
 *
 * @param sections free-form sections
 * @param items risk items
 */
public record RiskDetails(List<Section> sections, List<Item> items) {

  /** No details. */
  public static final RiskDetails EMPTY = new RiskDetails(List.of(), List.of());

  /** Defensive copies. */
  public RiskDetails {
    sections = sections == null ? List.of() : List.copyOf(sections);
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * Risk groups of the items, ascending (1 when there is no item).
   *
   * @return groups
   */
  @JsonIgnore
  public List<Integer> groups() {
    List<Integer> groups = items.stream().map(Item::riskGroup).distinct().sorted().toList();
    return groups.isEmpty() ? List.of(1) : groups;
  }

  /**
   * Items of a risk group.
   *
   * @param group risk group
   * @return items
   */
  public List<RiskItemData> itemsOf(int group) {
    return items.stream().filter(i -> i.riskGroup() == group).map(Item::data).toList();
  }

  /**
   * A free-form section.
   *
   * @param heading heading
   * @param text text
   */
  public record Section(String heading, String text) {}

  /**
   * A risk item.
   *
   * @param riskGroup risk group (account)
   * @param data risk data
   */
  public record Item(int riskGroup, RiskItemData data) {}
}
