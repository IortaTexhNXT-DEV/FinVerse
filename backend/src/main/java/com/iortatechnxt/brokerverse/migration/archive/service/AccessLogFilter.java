package com.iortatechnxt.brokerverse.migration.archive.service;

import java.time.LocalDate;

/**
 * The filter of the access log.
 *
 * @param username user
 * @param action SEARCH, VIEW, DOWNLOAD or EXPORT
 * @param from accessed from (business date)
 * @param to accessed to (business date, inclusive)
 */
public record AccessLogFilter(String username, String action, LocalDate from, LocalDate to) {}
