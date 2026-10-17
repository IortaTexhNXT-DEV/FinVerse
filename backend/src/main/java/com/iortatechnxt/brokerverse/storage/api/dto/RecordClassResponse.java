package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.RecordClass;

/**
 * A record class.
 *
 * @param code code
 * @param name name
 * @param bucketClass bucket class
 * @param retentionRecordType record type of the retention rules
 * @param retentionPeriod fallback retention
 * @param legalHold under legal hold from the start
 * @param archiveToEcm archived to ECM when final
 * @param active accepts new files
 * @param description description
 */
public record RecordClassResponse(
    String code,
    String name,
    String bucketClass,
    String retentionRecordType,
    String retentionPeriod,
    boolean legalHold,
    boolean archiveToEcm,
    boolean active,
    String description) {

  /**
   * Maps a class.
   *
   * @param c class
   * @return response
   */
  public static RecordClassResponse from(RecordClass c) {
    return new RecordClassResponse(
        c.getCode(),
        c.getName(),
        c.getBucketClass().name(),
        c.getRetentionRecordType(),
        c.getRetentionPeriod(),
        c.isLegalHold(),
        c.isArchiveToEcm(),
        c.isActive(),
        c.getDescription());
  }
}
