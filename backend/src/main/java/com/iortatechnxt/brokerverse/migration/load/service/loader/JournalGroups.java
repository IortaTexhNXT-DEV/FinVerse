package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Groups the line units of a trial balance or of adjustment journals into one unit per journal to
 * post (branch and currency): the first line is the main row, the other lines are carried as {@link
 * #LINES} children and as aliases (each line keeps its cross-reference), and the child rows of the
 * grouped lines (open-item detail) are kept once.
 */
final class JournalGroups {

  /** Child key of the other lines of a group. */
  static final String LINES = "LINES";

  private JournalGroups() {}

  /**
   * Groups units.
   *
   * @param units line units
   * @param key group key of a unit (e.g. version, branch and currency)
   * @return one unit per group
   */
  static List<LoadUnit> group(List<LoadUnit> units, Function<LoadUnit, String> key) {
    Map<String, List<LoadUnit>> groups = new LinkedHashMap<>();
    units.forEach(u -> groups.computeIfAbsent(key.apply(u), k -> new ArrayList<>()).add(u));
    List<LoadUnit> out = new ArrayList<>();
    for (List<LoadUnit> g : groups.values()) {
      LoadUnit first = g.get(0);
      Map<String, List<StageRow>> children = new LinkedHashMap<>();
      Set<Long> seen = new HashSet<>();
      List<LoadUnit.Alias> aliases = new ArrayList<>();
      for (int i = 0; i < g.size(); i++) {
        LoadUnit u = g.get(i);
        if (i > 0) {
          children.computeIfAbsent(LINES, k -> new ArrayList<>()).add(u.main());
          aliases.add(new LoadUnit.Alias(u.sourceSystem(), u.legacyKey(), u.hash()));
        }
        u.children()
            .forEach(
                (layout, rows) ->
                    rows.stream()
                        .filter(r -> seen.add(r.getId()))
                        .forEach(
                            r -> children.computeIfAbsent(layout, k -> new ArrayList<>()).add(r)));
      }
      out.add(first.with(children, aliases));
    }
    return out;
  }

  /**
   * Every line of a grouped unit, main row first.
   *
   * @param unit grouped unit
   * @return line rows
   */
  static List<StageRow> lines(LoadUnit unit) {
    List<StageRow> all = new ArrayList<>();
    all.add(unit.main());
    all.addAll(unit.rows(LINES));
    return all;
  }
}
