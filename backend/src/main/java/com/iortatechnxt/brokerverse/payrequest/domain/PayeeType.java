package com.iortatechnxt.brokerverse.payrequest.domain;

/** Who a request pays (MKT 1.2.0). */
public enum PayeeType {
  /** A client (refund). */
  CLIENT,
  /** An employee (cash advance). */
  EMPLOYEE
}
