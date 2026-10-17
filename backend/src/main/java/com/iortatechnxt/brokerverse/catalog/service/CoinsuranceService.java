package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation.ParticipationDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipationRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default co-insurance participation of an insurer (BRD-00 CRQ08, Risk Participation; G08 insurer
 * page): the participating insurers with their shares, the lead and the effective dates, under
 * maker-checker with the change log (G05). Shares of the same period cannot exceed 100 % and only
 * one participant leads; an arrangement is used as the default co-insurance split of an invoice
 * (FR-OP-007) only when its authorized shares on the booking date total exactly 100 % and include
 * the insurer itself.
 */
@Service
@Transactional
public class CoinsuranceService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String INVALID = "COINSURANCE_SHARE_INVALID";

  private final InsurerService insurers;
  private final InsurerParticipationRepository participations;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param insurers insurer profiles
   * @param participations participations
   * @param audit audit trail
   */
  public CoinsuranceService(
      InsurerService insurers,
      InsurerParticipationRepository participations,
      AuditTrailService audit) {
    this.insurers = insurers;
    this.participations = participations;
    this.audit = audit;
  }

  /**
   * Participations of an insurer's arrangement, latest period first.
   *
   * @param insurerId insurer profile
   * @return participations
   */
  @Transactional(readOnly = true)
  public List<InsurerParticipation> participations(Long insurerId) {
    return participations.findByInsurerIdOrderByEffectiveFromDescParticipantCodeAsc(insurerId);
  }

  /**
   * Adds a participating insurer, pending authorization.
   *
   * @param insurerId insurer profile of the arrangement
   * @param details participation
   * @return participation
   */
  public InsurerParticipation add(Long insurerId, ParticipationDetails details) {
    InsurerProfile insurer = insurers.get(insurerId);
    validate(insurer, details, null);
    InsurerParticipation saved =
        participations.save(new InsurerParticipation(insurer.getId(), details));
    audit.record(
        CatalogKind.INSURER_PARTICIPATION.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a participation; it is pending authorization again.
   *
   * @param id participation
   * @param details participation
   * @return participation
   */
  public InsurerParticipation update(Long id, ParticipationDetails details) {
    InsurerParticipation p =
        participations
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException(CatalogKind.INSURER_PARTICIPATION.label(), id));
    validate(insurers.get(p.getInsurerId()), details, id);
    p.update(details);
    audit.record(
        CatalogKind.INSURER_PARTICIPATION.label(),
        p.catalogReference(),
        AuditAction.UPDATE,
        p.catalogDescription());
    return p;
  }

  private void validate(InsurerProfile insurer, ParticipationDetails d, Long self) {
    validateValues(d);
    insurers.requireInsurer(insurer.getCompanyId(), d.participantCode());
    BigDecimal total = d.sharePct();
    for (InsurerParticipation other : participations(insurer.getId())) {
      if (sharesPeriod(other, d, self)) {
        requireCompatible(other, d);
        total = total.add(other.getSharePct());
      }
    }
    if (total.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException(
          INVALID,
          "The shares of the period would total "
              + total.stripTrailingZeros().toPlainString()
              + "%; they must total 100%");
    }
  }

  private static void validateValues(ParticipationDetails d) {
    boolean shareValid =
        d.sharePct() != null && d.sharePct().signum() > 0 && d.sharePct().compareTo(HUNDRED) <= 0;
    if (!shareValid) {
      throw new BusinessRuleException(INVALID, "The share must be above 0% and at most 100%");
    }
    boolean datesValid =
        d.effectiveFrom() != null
            && (d.effectiveTo() == null || !d.effectiveTo().isBefore(d.effectiveFrom()));
    if (!datesValid) {
      throw new BusinessRuleException(INVALID, "The participation cannot end before it starts");
    }
  }

  private static boolean sharesPeriod(
      InsurerParticipation other, ParticipationDetails d, Long self) {
    return !Objects.equals(other.getId(), self)
        && other.getRecordStatus() != RecordStatus.INACTIVE
        && other.overlaps(d.effectiveFrom(), d.effectiveTo());
  }

  private static void requireCompatible(InsurerParticipation other, ParticipationDetails d) {
    if (other.getParticipantCode().equals(d.participantCode())) {
      throw new BusinessRuleException(
          INVALID, d.participantCode() + " already participates in this period");
    }
    if (d.lead() && other.isLead()) {
      throw new BusinessRuleException(
          INVALID, other.getParticipantCode() + " already leads this period");
    }
  }

  /**
   * Total of the authorized shares effective on a date.
   *
   * @param insurerId insurer profile
   * @param on date
   * @return total in percent
   */
  @Transactional(readOnly = true)
  public BigDecimal totalOn(Long insurerId, LocalDate on) {
    return effective(participations(insurerId), on).stream()
        .map(InsurerParticipation::getSharePct)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * The default co-insurance split of an insurer on a date (FR-OP-007).
   *
   * @param companyId company
   * @param insurerCode insurer of the account
   * @param on booking date
   * @return shares, lead first, when the authorized shares total 100 % and include the insurer;
   *     empty otherwise
   */
  @Transactional(readOnly = true)
  public Optional<List<DefaultShare>> defaultShares(
      Long companyId, String insurerCode, LocalDate on) {
    InsurerProfile insurer;
    try {
      insurer = insurers.requireInsurer(companyId, insurerCode);
    } catch (ResourceNotFoundException ex) {
      return Optional.empty();
    }
    List<InsurerParticipation> active = effective(participations(insurer.getId()), on);
    BigDecimal total =
        active.stream()
            .map(InsurerParticipation::getSharePct)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean includesInsurer =
        active.stream().anyMatch(p -> p.getParticipantCode().equals(insurerCode));
    if (total.compareTo(HUNDRED) != 0 || !includesInsurer) {
      return Optional.empty();
    }
    List<InsurerParticipation> ordered = new ArrayList<>(active);
    ordered.sort(
        Comparator.comparing((InsurerParticipation p) -> !p.isLead())
            .thenComparing(InsurerParticipation::getParticipantCode));
    return Optional.of(
        ordered.stream()
            .map(p -> new DefaultShare(p.getParticipantCode(), p.getSharePct(), p.isLead()))
            .toList());
  }

  private static List<InsurerParticipation> effective(
      List<InsurerParticipation> all, LocalDate on) {
    return all.stream()
        .filter(InsurerParticipation::isActive)
        .filter(p -> p.effectiveOn(on))
        .toList();
  }

  /**
   * A share of the default split.
   *
   * @param insurerCode participating insurer
   * @param sharePct share in percent
   * @param lead whether it leads
   */
  public record DefaultShare(String insurerCode, BigDecimal sharePct, boolean lead) {}
}
