package com.iortatechnxt.brokerverse.eb.domain;

/** Who owes a tracked item (BRID-030). */
public enum EbResponsibleParty {
  /** The insurer or HMO provider. */
  INSURER,
  /** The client (HR). */
  CLIENT,
  /** BDOI itself. */
  BDOI
}
