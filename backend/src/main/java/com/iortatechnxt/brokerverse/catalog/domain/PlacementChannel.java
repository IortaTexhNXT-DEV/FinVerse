package com.iortatechnxt.brokerverse.catalog.domain;

/** How placements reach an insurer. Only e-mail is built; SFTP and API are parked (Q06). */
public enum PlacementChannel {
  /** Placement slip sent by e-mail. */
  EMAIL,
  /** Secure file transfer (parked, Q06). */
  SFTP,
  /** Insurer API (parked, Q06). */
  API
}
