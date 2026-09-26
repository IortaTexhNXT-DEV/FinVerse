package com.iortatechnxt.brokerverse.screening.config.api.dto;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersions;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A configuration version header (SNSRP-101-109).
 *
 * @param id id
 * @param companyId company
 * @param type configuration type
 * @param scope template type for TEMPLATE versions
 * @param versionNo number within the type and scope
 * @param label display label, e.g. "MATCH_CRITERIA v2"
 * @param status status
 * @param effectiveFrom effective date
 * @param changeNote change note
 * @param baseVersionId version compared with on submission
 * @param createdBy maker
 * @param createdAt created
 * @param submittedBy submitter
 * @param submittedAt submitted
 * @param decidedBy checker (or maker for a withdrawal)
 * @param decidedAt decided
 * @param decisionReason rejection reason
 */
public record ConfigVersionDto(
    Long id,
    Long companyId,
    ConfigType type,
    String scope,
    int versionNo,
    String label,
    ConfigStatus status,
    LocalDate effectiveFrom,
    String changeNote,
    Long baseVersionId,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    String decidedBy,
    Instant decidedAt,
    String decisionReason) {

  /**
   * Maps a version.
   *
   * @param v version
   * @return DTO
   */
  public static ConfigVersionDto from(ConfigVersion v) {
    return new ConfigVersionDto(
        v.getId(),
        v.getCompanyId(),
        v.getConfigType(),
        v.getScope(),
        v.getVersionNo(),
        ConfigVersions.label(v),
        v.getStatus(),
        v.getEffectiveFrom(),
        v.getChangeNote(),
        v.getBaseVersionId(),
        v.getCreatedBy(),
        v.getCreatedAt(),
        v.getSubmittedBy(),
        v.getSubmittedAt(),
        v.getDecidedBy(),
        v.getDecidedAt(),
        v.getDecisionReason());
  }
}
