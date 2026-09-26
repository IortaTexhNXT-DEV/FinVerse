package com.iortatechnxt.brokerverse.brokerclaims;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecorded;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Collects the {@link ClaimRecorded} events (the contract with the status engine of CL1-B). */
@Component
public class ClaimRecordedEvents {

  private final List<ClaimRecorded> events = new CopyOnWriteArrayList<>();

  @EventListener
  void on(ClaimRecorded event) {
    events.add(event);
  }

  /** The events received so far. */
  public List<ClaimRecorded> events() {
    return List.copyOf(events);
  }
}
