package com.iortatechnxt.brokerverse.submitted.service.adapter;

import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import java.util.Optional;

/**
 * Default {@link RenewalHandOff} before the Renewal module implements it (wave R3; cross-BRD
 * decision D2): the hand-off is only recorded as PENDING - no renewal account, hold cover or letter
 * - so there is never a second renewal path. The expiry scan offers the pending hand-offs again, so
 * they are replayed once the Renewal adapter is deployed.
 */
public class PendingRenewalHandOff implements RenewalHandOff {

  @Override
  public HandOffResult handOff(HandOffRequest request) {
    return new HandOffResult(
        Outcome.PENDING,
        null,
        null,
        "Hand-off pending: the renewal starts when the Renewal module takes it");
  }

  @Override
  public Optional<HandOffStatus> status(Long companyId, String sbmNo) {
    return Optional.empty();
  }
}
