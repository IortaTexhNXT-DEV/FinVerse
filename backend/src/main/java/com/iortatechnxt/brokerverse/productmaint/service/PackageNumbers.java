package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Numbers of the package request process, gap-free per year (BRPM.008/012): the request number
 * PKR-yyyy-nnnnnn and the quotation slip number PQS-yyyy-nnnnnn. The prefixes are the business
 * parameters PKG_REQUEST_PREFIX and PKG_QS_PREFIX (V755) while the BDOI numbering is open (UX-1).
 */
@Component
public class PackageNumbers {

  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the numbering.
   *
   * @param numbers document number series
   * @param parameters business parameters
   * @param clock clock
   */
  public PackageNumbers(
      DocumentNumberService numbers, SystemParameterService parameters, Clock clock) {
    this.numbers = numbers;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Next package request number (BRPM.008).
   *
   * @return e.g. PKR-2026-000001
   */
  public String request() {
    return series("PKG_REQUEST_PREFIX", "PKR-");
  }

  /**
   * Next package quotation slip number (BRPM.012).
   *
   * @return e.g. PQS-2026-000001
   */
  public String quotationSlip() {
    return series("PKG_QS_PREFIX", "PQS-");
  }

  private String series(String parameter, String fallback) {
    return numbers.next(parameters.text(parameter, fallback) + LocalDate.now(clock).getYear());
  }
}
