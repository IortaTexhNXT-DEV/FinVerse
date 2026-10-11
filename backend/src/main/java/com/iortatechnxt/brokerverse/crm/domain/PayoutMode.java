package com.iortatechnxt.brokerverse.crm.domain;

/** How a client is paid out (MKT 2.25.0, Addendum 1). */
public enum PayoutMode {
  /** Credit to the client's BDO current or savings account (CA / SA). */
  CTA,
  /** Check issued to a payee name. */
  CHECK
}
