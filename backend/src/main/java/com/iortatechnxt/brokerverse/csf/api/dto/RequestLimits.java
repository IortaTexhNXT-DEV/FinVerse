package com.iortatechnxt.brokerverse.csf.api.dto;

/** Lengths of the texts of the CSF request bodies (column lengths of the csf tables). */
final class RequestLimits {

  /** Longest remarks or reason. */
  static final int REMARKS = 500;

  /** Longest field value or address. */
  static final int VALUE = 300;

  private RequestLimits() {}
}
