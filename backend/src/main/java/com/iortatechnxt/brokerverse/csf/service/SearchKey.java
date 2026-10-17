package com.iortatechnxt.brokerverse.csf.service;

/** Key types of the Customer Search (BRCSF-003 / 3.001; e-mail topic 4). */
public enum SearchKey {
  /** Client name (client master). */
  NAME("Name"),
  /** Client code, prospect code or government ID number. */
  CLIENT_ID("Client ID"),
  /** ARN with or without its suffix, policy number or legacy account number. */
  ACCOUNT_NO("Account No."),
  /** Promissory note number of the account. */
  PN_NO("PN No."),
  /** Loan application number of the account or billing item. */
  APPLICATION_NO("Application No.");

  private final String label;

  SearchKey(String label) {
    this.label = label;
  }

  /**
   * Label shown to the agent.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
