package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import org.springframework.stereotype.Component;

/**
 * {@code INSURER_USABLE}: the insurer of the renewal is authorized, active and accredited ({@code
 * InsurerService.requireUsableInsurer}).
 */
@Component
public class InsurerUsableCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "INSURER_USABLE";

  private final InsurerService insurers;

  /**
   * Creates the check.
   *
   * @param insurers insurer master
   */
  public InsurerUsableCheck(InsurerService insurers) {
    this.insurers = insurers;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    String insurer = c.getSnapshot().insurerCode();
    if (insurer == null) {
      return Verdict.fail("The renewal has no insurer", null);
    }
    try {
      insurers.requireUsableInsurer(c.getCompanyId(), insurer);
      return Verdict.pass("Insurer " + insurer + " is usable");
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return Verdict.fail(e.getMessage(), insurer);
    }
  }
}
