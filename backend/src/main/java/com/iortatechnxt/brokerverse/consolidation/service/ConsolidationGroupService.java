package com.iortatechnxt.brokerverse.consolidation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.consolidation.api.dto.GroupRequest;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationGroupRepository;
import com.iortatechnxt.brokerverse.consolidation.domain.MemberValues;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consolidation group master: parent, subsidiaries, ownership and group accounts. */
@Service
@Transactional
public class ConsolidationGroupService {

  private static final String ENTITY = "ConsolidationGroup";

  private final ConsolidationGroupRepository groups;
  private final OrganizationService organization;
  private final ChartOfAccountsService accounts;
  private final CurrencyService currencies;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param groups group repository
   * @param organization organization service
   * @param accounts chart of accounts
   * @param currencies currency service
   * @param audit audit trail
   */
  public ConsolidationGroupService(
      ConsolidationGroupRepository groups,
      OrganizationService organization,
      ChartOfAccountsService accounts,
      CurrencyService currencies,
      AuditTrailService audit) {
    this.groups = groups;
    this.organization = organization;
    this.accounts = accounts;
    this.currencies = currencies;
    this.audit = audit;
  }

  /**
   * Lists groups.
   *
   * @return groups with members
   */
  @Transactional(readOnly = true)
  public List<ConsolidationGroup> list() {
    return groups.findAllByOrderByCode();
  }

  /**
   * Gets a group.
   *
   * @param id id
   * @return group with members
   */
  @Transactional(readOnly = true)
  public ConsolidationGroup get(Long id) {
    return groups.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Gets a group by code.
   *
   * @param code code
   * @return group with members
   */
  @Transactional(readOnly = true)
  public ConsolidationGroup getByCode(String code) {
    return groups.findByCode(code).orElseThrow(() -> new ResourceNotFoundException(ENTITY, code));
  }

  /**
   * Creates a group.
   *
   * @param r request
   * @return group
   */
  public ConsolidationGroup create(GroupRequest r) {
    if (groups.existsByCode(r.code())) {
      throw new DuplicateResourceException(ENTITY, r.code());
    }
    ConsolidationGroup group =
        new ConsolidationGroup(r.code(), r.name().trim(), r.parentCompanyId(), r.currency());
    apply(group, r);
    ConsolidationGroup saved = groups.save(group);
    audit.record(ENTITY, r.code(), AuditAction.CREATE, "Created consolidation group " + r.code());
    return saved;
  }

  /**
   * Updates name, currency, accounts, state and members (the parent cannot change).
   *
   * @param id id
   * @param r request
   * @return group
   */
  public ConsolidationGroup update(Long id, GroupRequest r) {
    ConsolidationGroup group = get(id);
    group.setName(r.name().trim());
    group.setCurrency(r.currency());
    apply(group, r);
    audit.record(ENTITY, group.getCode(), AuditAction.UPDATE, "Updated consolidation group");
    return group;
  }

  private void apply(ConsolidationGroup group, GroupRequest r) {
    organization.requireActiveCompany(group.getParentCompanyId());
    currencies.requireActive(r.currency());
    List<MemberValues> members = r.members().stream().map(GroupRequest.Member::values).toList();
    for (MemberValues m : members) {
      organization.requireActiveCompany(m.companyId());
      if (m.investmentAccount() != null) {
        accounts.getByCode(group.getParentCompanyId(), m.investmentAccount());
      }
      m.equityAccounts().forEach(code -> accounts.getByCode(m.companyId(), code));
    }
    group.setAccounts(r.ctaAccount().trim(), r.nciAccount().trim(), r.goodwillAccount().trim());
    group.setActive(r.active());
    group.replaceMembers(members);
  }
}
