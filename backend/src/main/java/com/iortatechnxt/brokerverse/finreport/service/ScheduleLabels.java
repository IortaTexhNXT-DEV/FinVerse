package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.AccountRef;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.RowFigures;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The code and name of the rows of an account schedule (FRBS 3.2.0): account code and name, party
 * code and name, party and document reference, cost centre or business line and its name, branch
 * code and name.
 */
@Component
public class ScheduleLabels {

  private static final String NONE = "(none)";
  private static final String DOCUMENT_SEPARATOR = "|";

  private final ScheduleQueries queries;
  private final FinReportQueries names;

  /**
   * Creates the labeller.
   *
   * @param queries branch names
   * @param names party and dimension names
   */
  public ScheduleLabels(ScheduleQueries queries, FinReportQueries names) {
    this.queries = queries;
    this.names = names;
  }

  /**
   * The code and name of every row.
   *
   * @param grouping rows
   * @param companyId company
   * @param accounts selected accounts
   * @param figures rows read
   * @return {code, name} per row key
   */
  public Map<String, String[]> labels(
      Grouping grouping, Long companyId, List<AccountRef> accounts, List<RowFigures> figures) {
    Map<String, String[]> labels = new LinkedHashMap<>();
    switch (grouping) {
      case ACCOUNT ->
          accounts.forEach(
              a -> labels.put(String.valueOf(a.id()), new String[] {a.code(), a.name()}));
      case BRANCH -> labels.putAll(queries.branchNames(companyId));
      case PARTY, DOCUMENT -> partyLabels(grouping, companyId, figures, labels);
      default -> {
        Map<String, String> dims = names.dimensionNames(companyId, grouping.name());
        figures.forEach(
            f ->
                labels.put(
                    f.key(), new String[] {orNone(f.key()), dims.getOrDefault(f.key(), "")}));
      }
    }
    return labels;
  }

  private void partyLabels(
      Grouping grouping, Long companyId, List<RowFigures> figures, Map<String, String[]> labels) {
    List<String> parties =
        figures.stream().map(f -> party(f.key())).filter(p -> !p.isEmpty()).distinct().toList();
    Map<String, String> partyNames = names.partyNames(companyId, parties);
    for (RowFigures f : figures) {
      String party = party(f.key());
      if (grouping == Grouping.PARTY) {
        labels.put(f.key(), new String[] {orNone(party), partyNames.getOrDefault(party, "")});
      } else {
        String document = f.key().substring(f.key().indexOf(DOCUMENT_SEPARATOR) + 1);
        labels.put(f.key(), new String[] {orNone(party), orNone(document)});
      }
    }
  }

  private static String party(String key) {
    int bar = key.indexOf(DOCUMENT_SEPARATOR);
    return bar < 0 ? key : key.substring(0, bar);
  }

  private static String orNone(String value) {
    return value == null || value.isEmpty() ? NONE : value;
  }
}
