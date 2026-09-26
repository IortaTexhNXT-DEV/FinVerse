package com.iortatechnxt.brokerverse.eb.domain;

/** How a cycle ended (design 4.2). */
public enum EbCycleOutcome {
  /** Renewed with the incumbent insurer(s). */
  RENEWED_INCUMBENT,
  /** Renewed and moved to other insurer(s). */
  MOVED,
  /** New business placed. */
  NEW_PLACED,
  /** Renewal not renewed. */
  NOT_RENEWED,
  /** Lost (list EB_LOST_REASON). */
  LOST
}
