package com.iortatechnxt.brokerverse.brokerclaims.location.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRefRepository;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer location references (BRCLM.042; FR-CL-023): for each insured location of a cover and each
 * insurer, the reference the insurer uses, with effective dates. A new reference end-dates the open
 * one of the same location and insurer the day before it starts; nothing is deleted and every
 * change is audited. Maintained on screen (BCL_LOCATION_REF_MAINTAIN) or by the bulk upload {@code
 * BCL_LOCATION_REF}. Where the references come from is open (CLQ18).
 */
@Service
@Transactional
public class LocationRefService {

  /** Audit entity type of a reference. */
  public static final String ENTITY = "BrokerClaimLocationRef";

  private final LocationRefRepository refs;
  private final CoverService covers;
  private final InsurerService insurers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param refs references
   * @param covers covers and their locations
   * @param insurers insurer master
   * @param audit audit trail
   * @param clock clock
   */
  public LocationRefService(
      LocationRefRepository refs,
      CoverService covers,
      InsurerService insurers,
      AuditTrailService audit,
      Clock clock) {
    this.refs = refs;
    this.covers = covers;
    this.insurers = insurers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the reference of a location and insurer from a date (R1, R2).
   *
   * @param companyId company
   * @param request cover, location, insurer, reference and effective date
   * @return the new reference
   */
  public LocationRef maintain(Long companyId, NewRef request) {
    Account account = covers.account(companyId, request.arn());
    RiskItem item = requireLocation(account, request.itemNo());
    if (request.insurerCode() == null || request.insurerCode().isBlank()) {
      throw new BusinessRuleException("BCL_INSURER_REQUIRED", "Select the insurer");
    }
    String insurer =
        insurers.requireInsurer(companyId, request.insurerCode().strip()).getPartyCode();
    LocalDate from =
        request.effectiveFrom() == null ? LocalDate.now(clock) : request.effectiveFrom();
    LocationRef created =
        new LocationRef(
            companyId,
            new LocationRef.Location(
                account.getId(), account.getArn(), item.getItemNo(), item.getLocationKey()),
            insurer,
            request.reference(),
            from);
    refs.findByCompanyIdAndArnAndAccountItemNoAndInsurerCodeAndEffectiveToIsNull(
            companyId, account.getArn(), item.getItemNo(), insurer)
        .ifPresent(
            open -> {
              open.supersede(from);
              refs.saveAndFlush(open);
              audit.record(
                  ENTITY,
                  open.getId(),
                  AuditAction.UPDATE,
                  key(open)
                      + " "
                      + open.getInsurerLocationRef()
                      + " ended "
                      + open.getEffectiveTo());
            });
    LocationRef saved = refs.save(created);
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        key(saved) + " " + saved.getInsurerLocationRef() + " from " + saved.getEffectiveFrom());
    return saved;
  }

  /**
   * Every reference of a cover, current and past.
   *
   * @param companyId company
   * @param arn account reference number
   * @return references by location, insurer and start (newest first)
   */
  @Transactional(readOnly = true)
  public List<LocationRef> ofCover(Long companyId, String arn) {
    return refs.findByCompanyIdAndArnOrderByAccountItemNoAscInsurerCodeAscEffectiveFromDesc(
        companyId, arn);
  }

  /**
   * The references of a cover valid on a date (shown on every claim location, FR-CL-023).
   *
   * @param companyId company
   * @param arn account reference number
   * @param date date
   * @return valid references
   */
  @Transactional(readOnly = true)
  public List<LocationRef> validOn(Long companyId, String arn, LocalDate date) {
    return ofCover(companyId, arn).stream().filter(r -> r.validOn(date)).toList();
  }

  /**
   * References by ARN, insurer, reference or location key.
   *
   * @param companyId company
   * @param text text, may be empty
   * @param pageable page
   * @return references
   */
  @Transactional(readOnly = true)
  public Page<LocationRef> search(Long companyId, String text, Pageable pageable) {
    String term = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    return refs.search(companyId, "%" + term + "%", pageable);
  }

  /**
   * A location item of a cover.
   *
   * @param account cover
   * @param itemNo item number
   * @return the item
   */
  static RiskItem requireLocation(Account account, int itemNo) {
    return CoverService.locations(account).stream()
        .filter(i -> i.getItemNo() == itemNo)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "BCL_LOCATION_NOT_ON_COVER",
                    "Location " + itemNo + " is not on the cover " + account.getArn()));
  }

  private static String key(LocationRef r) {
    return r.getArn() + " item " + r.getAccountItemNo() + " " + r.getInsurerCode();
  }

  /**
   * A reference to record.
   *
   * @param arn cover
   * @param itemNo location item number
   * @param insurerCode insurer
   * @param reference insurer's location reference
   * @param effectiveFrom first day of validity, today when null
   */
  public record NewRef(
      String arn, int itemNo, String insurerCode, String reference, LocalDate effectiveFrom) {}
}
