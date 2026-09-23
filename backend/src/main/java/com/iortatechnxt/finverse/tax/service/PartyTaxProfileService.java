package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.finverse.tax.domain.PartyTaxProfileRepository;
import com.iortatechnxt.finverse.tax.domain.PayeeClass;
import com.iortatechnxt.finverse.tax.domain.TaxCode;
import com.iortatechnxt.finverse.tax.domain.TaxCodeRepository;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.domain.Taxpayer;
import com.iortatechnxt.finverse.tax.domain.VatTreatment;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance of party tax profiles (maker-checker, audited): TIN, registered name, VAT treatment
 * and default ATC of suppliers, intermediaries and customers.
 */
@Service
@Transactional
public class PartyTaxProfileService {

  /** Audit entity name. */
  public static final String ENTITY = "PartyTaxProfile";

  private final PartyTaxProfileRepository profiles;
  private final TaxCodeRepository codes;
  private final PartyService parties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param profiles repository
   * @param codes tax codes (default ATC validation)
   * @param parties party master
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PartyTaxProfileService(
      PartyTaxProfileRepository profiles,
      TaxCodeRepository codes,
      PartyService parties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.profiles = profiles;
    this.codes = codes;
    this.parties = parties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the profiles of a company.
   *
   * @param companyId company
   * @return profiles ordered by party code
   */
  @Transactional(readOnly = true)
  public List<PartyTaxProfile> list(Long companyId) {
    return profiles.findByCompanyIdOrderByPartyCode(companyId);
  }

  /**
   * Gets a profile.
   *
   * @param id id
   * @return profile
   */
  @Transactional(readOnly = true)
  public PartyTaxProfile get(Long id) {
    return profiles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a profile (pending authorization).
   *
   * @param c values
   * @return profile
   */
  public PartyTaxProfile create(PartyTaxProfileCommand c) {
    Party party = parties.getByCode(c.companyId(), c.partyCode());
    if (profiles.existsByCompanyIdAndPartyId(c.companyId(), party.getId())) {
      throw new DuplicateResourceException(ENTITY, c.partyCode());
    }
    PartyTaxProfile profile = new PartyTaxProfile(c.companyId(), party.getId(), party.getCode());
    apply(profile, c);
    PartyTaxProfile saved = profiles.save(profile);
    audit.record(ENTITY, saved.getPartyCode(), AuditAction.CREATE, "Created tax profile");
    return saved;
  }

  /**
   * Updates a profile; it returns to pending authorization.
   *
   * @param id id
   * @param c values (party and company are immutable)
   * @return profile
   */
  public PartyTaxProfile update(Long id, PartyTaxProfileCommand c) {
    PartyTaxProfile profile = get(id);
    apply(profile, c);
    profile.markModified();
    audit.record(ENTITY, profile.getPartyCode(), AuditAction.UPDATE, "Updated tax profile");
    return profile;
  }

  /**
   * Authorizes a profile (checker).
   *
   * @param id id
   * @return profile
   */
  public PartyTaxProfile authorize(Long id) {
    PartyTaxProfile profile = get(id);
    profile.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, profile.getPartyCode(), AuditAction.AUTHORIZE, "Authorized tax profile");
    return profile;
  }

  private void apply(PartyTaxProfile profile, PartyTaxProfileCommand c) {
    Taxpayer tin = Taxpayer.parse(c.tin(), c.registeredName(), null, null);
    if ("000000000".equals(tin.tin())) {
      throw new BusinessRuleException("INVALID_TIN", "A TIN has 9 digits (plus a branch code)");
    }
    String branch = TaxMasterSupport.blankToNull(c.branchCode());
    String atc = TaxMasterSupport.blankToNull(c.defaultAtcCode());
    if (atc != null) {
      requireAtc(c.companyId(), atc, c.payeeClass());
    }
    profile.setTin(tin.tin());
    profile.setBranchCode(branch == null ? tin.branchCode() : branch);
    profile.setPayeeClass(c.payeeClass());
    profile.setRegisteredName(c.registeredName());
    applyNames(profile, c);
    profile.setRegisteredAddress(TaxMasterSupport.blankToNull(c.registeredAddress()));
    profile.setZipCode(TaxMasterSupport.blankToNull(c.zipCode()));
    profile.setVatTreatment(c.vatTreatment() == null ? VatTreatment.REGULAR : c.vatTreatment());
    profile.setDefaultAtcCode(atc);
  }

  /** Individuals are listed by last, first and middle name; corporations by registered name. */
  private static void applyNames(PartyTaxProfile profile, PartyTaxProfileCommand c) {
    if (c.payeeClass() != PayeeClass.INDIVIDUAL) {
      profile.setLastName(null);
      profile.setFirstName(null);
      profile.setMiddleName(null);
      return;
    }
    if (isBlank(c.lastName()) || isBlank(c.firstName())) {
      throw new BusinessRuleException(
          "INDIVIDUAL_NAME_REQUIRED", "Individuals need last and first name for the alphalists");
    }
    profile.setLastName(c.lastName().trim());
    profile.setFirstName(c.firstName().trim());
    profile.setMiddleName(TaxMasterSupport.blankToNull(c.middleName()));
  }

  private void requireAtc(Long companyId, String code, PayeeClass payeeClass) {
    TaxCode taxCode =
        codes
            .findByCompanyIdAndCode(companyId, code)
            .orElseThrow(() -> new ResourceNotFoundException(TaxCodeService.ENTITY, code));
    if (taxCode.getTaxType() != TaxType.EWT) {
      throw new BusinessRuleException(
          "NOT_A_WITHHOLDING_CODE", "Default ATC " + code + " is not a withholding tax code");
    }
    if (taxCode.getPayeeClass() != null && taxCode.getPayeeClass() != payeeClass) {
      throw new BusinessRuleException(
          "ATC_PAYEE_MISMATCH",
          "ATC " + code + " applies to " + taxCode.getPayeeClass() + " payees only");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
