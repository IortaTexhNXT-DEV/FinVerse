package com.iortatechnxt.brokerverse.consolidation.api.dto;

import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationMember;
import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidation group view.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param parentCompanyId parent company
 * @param currency consolidation currency
 * @param ctaAccount translation reserve account
 * @param nciAccount non-controlling interest account
 * @param goodwillAccount goodwill account
 * @param active active flag
 * @param members subsidiaries
 */
public record GroupResponse(
    Long id,
    String code,
    String name,
    Long parentCompanyId,
    String currency,
    String ctaAccount,
    String nciAccount,
    String goodwillAccount,
    boolean active,
    List<Member> members) {

  /**
   * Maps an entity.
   *
   * @param g group
   * @return view
   */
  public static GroupResponse from(ConsolidationGroup g) {
    return new GroupResponse(
        g.getId(),
        g.getCode(),
        g.getName(),
        g.getParentCompanyId(),
        g.getCurrency(),
        g.getCtaAccount(),
        g.getNciAccount(),
        g.getGoodwillAccount(),
        g.isActive(),
        g.getMembers().stream().map(Member::from).toList());
  }

  /**
   * Subsidiary view.
   *
   * @param companyId company
   * @param ownershipPct ownership percent
   * @param investmentAccount parent's investment account
   * @param equityAccounts subsidiary capital accounts
   */
  public record Member(
      Long companyId,
      BigDecimal ownershipPct,
      String investmentAccount,
      List<String> equityAccounts) {

    /**
     * Maps a member.
     *
     * @param m member
     * @return view
     */
    public static Member from(ConsolidationMember m) {
      return new Member(
          m.getCompanyId(), m.getOwnershipPct(), m.getInvestmentAccount(), m.equityAccountCodes());
    }
  }
}
