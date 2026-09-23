package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import com.iortatechnxt.finverse.dimension.service.DimensionService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.LayerRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.ParticipantRequest;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant.ParticipantTerms;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Treaty master maintenance under maker-checker control, and the treaty programme lookup used by
 * the allocation and the claims listener.
 *
 * <p>Rules: participants are active reinsurers whose shares total 100 %; the broker is an active
 * reinsurance broker; the statement currency is the company base currency (reinsurance accounting
 * is kept in base currency); only one authorized treaty of each type per line of business and
 * underwriting year.
 */
@Service
@Transactional
public class TreatyService {

  static final String ENTITY = "Treaty";

  private static final Set<PartyType> REINSURERS = EnumSet.of(PartyType.REINSURER);
  private static final Set<PartyType> BROKERS = EnumSet.of(PartyType.RI_BROKER);

  private final TreatyRepository treaties;
  private final PartyService parties;
  private final DimensionService dimensions;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param treaties repository
   * @param parties reinsurers and brokers
   * @param dimensions line of business validation
   * @param organization company base currency
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TreatyService(
      TreatyRepository treaties,
      PartyService parties,
      DimensionService dimensions,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.treaties = treaties;
    this.parties = parties;
    this.dimensions = dimensions;
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists a company's treaties.
   *
   * @param companyId company
   * @return treaties, latest underwriting year first
   */
  @Transactional(readOnly = true)
  public List<Treaty> list(Long companyId) {
    return treaties.findByCompanyIdOrderByUwYearDescBusinessLineAscCodeAsc(companyId);
  }

  /**
   * Gets a treaty.
   *
   * @param id id
   * @return treaty
   */
  @Transactional(readOnly = true)
  public Treaty get(Long id) {
    return treaties.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Gets a treaty by code.
   *
   * @param companyId company
   * @param code code
   * @return treaty
   */
  @Transactional(readOnly = true)
  public Treaty getByCode(Long companyId, String code) {
    return treaties
        .findByCompanyIdAndCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, code));
  }

  /**
   * The authorized programme of a line of business and underwriting year.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param uwYear underwriting year
   * @return programme (empty when no treaty is authorized)
   */
  @Transactional(readOnly = true)
  public TreatyProgramme programme(Long companyId, String businessLine, int uwYear) {
    return TreatyProgramme.of(
        treaties.findByCompanyIdAndBusinessLineAndUwYearAndRecordStatus(
            companyId, businessLine, uwYear, RecordStatus.ACTIVE));
  }

  /**
   * Creates a treaty (pending authorization).
   *
   * @param r request
   * @return treaty
   */
  public Treaty create(TreatyRequest r) {
    if (treaties.existsByCompanyIdAndCode(r.companyId(), r.code())) {
      throw new DuplicateResourceException(ENTITY, r.code());
    }
    validate(r);
    Treaty treaty = new Treaty(r.companyId(), r.code(), r.toTerms());
    treaty.define(r.toTerms(), broker(r), participants(r), layers(r.layers()));
    Treaty saved = treaties.save(treaty);
    audit.record(
        ENTITY, saved.getCode(), AuditAction.CREATE, "Created " + r.treatyType() + " " + r.name());
    return saved;
  }

  /**
   * Updates a treaty; it returns to pending authorization.
   *
   * @param id id
   * @param r request
   * @return treaty
   */
  public Treaty update(Long id, TreatyRequest r) {
    Treaty treaty = get(id);
    validate(r);
    treaty.define(r.toTerms(), broker(r), participants(r), layers(r.layers()));
    audit.record(ENTITY, treaty.getCode(), AuditAction.UPDATE, "Updated treaty");
    return treaty;
  }

  /**
   * Authorizes a treaty (checker).
   *
   * @param id id
   * @return treaty
   */
  public Treaty authorize(Long id) {
    Treaty treaty = get(id);
    boolean duplicate =
        treaties
            .findByCompanyIdAndBusinessLineAndUwYearAndRecordStatus(
                treaty.getCompanyId(),
                treaty.getBusinessLine(),
                treaty.getUwYear(),
                RecordStatus.ACTIVE)
            .stream()
            .anyMatch(t -> t.getTreatyType() == treaty.getTreatyType() && !t.getId().equals(id));
    if (duplicate) {
      throw new BusinessRuleException(
          "TREATY_PROGRAMME_DUPLICATE",
          "An authorized "
              + treaty.getTreatyType()
              + " treaty already exists for "
              + treaty.getBusinessLine()
              + " "
              + treaty.getUwYear());
    }
    treaty.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, treaty.getCode(), AuditAction.AUTHORIZE, "Authorized treaty");
    return treaty;
  }

  private void validate(TreatyRequest r) {
    dimensions.validateOptional(r.companyId(), DimensionType.BUSINESS_LINE, r.businessLine());
    String base = organization.getCompany(r.companyId()).getBaseCurrency();
    if (!base.equals(r.currency())) {
      throw new BusinessRuleException(
          "TREATY_CURRENCY", "Treaty statements are kept in the base currency " + base);
    }
  }

  private Party broker(TreatyRequest r) {
    return r.brokerCode() == null || r.brokerCode().isBlank()
        ? null
        : parties.requireActive(r.companyId(), r.brokerCode(), BROKERS);
  }

  private List<TreatyParticipant> participants(TreatyRequest r) {
    List<TreatyParticipant> out = new ArrayList<>();
    int line = 1;
    for (ParticipantRequest p : r.participants()) {
      Party party = parties.requireActive(r.companyId(), p.reinsurerCode(), REINSURERS);
      out.add(
          new TreatyParticipant(
              line++,
              party,
              new ParticipantTerms(
                  p.sharePct(),
                  p.commissionPct(),
                  p.profitCommissionPct(),
                  p.premiumReservePct())));
    }
    return out;
  }

  private static List<TreatyLayer> layers(List<LayerRequest> requests) {
    List<TreatyLayer> out = new ArrayList<>();
    int layerNo = 1;
    for (LayerRequest l : requests) {
      out.add(
          new TreatyLayer(
              layerNo++,
              l.priority(),
              l.limit(),
              l.minDepositPremium(),
              l.reinstatements() == null ? 0 : l.reinstatements()));
    }
    return out;
  }
}
