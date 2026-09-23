package com.iortatechnxt.finverse.claims;

import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Test listener bean: records every claim movement it is notified of and counts deliveries per
 * reference, so tests can check that each movement reaches listeners exactly once.
 */
@Component
public class RecordingClaimListener implements ClaimMovementListener {

  private final List<ClaimMovement> movements = Collections.synchronizedList(new ArrayList<>());
  private final Map<String, Integer> deliveries = Collections.synchronizedMap(new HashMap<>());

  @Override
  public void onClaimMovement(ClaimMovement movement) {
    movements.add(movement);
    deliveries.merge(movement.reference(), 1, Integer::sum);
  }

  /** Movements received for one claim, in order. */
  public List<ClaimMovement> of(Long claimId) {
    synchronized (movements) {
      return movements.stream().filter(m -> m.claimId().equals(claimId)).toList();
    }
  }

  /** How often a reference was delivered. */
  public int deliveries(String reference) {
    return deliveries.getOrDefault(reference, 0);
  }
}
