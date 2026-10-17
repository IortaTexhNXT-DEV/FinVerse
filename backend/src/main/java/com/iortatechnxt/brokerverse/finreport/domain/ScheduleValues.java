package com.iortatechnxt.brokerverse.finreport.domain;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.ScheduleFamily;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import java.util.List;
import java.util.Locale;

/**
 * The maintainable content of an account schedule definition (FRBS 3.2.0).
 *
 * @param name title
 * @param family Appendix A family
 * @param sourceRef where BDOI names the report (appendix row, report list number)
 * @param description description
 * @param selectorKind account prefixes or report groups
 * @param accountSelector comma-separated prefixes or report groups
 * @param grouping rows
 * @param currency one currency shown in that currency, null for every currency in base currency
 * @param side side shown as positive
 * @param basis balance or movement
 * @param ageingSlots ageing bucket bounds ("30,90,180"), null without ageing
 * @param comparative comparative period
 * @param commentary whether a commentary column is kept per row and month
 * @param boardDocument whether the schedule is a board-deck document (Word output requested)
 * @param layoutStatus confirmed or to confirm (AQ05)
 * @param active whether the schedule is offered
 * @param columns figures in order
 */
public record ScheduleValues(
    String name,
    ScheduleFamily family,
    String sourceRef,
    String description,
    SelectorKind selectorKind,
    String accountSelector,
    Grouping grouping,
    String currency,
    Side side,
    Basis basis,
    String ageingSlots,
    Comparative comparative,
    boolean commentary,
    boolean boardDocument,
    LayoutStatus layoutStatus,
    boolean active,
    List<ScheduleColumn> columns) {

  /** Trims texts, blanks become null, upper-case currency, copies the columns. */
  public ScheduleValues {
    name = trim(name);
    sourceRef = trim(sourceRef);
    description = trim(description);
    accountSelector = trim(accountSelector);
    currency = trim(currency) == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    ageingSlots = trim(ageingSlots);
    comparative = comparative == null ? Comparative.NONE : comparative;
    layoutStatus = layoutStatus == null ? LayoutStatus.TO_CONFIRM : layoutStatus;
    columns = columns == null ? List.of() : List.copyOf(columns);
  }

  private static String trim(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  /**
   * The selector entries.
   *
   * @return prefixes or report groups, trimmed, without blanks
   */
  public List<String> selectorEntries() {
    return accountSelector == null
        ? List.of()
        : List.of(accountSelector.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
  }
}
