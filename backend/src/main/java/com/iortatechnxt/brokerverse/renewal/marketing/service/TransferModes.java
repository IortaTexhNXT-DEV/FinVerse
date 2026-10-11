package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Component;

/**
 * Which transfer to another Marketing unit is in use, by {@value #MODE}: the ownership transfer of
 * the BRD (the renewal account moves to the receiving unit), the referral of BDOI's FRS (FRRN.011:
 * the receiving unit opens a New Business account, the renewal account keeps its owner) or both
 * (the default).
 */
@Component
public class TransferModes {

  /** Parameter: TRANSFER, REFERRAL or BOTH. */
  public static final String MODE = "RNW_TRANSFER_MODE";

  private final SystemParameterService parameters;

  /**
   * Creates the component.
   *
   * @param parameters system parameters
   */
  public TransferModes(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Whether the referral is in use.
   *
   * @return true unless the setting keeps the ownership transfer only
   */
  public boolean referral() {
    return !"TRANSFER".equals(mode());
  }

  /**
   * Whether the ownership transfer is in use.
   *
   * @return true unless the setting keeps the referral only
   */
  public boolean transfer() {
    return !"REFERRAL".equals(mode());
  }

  private String mode() {
    return parameters.text(MODE, "BOTH").strip();
  }
}
