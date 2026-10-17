package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry.EntryData;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MapVersionStatus;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Legacy codes given on a configuration upload (legacy branch codes, legacy GL accounts) go into
 * the draft version of their code map (BRANCH, GL_ACCOUNT) of the Migration Console, created from
 * the approved version when there is none; the Data Steward submits it and the owner approves it
 * there as usual.
 */
@Component
public class LegacyCodeMaps {

  /** Source system of the entries: the code applies to every legacy system. */
  private static final String ANY_SOURCE = "*";

  private final CodeMapService maps;

  /**
   * Creates the helper.
   *
   * @param maps code maps
   */
  public LegacyCodeMaps(CodeMapService maps) {
    this.maps = maps;
  }

  /**
   * The legacy codes of a cell ("001, 002").
   *
   * @param cell cell, may be null
   * @return codes
   */
  public static List<String> codes(String cell) {
    if (cell == null || cell.isBlank()) {
      return List.of();
    }
    return Arrays.stream(cell.split("[,;]")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }

  /**
   * Why legacy codes cannot be added to a code map now (a version waits for approval).
   *
   * @param setCode code map set
   * @return message, empty when they can be added
   */
  public Optional<String> blocked(String setCode) {
    boolean submitted =
        maps.versions(setCode).stream().anyMatch(v -> v.getStatus() == MapVersionStatus.SUBMITTED);
    return submitted
        ? Optional.of(
            "the code map "
                + setCode
                + " waits for approval; add the legacy codes after its decision")
        : Optional.empty();
  }

  /**
   * Maps legacy codes to a target code in the draft of a code map.
   *
   * @param companyId company of the approval work case of a new draft
   * @param setCode code map set
   * @param legacy legacy codes
   * @param target BIBS code
   * @param description description of the legacy codes
   * @param jobNo upload number
   */
  public void map(
      Long companyId,
      String setCode,
      List<String> legacy,
      String target,
      String description,
      String jobNo) {
    if (legacy.isEmpty()) {
      return;
    }
    CodeMapVersion draft =
        maps.versions(setCode).stream()
            .filter(v -> v.getStatus() == MapVersionStatus.DRAFT)
            .findFirst()
            .orElseGet(
                () ->
                    maps.createDraft(companyId, setCode, true, "Legacy codes of upload " + jobNo));
    List<CodeMapEntry> entries = maps.entries(draft.getId());
    for (String code : legacy) {
      Long existing =
          entries.stream()
              .filter(e -> ANY_SOURCE.equals(e.getSourceSystem()) && code.equals(e.getLegacyCode()))
              .map(CodeMapEntry::getId)
              .findFirst()
              .orElse(null);
      maps.saveEntry(
          draft.getId(),
          existing,
          new EntryData(
              ANY_SOURCE,
              code,
              description,
              null,
              null,
              EntryAction.MAP,
              target,
              "From upload " + jobNo));
    }
  }
}
