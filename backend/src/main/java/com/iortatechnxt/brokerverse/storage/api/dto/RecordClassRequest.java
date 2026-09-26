package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.RecordClassSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * New settings of a record class.
 *
 * @param retentionRecordType record type of the retention rules (optional)
 * @param retentionPeriod fallback retention, e.g. P20Y
 * @param legalHold under legal hold from the start
 * @param archiveToEcm archived to ECM when final
 * @param active accepts new files
 */
public record RecordClassRequest(
    @Size(max = 40) String retentionRecordType,
    @NotBlank @Pattern(regexp = "P[0-9]+[YMD]") String retentionPeriod,
    boolean legalHold,
    boolean archiveToEcm,
    boolean active) {

  /**
   * The settings.
   *
   * @return settings
   */
  public RecordClassSettings toSettings() {
    return new RecordClassSettings(
        retentionRecordType, retentionPeriod, legalHold, archiveToEcm, active);
  }
}
