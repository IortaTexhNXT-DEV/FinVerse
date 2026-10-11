package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntryRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSetRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersionRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MapVersionStatus;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the APPROVED versions of code map sets for a validation or a loader (the versions current
 * when the batch is validated, FR-DM-011 R1).
 */
@Component
public class CodeMapLoader {

  private final CodeMapSetRepository sets;
  private final CodeMapVersionRepository versions;
  private final CodeMapEntryRepository entries;

  /**
   * Creates the loader.
   *
   * @param sets sets
   * @param versions versions
   * @param entries entries
   */
  public CodeMapLoader(
      CodeMapSetRepository sets,
      CodeMapVersionRepository versions,
      CodeMapEntryRepository entries) {
    this.sets = sets;
    this.versions = versions;
    this.entries = entries;
  }

  /**
   * The approved maps of the sets.
   *
   * @param setCodes sets
   * @return maps (sets without an approved version are absent)
   */
  @Transactional(readOnly = true)
  public CodeMaps approved(Collection<String> setCodes) {
    Map<String, CodeMaps.SetMap> out = new HashMap<>();
    for (String code : setCodes) {
      List<CodeMapVersion> approved =
          versions.findBySetCodeAndStatus(code, MapVersionStatus.APPROVED);
      if (approved.isEmpty()) {
        continue;
      }
      CodeMapVersion v = approved.get(0);
      String defaultTarget = sets.findByCode(code).map(CodeMapSet::getDefaultTarget).orElse(null);
      List<CodeMapEntry> list =
          entries.findByVersionIdOrderBySourceSystemAscLegacyCodeAscIdAsc(v.getId());
      out.put(code, new CodeMaps.SetMap(v.getVersionNo(), defaultTarget, list));
    }
    return new CodeMaps(out);
  }

  /**
   * The approved maps of every set whose code starts with a prefix (for example {@code LOV:}).
   *
   * @param prefix prefix
   * @return maps
   */
  @Transactional(readOnly = true)
  public CodeMaps approvedWithPrefix(String prefix) {
    Map<String, CodeMapSet> byCode =
        sets.findAllByOrderByCodeAsc().stream()
            .filter(s -> s.getCode().startsWith(prefix))
            .collect(Collectors.toMap(CodeMapSet::getCode, Function.identity()));
    return approved(byCode.keySet());
  }
}
