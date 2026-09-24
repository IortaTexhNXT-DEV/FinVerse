package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Numbers of the quotation flow, gap-free per year (BRNB.006/102): the ARN ({@code ARN-yyyy}), the
 * quotation number shown as Proposal No. and the request number. The quotation and request prefixes
 * are business parameters while the BDOI format is open (UX-1, Q15).
 */
@Component
public class QuotationNumbers {

  /** Parameter: prefix of quotation numbers. */
  public static final String QUOTATION_PREFIX = "QUOTATION_NUMBER_PREFIX";

  /** Parameter: prefix of request numbers. */
  public static final String REQUEST_PREFIX = "QUOTATION_REQUEST_PREFIX";

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
  public QuotationNumbers(
      DocumentNumberService numbers, SystemParameterService parameters, Clock clock) {
    this.numbers = numbers;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Next Account Reference Number.
   *
   * @return ARN-yyyy-nnnnnn
   */
  public String arn() {
    return numbers.next("ARN-" + year());
  }

  /**
   * Next quotation number.
   *
   * @return e.g. QT-2026-000001
   */
  public String quotation() {
    return numbers.next(parameters.text(QUOTATION_PREFIX, "QT") + "-" + year());
  }

  /**
   * Next request number.
   *
   * @return e.g. REQ-2026-000001
   */
  public String request() {
    return numbers.next(parameters.text(REQUEST_PREFIX, "REQ") + "-" + year());
  }

  private int year() {
    return LocalDate.now(clock).getYear();
  }
}
