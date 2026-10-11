package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Criteria of the masterlist (BRIDSP-04, 29; FRS FR-SP-010, 012). Null fields do not filter.
 *
 * @param companyId company
 * @param tab tab of the work list
 * @param text masterlist, PN or policy number, or assured
 * @param segment segment
 * @param businessType NB or RB
 * @param bucket bucket
 * @param expiryMonth expiry month
 * @param insurerCode insurer
 * @param handler handler
 * @param conversionStatus conversion status
 * @param migrated migrated records only (true) or not migrated (false)
 * @param changedSince changed on or after this date
 */
public record MasterlistFilter(
    Long companyId,
    Tab tab,
    String text,
    String segment,
    String businessType,
    String bucket,
    YearMonth expiryMonth,
    String insurerCode,
    String handler,
    String conversionStatus,
    Boolean migrated,
    LocalDate changedSince) {

  /** Tabs of the Masterlist work list. */
  public enum Tab {
    /** Every record. */
    ALL,
    /** Received, waiting for validation. */
    FOR_VALIDATION,
    /** Classified or in review. */
    CLASSIFIED,
    /** For renewal or handed to Renewal. */
    FOR_RENEWAL,
    /** For manual disposition. */
    MANUAL_DISPOSITION,
    /** Excluded from renewal. */
    NON_RENEWAL,
    /** Fallout of the last run. */
    FALLOUT
  }

  /**
   * Every record of a company.
   *
   * @param companyId company
   * @return filter
   */
  public static MasterlistFilter all(Long companyId) {
    return new MasterlistFilter(
        companyId, Tab.ALL, null, null, null, null, null, null, null, null, null, null);
  }
}
