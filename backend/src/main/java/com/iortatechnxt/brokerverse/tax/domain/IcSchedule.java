package com.iortatechnxt.brokerverse.tax.domain;

/**
 * Insurance Commission statutory schedules built from the ledger (annual statement and quarterly
 * reports of a non-life company). Income-statement schedules (premiums, losses, commissions) are
 * period movements analysed by line of business; the others are balances as of a date.
 */
public enum IcSchedule {
  PREMIUMS("Premiums written per line of business", IcMeasure.MOVEMENT, true),
  LOSSES("Losses incurred per line of business", IcMeasure.MOVEMENT, true),
  COMMISSIONS("Commissions per line of business", IcMeasure.MOVEMENT, true),
  NET_WORTH("Net worth computation", IcMeasure.BALANCE, false),
  RBC("Risk-based capital summary", IcMeasure.BALANCE, false),
  RESERVES("Technical reserves", IcMeasure.BALANCE, false),
  INVESTMENTS("Investments", IcMeasure.BALANCE, false);

  private final String title;
  private final IcMeasure defaultMeasure;
  private final boolean byLineOfBusiness;

  IcSchedule(String title, IcMeasure defaultMeasure, boolean byLineOfBusiness) {
    this.title = title;
    this.defaultMeasure = defaultMeasure;
    this.byLineOfBusiness = byLineOfBusiness;
  }

  public String title() {
    return title;
  }

  public IcMeasure defaultMeasure() {
    return defaultMeasure;
  }

  public boolean byLineOfBusiness() {
    return byLineOfBusiness;
  }
}
