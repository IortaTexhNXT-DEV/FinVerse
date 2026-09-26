package com.iortatechnxt.brokerverse.consolidation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consolidation group: a parent company and its subsidiaries (with ownership), the consolidation
 * (reporting) currency and the group accounts used by translation and elimination entries.
 *
 * <p>The group chart of accounts is the parent's chart: member balances are aggregated by account
 * code, so members must use the same codes (standard chart).
 */
@Entity
@Table(name = "con_group")
public class ConsolidationGroup extends BaseEntity {

  @Column(nullable = false, length = 20, unique = true)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "parent_company_id", nullable = false)
  private Long parentCompanyId;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "cta_account", nullable = false, length = 30)
  private String ctaAccount;

  @Column(name = "nci_account", nullable = false, length = 30)
  private String nciAccount;

  @Column(name = "goodwill_account", nullable = false, length = 30)
  private String goodwillAccount;

  @Column(nullable = false)
  private boolean active = true;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<ConsolidationMember> members = new ArrayList<>();

  protected ConsolidationGroup() {}

  /**
   * Creates a group.
   *
   * @param code unique code
   * @param name name
   * @param parentCompanyId parent company
   * @param currency consolidation currency
   */
  public ConsolidationGroup(String code, String name, Long parentCompanyId, String currency) {
    this.code = code;
    this.name = name;
    this.parentCompanyId = parentCompanyId;
    this.currency = currency;
  }

  /**
   * Replaces the subsidiaries.
   *
   * @param subsidiaries member values
   */
  public void replaceMembers(List<MemberValues> subsidiaries) {
    Set<Long> seen = new HashSet<>();
    for (MemberValues m : subsidiaries) {
      if (m.companyId().equals(parentCompanyId) || !seen.add(m.companyId())) {
        throw new BusinessRuleException(
            "INVALID_GROUP_MEMBER",
            "Each subsidiary must be listed once and differ from the parent");
      }
    }
    // Update in place (unique group/company key: Hibernate inserts before it deletes).
    members.removeIf(existing -> !seen.contains(existing.getCompanyId()));
    for (MemberValues m : subsidiaries) {
      members.stream()
          .filter(existing -> existing.getCompanyId().equals(m.companyId()))
          .findFirst()
          .ifPresentOrElse(
              existing -> existing.update(m), () -> members.add(new ConsolidationMember(this, m)));
    }
  }

  /**
   * Parent plus subsidiary company ids.
   *
   * @return ids, parent first
   */
  public List<Long> companyIds() {
    List<Long> ids = new ArrayList<>();
    ids.add(parentCompanyId);
    members.forEach(m -> ids.add(m.getCompanyId()));
    return ids;
  }

  /**
   * Sets the group accounts.
   *
   * @param cta currency translation reserve (equity)
   * @param nci non-controlling interest (equity)
   * @param goodwill goodwill on consolidation (asset)
   */
  public void setAccounts(String cta, String nci, String goodwill) {
    this.ctaAccount = cta;
    this.nciAccount = nci;
    this.goodwillAccount = goodwill;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getParentCompanyId() {
    return parentCompanyId;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getCtaAccount() {
    return ctaAccount;
  }

  public String getNciAccount() {
    return nciAccount;
  }

  public String getGoodwillAccount() {
    return goodwillAccount;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<ConsolidationMember> getMembers() {
    return members;
  }
}
