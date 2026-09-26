package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Numbers of the non-package flow, gap-free per year (BRNB.006/008/017/102): the PRF marketing
 * reference, the ARN, the quotation slip and the proposal slip. The prefixes are business
 * parameters while the BDOI numbering format is open (UX-1, Q15).
 */
@Component
public class ProposalNumbers {

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
  public ProposalNumbers(
      DocumentNumberService numbers, SystemParameterService parameters, Clock clock) {
    this.numbers = numbers;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Next PRF marketing reference (BRNB.006).
   *
   * @return e.g. PRF-2026-000001
   */
  public String prf() {
    return series("PROPOSAL_NUMBER_PREFIX", "PRF");
  }

  /**
   * Next quotation slip number (BRNB.008).
   *
   * @return e.g. QS-2026-000001
   */
  public String quotationSlip() {
    return series("QUOTATION_SLIP_PREFIX", "QS");
  }

  /**
   * Next proposal slip number (BRNB.017).
   *
   * @return e.g. PS-2026-000001
   */
  public String proposalSlip() {
    return series("PROPOSAL_SLIP_PREFIX", "PS");
  }

  /**
   * Next Account Reference Number (BRNB.102), shared with quotations and accounts.
   *
   * @return ARN-yyyy-nnnnnn
   */
  public String arn() {
    return numbers.next("ARN-" + LocalDate.now(clock).getYear());
  }

  private String series(String parameter, String fallback) {
    return numbers.next(
        parameters.text(parameter, fallback) + "-" + LocalDate.now(clock).getYear());
  }
}
