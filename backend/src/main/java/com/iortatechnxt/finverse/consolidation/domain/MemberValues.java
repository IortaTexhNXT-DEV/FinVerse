package com.iortatechnxt.finverse.consolidation.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Subsidiary settings of a consolidation group.
 *
 * @param companyId subsidiary company
 * @param ownershipPct parent's ownership in percent (0 &lt; x ≤ 100)
 * @param investmentAccount parent's investment-in-subsidiary account (null: no elimination)
 * @param equityAccounts subsidiary's share capital accounts eliminated against the investment
 */
public record MemberValues(
    Long companyId,
    BigDecimal ownershipPct,
    String investmentAccount,
    List<String> equityAccounts) {

  /** Canonical constructor copying the account list. */
  public MemberValues {
    equityAccounts = equityAccounts == null ? List.of() : List.copyOf(equityAccounts);
  }
}
