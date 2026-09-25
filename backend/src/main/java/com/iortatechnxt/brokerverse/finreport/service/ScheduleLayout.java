package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.AccountRef;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The columns and notes of an account schedule (FRBS 3.2.0): the row code and name headed after the
 * grouping, the figures of the definition, the ageing buckets and the commentary column; notes on
 * the accounts, the currency, the ageing method and a layout still to confirm (AQ05).
 */
final class ScheduleLayout {

  /** Key prefix of the ageing bucket columns. */
  static final String AGE_PREFIX = "age";

  private ScheduleLayout() {}

  static List<ReportColumn> columns(ScheduleDefinition def, AgeingSlots slots) {
    String[] headings = headings(def.getGrouping());
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(ScheduleEngine.CODE, headings[0]));
    columns.add(ReportColumn.text(ScheduleEngine.NAME, headings[1]));
    for (ScheduleColumn c : def.getColumns()) {
      columns.add(
          c.measure() == Measure.VARIANCE_PCT
              ? ReportColumn.percent(ScheduleEngine.key(c), c.label())
              : ReportColumn.amount(ScheduleEngine.key(c), c.label()));
    }
    if (slots != null) {
      List<String> ages = slots.labels();
      for (int i = 0; i < ages.size(); i++) {
        columns.add(ReportColumn.amount(AGE_PREFIX + i, ages.get(i) + " days"));
      }
    }
    if (def.isCommentary()) {
      columns.add(ReportColumn.text(ScheduleEngine.COMMENT, "Commentary"));
    }
    return columns;
  }

  private static String[] headings(Grouping grouping) {
    return switch (grouping) {
      case ACCOUNT -> new String[] {"Account", "Account name"};
      case PARTY -> new String[] {"Party", "Party name"};
      case DOCUMENT -> new String[] {"Party", "Document"};
      case COST_CENTER -> new String[] {"Cost centre", "Cost centre name"};
      case BRANCH -> new String[] {"Branch", "Branch name"};
      case BUSINESS_LINE -> new String[] {"Line of business", "Name"};
    };
  }

  static List<String> notes(ScheduleDefinition def, List<AccountRef> accounts, AgeingSlots slots) {
    List<String> notes = new ArrayList<>();
    notes.add(
        accounts.isEmpty()
            ? "No postable account matches " + def.values().accountSelector()
            : "Accounts: "
                + accounts.stream().map(AccountRef::code).collect(Collectors.joining(", ")));
    notes.add(
        def.getCurrency() == null
            ? "Amounts in base currency, every transaction currency"
            : "Amounts in "
                + def.getCurrency()
                + ", transactions in "
                + def.getCurrency()
                + " only");
    if (slots != null) {
      notes.add(
          "Ageing "
              + slots.describe()
              + " days, first in first out: the balance is made of the most recent increases");
    }
    if (def.getLayoutStatus() == LayoutStatus.TO_CONFIRM) {
      notes.add("Draft layout, to be confirmed with FRBS (AQ05)");
    }
    return notes;
  }
}
