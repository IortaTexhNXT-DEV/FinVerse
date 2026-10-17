package com.iortatechnxt.brokerverse.renewal.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of an action on several renewals (select single, multiple, all except, all): the renewals
 * done and the refused ones with the reason, so one refusal never blocks the others.
 *
 * @param done renewal references done
 * @param refused renewal reference to the reason it was refused
 */
public record BatchOutcome(List<String> done, Map<String, String> refused) {

  /** Defensive copies. */
  public BatchOutcome {
    done = List.copyOf(done);
    refused = Map.copyOf(refused);
  }

  /** Collects an outcome. */
  public static final class Builder {
    private final List<String> done = new ArrayList<>();
    private final Map<String, String> refused = new LinkedHashMap<>();

    /**
     * Records a renewal done.
     *
     * @param ref renewal reference
     */
    public void done(String ref) {
      done.add(ref);
    }

    /**
     * Records a refusal.
     *
     * @param ref renewal reference
     * @param reason reason
     */
    public void refused(String ref, String reason) {
      refused.put(ref, reason);
    }

    /**
     * The outcome.
     *
     * @return outcome
     */
    public BatchOutcome build() {
      return new BatchOutcome(done, refused);
    }
  }
}
