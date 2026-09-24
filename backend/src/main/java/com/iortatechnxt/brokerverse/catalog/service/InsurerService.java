package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch.BranchDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranchRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.party.api.dto.PartyRequest;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyRepository;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer panel (Q06 for channels): insurers are parties of type INSURER with a broking profile
 * (accreditation, placement mailboxes, credit days) and branches carrying the LGT rate (Appendix
 * A). Creating an insurer here also creates its party, pending authorization with the profile.
 */
@Service
@Transactional
public class InsurerService {

  private static final String BASE_CURRENCY = "PHP";

  private final InsurerProfileRepository insurers;
  private final InsurerBranchRepository branches;
  private final PartyService parties;
  private final PartyRepository partyRepository;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param insurers insurer profiles
   * @param branches insurer branches
   * @param parties party service
   * @param partyRepository party lookups
   * @param audit audit trail
   */
  public InsurerService(
      InsurerProfileRepository insurers,
      InsurerBranchRepository branches,
      PartyService parties,
      PartyRepository partyRepository,
      AuditTrailService audit) {
    this.insurers = insurers;
    this.branches = branches;
    this.parties = parties;
    this.partyRepository = partyRepository;
    this.audit = audit;
  }

  /**
   * Insurers of a company by name.
   *
   * @param companyId company
   * @return insurers
   */
  @Transactional(readOnly = true)
  public List<InsurerProfile> insurers(Long companyId) {
    return insurers.findByCompanyIdOrderByNameAsc(companyId);
  }

  /**
   * One insurer profile.
   *
   * @param id id
   * @return insurer
   */
  @Transactional(readOnly = true)
  public InsurerProfile get(Long id) {
    return insurers
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.INSURER.label(), id));
  }

  /**
   * An insurer by party code.
   *
   * @param companyId company
   * @param partyCode insurer party code
   * @return insurer
   */
  @Transactional(readOnly = true)
  public InsurerProfile requireInsurer(Long companyId, String partyCode) {
    return insurers
        .findByCompanyIdAndPartyCode(companyId, partyCode)
        .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.INSURER.label(), partyCode));
  }

  /**
   * Adds an insurer: its party (unless an INSURER party with the code exists) and its profile, both
   * pending authorization.
   *
   * @param companyId company
   * @param partyCode party code
   * @param contact tax id, address, e-mail and phone of the party
   * @param details profile
   * @return insurer
   */
  public InsurerProfile create(
      Long companyId, String partyCode, PartyContact contact, InsurerDetails details) {
    if (insurers.findByCompanyIdAndPartyCode(companyId, partyCode).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.INSURER.label(), partyCode);
    }
    Optional<Party> existing = partyRepository.findByCompanyIdAndCode(companyId, partyCode);
    if (existing.isPresent() && existing.get().getPartyType() != PartyType.INSURER) {
      throw new BusinessRuleException(
          "PARTY_NOT_INSURER", "Party " + partyCode + " exists and is not an insurer");
    }
    if (existing.isEmpty()) {
      parties.create(
          new PartyRequest(
              companyId,
              partyCode,
              details.name(),
              PartyType.INSURER,
              contact.taxId(),
              contact.address(),
              contact.email(),
              contact.phone(),
              BASE_CURRENCY,
              details.defaultCreditDays(),
              null,
              null,
              details.accreditationNo(),
              null,
              null,
              null));
    }
    InsurerProfile saved = insurers.save(new InsurerProfile(companyId, partyCode, details));
    audit.record(
        CatalogKind.INSURER.label(), partyCode, AuditAction.CREATE, "Added " + details.name());
    return saved;
  }

  /**
   * Changes an insurer profile, pending authorization.
   *
   * @param id profile
   * @param details profile
   * @return insurer
   */
  public InsurerProfile update(Long id, InsurerDetails details) {
    InsurerProfile insurer = get(id);
    insurer.update(details);
    audit.record(
        CatalogKind.INSURER.label(),
        insurer.getPartyCode(),
        AuditAction.UPDATE,
        "Changed " + details.name());
    return insurer;
  }

  /**
   * Branches of an insurer.
   *
   * @param insurerId profile
   * @return branches by code
   */
  @Transactional(readOnly = true)
  public List<InsurerBranch> branches(Long insurerId) {
    return branches.findByInsurerIdOrderByCodeAsc(insurerId);
  }

  /**
   * Adds a branch, pending authorization.
   *
   * @param insurerId profile
   * @param code branch code
   * @param details name, city, LGT rate, mailbox
   * @return branch
   */
  public InsurerBranch createBranch(Long insurerId, String code, BranchDetails details) {
    InsurerProfile insurer = get(insurerId);
    if (branches.findByInsurerIdAndCode(insurerId, code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.INSURER_BRANCH.label(), code);
    }
    InsurerBranch saved = branches.save(new InsurerBranch(insurerId, code, details));
    audit.record(
        CatalogKind.INSURER_BRANCH.label(),
        insurer.getPartyCode() + "/" + code,
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a branch (e.g. its LGT rate), pending authorization.
   *
   * @param branchId branch
   * @param details name, city, LGT rate, mailbox
   * @return branch
   */
  public InsurerBranch updateBranch(Long branchId, BranchDetails details) {
    InsurerBranch branch =
        branches
            .findById(branchId)
            .orElseThrow(
                () -> new ResourceNotFoundException(CatalogKind.INSURER_BRANCH.label(), branchId));
    branch.update(details);
    audit.record(
        CatalogKind.INSURER_BRANCH.label(),
        branch.catalogReference(),
        AuditAction.UPDATE,
        branch.catalogDescription());
    return branch;
  }

  /**
   * An active insurer that may receive placements.
   *
   * @param companyId company
   * @param partyCode insurer party code
   * @return insurer
   */
  @Transactional(readOnly = true)
  public InsurerProfile requireUsableInsurer(Long companyId, String partyCode) {
    InsurerProfile insurer = requireInsurer(companyId, partyCode);
    if (!insurer.isActive()) {
      throw new BusinessRuleException(
          "INSURER_NOT_ACTIVE", "Insurer " + partyCode + " is not authorized or is inactive");
    }
    return insurer;
  }

  /**
   * An active branch of an active insurer.
   *
   * @param companyId company
   * @param partyCode insurer party code
   * @param branchCode branch code
   * @return branch
   */
  @Transactional(readOnly = true)
  public InsurerBranch requireUsableBranch(Long companyId, String partyCode, String branchCode) {
    InsurerProfile insurer = requireUsableInsurer(companyId, partyCode);
    InsurerBranch branch =
        branches
            .findByInsurerIdAndCode(insurer.getId(), branchCode)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INSURER_BRANCH_UNKNOWN",
                        "Insurer " + partyCode + " has no branch " + branchCode));
    if (!branch.isActive()) {
      throw new BusinessRuleException(
          "INSURER_BRANCH_NOT_ACTIVE", "Branch " + branchCode + " is not authorized or inactive");
    }
    return branch;
  }

  /**
   * The active insurer panel of a company: authorized insurers with their authorized branches
   * (placement, quotation slips, cashiering).
   *
   * @param companyId company
   * @return insurers by name with their active branches
   */
  @Transactional(readOnly = true)
  public List<PanelInsurer> panel(Long companyId) {
    return insurers.findByCompanyIdOrderByNameAsc(companyId).stream()
        .filter(InsurerProfile::isActive)
        .map(
            i ->
                new PanelInsurer(
                    i,
                    branches.findByInsurerIdOrderByCodeAsc(i.getId()).stream()
                        .filter(InsurerBranch::isActive)
                        .toList()))
        .toList();
  }

  /**
   * An insurer of the panel with its active branches.
   *
   * @param insurer insurer profile
   * @param branches active branches
   */
  public record PanelInsurer(InsurerProfile insurer, List<InsurerBranch> branches) {}

  /**
   * Contact data of the insurer's party.
   *
   * @param taxId tax identification number
   * @param address address
   * @param email general e-mail
   * @param phone phone
   */
  public record PartyContact(String taxId, String address, String email, String phone) {}
}
