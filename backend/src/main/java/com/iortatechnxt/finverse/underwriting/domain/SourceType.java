package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.party.domain.PartyType;
import java.util.Optional;

/** Distribution channel through which a policy was sourced. */
public enum SourceType {
  DIRECT(null),
  AGENT(PartyType.AGENT),
  BROKER(PartyType.BROKER);

  private final PartyType intermediaryType;

  SourceType(PartyType intermediaryType) {
    this.intermediaryType = intermediaryType;
  }

  /**
   * Party type the intermediary must have.
   *
   * @return type, empty for direct business
   */
  public Optional<PartyType> intermediaryType() {
    return Optional.ofNullable(intermediaryType);
  }

  /**
   * Whether an intermediary (who earns commission) is involved.
   *
   * @return true for agent and broker business
   */
  public boolean isIntermediated() {
    return intermediaryType != null;
  }
}
