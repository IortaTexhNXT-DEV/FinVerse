package com.iortatechnxt.brokerverse.party.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.party.api.dto.PartyRequest;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyRepository;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.time.Clock;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business partner master maintenance (maker-checker) and lookups for other modules. */
@Service
@Transactional
public class PartyService {

  private static final String PARTY = "Party";

  private final PartyRepository parties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param parties repository
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PartyService(
      PartyRepository parties, AuditTrailService audit, CurrentUser currentUser, Clock clock) {
    this.parties = parties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists or searches parties.
   *
   * @param companyId company
   * @param types types to include (empty = all)
   * @param term optional search term
   * @return parties ordered by name
   */
  @Transactional(readOnly = true)
  public List<Party> search(Long companyId, Collection<PartyType> types, String term) {
    Collection<PartyType> effective = types.isEmpty() ? EnumSet.allOf(PartyType.class) : types;
    if (term == null || term.isBlank()) {
      return parties.findByCompanyIdAndPartyTypeInOrderByName(companyId, effective);
    }
    return parties.search(companyId, term.trim().toLowerCase(Locale.ROOT), effective);
  }

  /**
   * Gets a party.
   *
   * @param id id
   * @return party
   */
  @Transactional(readOnly = true)
  public Party get(Long id) {
    return parties.findById(id).orElseThrow(() -> new ResourceNotFoundException(PARTY, id));
  }

  /**
   * Gets a party by code.
   *
   * @param companyId company
   * @param code code
   * @return party
   */
  @Transactional(readOnly = true)
  public Party getByCode(Long companyId, String code) {
    return parties
        .findByCompanyIdAndCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException(PARTY, code));
  }

  /**
   * Returns an active party of one of the expected types, or fails.
   *
   * @param companyId company
   * @param code code
   * @param expected allowed types
   * @return party
   */
  @Transactional(readOnly = true)
  public Party requireActive(Long companyId, String code, Collection<PartyType> expected) {
    Party party = getByCode(companyId, code);
    if (!party.isActive()) {
      throw new BusinessRuleException("INACTIVE_PARTY", "Party " + code + " is not active");
    }
    if (!expected.contains(party.getPartyType())) {
      throw new BusinessRuleException(
          "WRONG_PARTY_TYPE",
          "Party " + code + " is a " + party.getPartyType() + ", expected " + expected);
    }
    return party;
  }

  /**
   * Creates a party (pending authorization).
   *
   * @param r request
   * @return party
   */
  public Party create(PartyRequest r) {
    if (parties.existsByCompanyIdAndCode(r.companyId(), r.code())) {
      throw new DuplicateResourceException(PARTY, r.code());
    }
    Party party = new Party(r.companyId(), r.code(), r.name(), r.partyType(), r.defaultCurrency());
    apply(party, r);
    Party saved = parties.save(party);
    audit.record(
        PARTY,
        saved.getCode(),
        AuditAction.CREATE,
        "Created " + saved.getPartyType() + " " + saved.getName());
    return saved;
  }

  /**
   * Updates a party; it returns to pending authorization.
   *
   * @param id id
   * @param r request
   * @return party
   */
  public Party update(Long id, PartyRequest r) {
    Party party = get(id);
    party.setName(r.name());
    party.setDefaultCurrency(r.defaultCurrency());
    apply(party, r);
    party.markModified();
    audit.record(PARTY, party.getCode(), AuditAction.UPDATE, "Updated party");
    return party;
  }

  /**
   * Authorizes a party.
   *
   * @param id id
   * @return party
   */
  public Party authorize(Long id) {
    Party party = get(id);
    party.authorize(currentUser.username(), clock.instant());
    audit.record(PARTY, party.getCode(), AuditAction.AUTHORIZE, "Authorized party");
    return party;
  }

  /**
   * Creates a party and authorizes it as a system step in the same transaction. Used when the party
   * is the by-product of a business record already approved under four eyes, e.g. the sub-ledger
   * party of a KYC-verified client confirmed in the crm module (BRNB.090).
   *
   * @param r request
   * @param reason why no separate authorization is needed (audited)
   * @return the active party
   */
  public Party createAuthorizedBySystem(PartyRequest r, String reason) {
    Party party = create(r);
    party.authorize(CurrentUser.SYSTEM, clock.instant());
    audit.record(
        PARTY, party.getCode(), AuditAction.AUTHORIZE, "Authorized by the system: " + reason);
    return party;
  }

  private static void apply(Party p, PartyRequest r) {
    p.setTaxId(r.taxId());
    p.setAddress(r.address());
    p.setEmail(r.email());
    p.setPhone(r.phone());
    p.setCreditDays(r.creditDays());
    p.setCommissionRate(r.commissionRate());
    p.setWithholdingTaxRate(r.withholdingTaxRate());
    p.setLicenceNo(r.licenceNo());
    p.setBankName(r.bankName());
    p.setBankAccountNo(r.bankAccountNo());
    p.setBranchId(r.branchId());
  }
}
