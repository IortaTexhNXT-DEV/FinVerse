package com.iortatechnxt.finverse.claims.domain;

/** Document that produced a claim movement line. */
public enum MovementSource {
  /** Reserve change (entered by a user, or a system release). */
  RESERVE,
  /** Claim settlement. */
  SETTLEMENT,
  /** Salvage / subrogation recovery. */
  RECOVERY
}
