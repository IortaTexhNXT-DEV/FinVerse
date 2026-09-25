package com.iortatechnxt.brokerverse.security.domain;

/**
 * One changed attribute of an access change.
 *
 * @param subjectType user or role
 * @param subject user name or role code
 * @param activity what was done
 * @param attribute changed attribute (e.g. {@code roles}, {@code email}, {@code permissions})
 * @param from value before (null when created)
 * @param to value after (null when removed)
 */
public record AccessChange(
    AccessSubjectType subjectType,
    String subject,
    AccessChangeActivity activity,
    String attribute,
    String from,
    String to) {}
