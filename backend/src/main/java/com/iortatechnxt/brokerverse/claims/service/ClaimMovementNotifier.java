package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.MovementLine;
import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementListener;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Notifies every {@link ClaimMovementListener} bean (reinsurance, for example) of each posted claim
 * movement, synchronously and inside the posting transaction: a listener failure rolls the claim
 * transaction back, keeping the gross and ceded books consistent. With no listener deployed this is
 * a no-op.
 */
@Component
public class ClaimMovementNotifier {

  private final List<ClaimMovementListener> listeners;

  /**
   * Creates the notifier.
   *
   * @param listeners listener beans (may be empty)
   */
  public ClaimMovementNotifier(List<ClaimMovementListener> listeners) {
    this.listeners = List.copyOf(listeners);
  }

  /**
   * Notifies the kernel movements of a posted line.
   *
   * @param line posted movement line
   */
  public void publish(MovementLine line) {
    if (listeners.isEmpty()) {
      return;
    }
    for (ClaimMovement movement : KernelMovements.of(line)) {
      listeners.forEach(l -> l.onClaimMovement(movement));
    }
  }
}
