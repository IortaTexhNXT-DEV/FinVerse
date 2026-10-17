package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The approved code maps a validation uses (DATA_MIGRATION_DESIGN section 7): per set, the entries
 * of its APPROVED version by source system and legacy code, with the default target of the set. An
 * entry of source system {@code *} applies to every source. The batch records the versions used.
 */
public final class CodeMaps {

  /** Source system of an entry that applies to every source. */
  public static final String ANY_SOURCE = "*";

  private final Map<String, SetMap> sets;

  CodeMaps(Map<String, SetMap> sets) {
    this.sets = Map.copyOf(sets);
  }

  /**
   * No maps (tests, objects without coded columns).
   *
   * @return empty maps
   */
  public static CodeMaps empty() {
    return new CodeMaps(Map.of());
  }

  /**
   * Resolves a legacy code.
   *
   * @param setCode set
   * @param source source system of the row
   * @param legacyCode legacy code
   * @return the resolution, empty when the set has no approved version or no entry for the code
   */
  public Optional<Resolution> resolve(String setCode, String source, String legacyCode) {
    SetMap set = sets.get(setCode);
    if (set == null || legacyCode == null) {
      return Optional.empty();
    }
    CodeMapEntry entry = set.find(source, legacyCode);
    if (entry == null) {
      return Optional.empty();
    }
    String target =
        entry.getAction() == EntryAction.DEFAULT ? set.defaultTarget() : entry.getTargetCode();
    return Optional.of(new Resolution(entry.getAction(), target, set.versionNo()));
  }

  /**
   * Whether the set has an approved version.
   *
   * @param setCode set
   * @return true when approved
   */
  public boolean approved(String setCode) {
    return sets.containsKey(setCode);
  }

  /**
   * The versions used, set code to version number.
   *
   * @return versions
   */
  public Map<String, Integer> versions() {
    Map<String, Integer> out = new HashMap<>();
    sets.forEach((code, set) -> out.put(code, set.versionNo()));
    return Collections.unmodifiableMap(out);
  }

  /**
   * The entries of an approved set (the PACKAGE map loaded as reference data).
   *
   * @param setCode set
   * @return entries
   */
  public List<CodeMapEntry> entries(String setCode) {
    SetMap set = sets.get(setCode);
    return set == null ? List.of() : set.all();
  }

  /**
   * How a legacy code resolves.
   *
   * @param action action of the entry
   * @param target target code (null for REJECT and a CREATE without target)
   * @param versionNo version used
   */
  public record Resolution(EntryAction action, String target, int versionNo) {}

  /** The approved entries of one set. */
  static final class SetMap {

    private final int versionNo;
    private final String defaultTarget;
    private final List<CodeMapEntry> all;
    private final Map<String, CodeMapEntry> byKey = new HashMap<>();

    SetMap(int versionNo, String defaultTarget, List<CodeMapEntry> entries) {
      this.versionNo = versionNo;
      this.defaultTarget = defaultTarget;
      this.all = List.copyOf(entries);
      for (CodeMapEntry e : entries) {
        byKey.putIfAbsent(key(e.getSourceSystem(), e.getLegacyCode()), e);
      }
    }

    CodeMapEntry find(String source, String legacyCode) {
      String code = legacyCode.strip();
      CodeMapEntry exact = byKey.get(key(source, code));
      if (exact != null) {
        return exact;
      }
      CodeMapEntry upper = byKey.get(key(source, code.toUpperCase(Locale.ROOT)));
      return upper != null ? upper : byKey.get(key(ANY_SOURCE, code));
    }

    int versionNo() {
      return versionNo;
    }

    String defaultTarget() {
      return defaultTarget;
    }

    List<CodeMapEntry> all() {
      return all;
    }

    private static String key(String source, String code) {
      return (source == null ? "" : source.toUpperCase(Locale.ROOT)) + "|" + code;
    }
  }
}
