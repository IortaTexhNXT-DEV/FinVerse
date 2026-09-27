package com.iortatechnxt.brokerverse.renewal.domain;

/** Decision of the insurer on a renewal (BRD 3.009.6; BRRN.035). */
public enum InsurerResponseCode {
  /** Renew as is. */
  RENEW_AS_IS,
  /** Renew with revised terms. */
  REVISE,
  /** Do not renew. */
  REJECT;
}
