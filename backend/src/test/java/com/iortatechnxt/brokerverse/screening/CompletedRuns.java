package com.iortatechnxt.brokerverse.screening;

import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningCompleted;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Collects the published screening results in tests (the view of the case wave). */
@Component
public class CompletedRuns {

  private final List<ScreeningResult> results = new ArrayList<>();

  @EventListener
  synchronized void on(ScreeningCompleted event) {
    results.add(event.result());
  }

  /**
   * The results that recorded matches of a client.
   *
   * @param clientId client
   * @return results
   */
  public synchronized List<ScreeningResult> of(Long clientId) {
    return results.stream().filter(r -> !r.matchesOf(clientId).isEmpty()).toList();
  }
}
