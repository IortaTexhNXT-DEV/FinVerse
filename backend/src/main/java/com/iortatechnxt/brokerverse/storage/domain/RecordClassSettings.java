package com.iortatechnxt.brokerverse.storage.domain;

/**
 * Maintainable settings of a record class.
 *
 * @param retentionRecordType record type of the retention rules (optional)
 * @param retentionPeriod fallback retention, ISO-8601 period such as {@code P20Y}
 * @param legalHold files of the class are under legal hold from the start
 * @param archiveToEcm final records of the class are archived to ECM
 * @param active the class accepts new files
 */
public record RecordClassSettings(
    String retentionRecordType,
    String retentionPeriod,
    boolean legalHold,
    boolean archiveToEcm,
    boolean active) {}
