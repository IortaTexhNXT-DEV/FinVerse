package com.iortatechnxt.finverse.journal.domain;

/**
 * Journal (voucher) types.
 *
 * <p>{@code systemGenerated} journals originate from the accounting engine or period-end processes;
 * {@code adjustment} journals may post into periods that are soft-closed or reopened.
 */
public enum JournalType {
  MANUAL("JV", false, false),
  ADJUSTMENT("ADJ", false, true),
  ACCRUAL("ACR", false, true),
  PREMIUM("PRM", true, false),
  ENDORSEMENT("END", true, false),
  CLAIMS("CLM", true, false),
  REINSURANCE("RI", true, false),
  COMMISSION("COM", true, false),
  RECEIPT("RCT", true, false),
  PAYMENT("PAY", true, false),
  INVESTMENT("INV", true, false),
  PROVISION("PRV", true, true),
  REVALUATION("REV", true, true),
  REVERSAL("RVS", false, true),
  CLOSING("CLS", true, true),
  OPENING("OPN", true, true),
  CONSOLIDATION("CON", true, true);

  private final String prefix;
  private final boolean systemGenerated;
  private final boolean adjustment;

  JournalType(String prefix, boolean systemGenerated, boolean adjustment) {
    this.prefix = prefix;
    this.systemGenerated = systemGenerated;
    this.adjustment = adjustment;
  }

  public String prefix() {
    return prefix;
  }

  public boolean isSystemGenerated() {
    return systemGenerated;
  }

  /**
   * Whether this journal may post into soft-closed (CLOSING) or REOPENED periods.
   *
   * @return true for system generated or adjustment journals
   */
  public boolean isPrivileged() {
    return systemGenerated || adjustment;
  }
}
