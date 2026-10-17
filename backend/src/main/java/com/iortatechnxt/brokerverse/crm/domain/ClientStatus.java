package com.iortatechnxt.brokerverse.crm.domain;

/** Client lifecycle (BRNB.101). */
public enum ClientStatus {
  /** Minimum data captured; may be quoted but not placed or booked. */
  PROSPECT,
  /** Onboarded: KYC verified and client code issued. */
  CONFIRMED,
  /** No longer used for new business. */
  INACTIVE
}
