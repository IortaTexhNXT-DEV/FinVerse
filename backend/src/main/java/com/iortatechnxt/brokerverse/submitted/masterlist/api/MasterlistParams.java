package com.iortatechnxt.brokerverse.submitted.masterlist.api;

import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistFilter;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistFilter.Tab;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the masterlist (FRS FR-SP-010, 012).
 *
 * @param companyId company
 * @param tab tab (default ALL)
 * @param q masterlist, PN or policy number, assured or borrower
 * @param segment segment
 * @param businessType NB or RB
 * @param bucket bucket
 * @param expiryMonth expiry month (yyyy-MM)
 * @param insurer insurer
 * @param handler handler
 * @param conversionStatus conversion status
 * @param migrated migrated only (true) or not migrated (false)
 * @param changedSince changed on or after this date
 */
public record MasterlistParams(
    Long companyId,
    Tab tab,
    String q,
    String segment,
    String businessType,
    String bucket,
    String expiryMonth,
    String insurer,
    String handler,
    String conversionStatus,
    Boolean migrated,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changedSince) {

  /**
   * The service filter.
   *
   * @return filter
   */
  public MasterlistFilter filter() {
    return new MasterlistFilter(
        companyId,
        tab == null ? Tab.ALL : tab,
        q,
        segment,
        businessType,
        bucket,
        expiryMonth == null || expiryMonth.isBlank() ? null : YearMonth.parse(expiryMonth),
        insurer,
        handler,
        conversionStatus,
        migrated,
        changedSince);
  }
}
