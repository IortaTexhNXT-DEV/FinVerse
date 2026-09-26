package com.iortatechnxt.brokerverse.screening.watchlist.domain;

/**
 * Counts of an ingestion run (SNSRP-201 "volumes").
 *
 * @param received records read
 * @param added new entries
 * @param updated changed entries
 * @param delisted entries delisted (missing from a full file)
 * @param unchanged records identical to the list
 * @param failed records that failed validation
 */
public record RunCounts(
    int received, int added, int updated, int delisted, int unchanged, int failed) {}
