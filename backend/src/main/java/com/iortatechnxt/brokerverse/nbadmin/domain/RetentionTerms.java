package com.iortatechnxt.brokerverse.nbadmin.domain;

/**
 * Maintainable terms of a retention rule.
 *
 * @param statuses comma separated status codes
 * @param yearsOnline years a record stays online after its last activity
 * @param yearsArchive years a record stays in the archive afterwards
 * @param action REVIEW or ARCHIVE
 * @param active whether the rule is evaluated
 * @param description description
 */
public record RetentionTerms(
    String statuses,
    int yearsOnline,
    int yearsArchive,
    RetentionAction action,
    boolean active,
    String description) {}
