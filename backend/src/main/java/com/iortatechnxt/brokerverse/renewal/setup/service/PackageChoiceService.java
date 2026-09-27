package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.ApprovalStatus;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.PackageChoice;
import com.iortatechnxt.brokerverse.renewal.domain.PackageChoiceRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package choices of the Renewal processing team (DMQ36): when a migrated policy is in the
 * Exception bucket because its legacy package has no single released BIBS package, a member of the
 * team chooses the version (maker) and another member approves it (checker). The approved version
 * is set on the renewal and its checks run again.
 */
@Service
@Transactional
public class PackageChoiceService {

  private final PackageChoiceRepository choices;
  private final RenewalRecords records;
  private final PackageMapService map;
  private final ReevaluationService reevaluation;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param choices package choices
   * @param records renewals
   * @param map package map (target validation)
   * @param reevaluation checks
   * @param notices notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PackageChoiceService(
      PackageChoiceRepository choices,
      RenewalRecords records,
      PackageMapService map,
      ReevaluationService reevaluation,
      RenewalNotices notices,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.choices = choices;
    this.records = records;
    this.map = map;
    this.reevaluation = reevaluation;
    this.notices = notices;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Chooses the package version of a migrated renewal (maker).
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param productCode package (risk code)
   * @param versionNo version
   * @param reason reason
   * @return the choice, pending
   */
  public PackageChoice propose(
      Long companyId, String renewalRef, String productCode, Integer versionNo, String reason) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    if (c.getSource() != CandidateSource.LEGACY || c.getSnapshot().legacyPackageCode() == null) {
      throw new BusinessRuleException(
          "RNW_PACKAGE_NOT_MIGRATED", "Only a migrated packaged policy needs a package choice");
    }
    if (!c.getStage().isOpen()) {
      throw new BusinessRuleException("RNW_PACKAGE_CLOSED", "The renewal is closed");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("RNW_PACKAGE_REASON", "Enter the reason of the choice");
    }
    if (choices.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .anyMatch(p -> p.getStatus() == ApprovalStatus.PENDING)) {
      throw new BusinessRuleException(
          "RNW_PACKAGE_PENDING", "A package choice is already waiting for approval");
    }
    List<String> problems = map.targetProblems(productCode, versionNo);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_PACKAGE_TARGET", problems.get(0));
    }
    PackageChoice choice =
        choices.save(new PackageChoice(c, productCode.strip(), versionNo, reason.strip()));
    audit.record(
        RenewalCodes.ENTITY,
        c.getId(),
        AuditAction.SUBMIT,
        "Package " + productCode + " version " + versionNo + " chosen for " + c.getRenewalRef());
    return choice;
  }

  /**
   * Decides a choice (checker, another member of the team).
   *
   * @param id choice
   * @param approve approve or reject
   * @param remarks remarks (required to reject)
   * @return the choice
   */
  public PackageChoice decide(Long id, boolean approve, String remarks) {
    PackageChoice choice =
        choices.findById(id).orElseThrow(() -> new ResourceNotFoundException("Package choice", id));
    if (!approve && (remarks == null || remarks.isBlank())) {
      throw new BusinessRuleException("RNW_PACKAGE_REJECT_REMARKS", "Enter the reason");
    }
    RenewalCandidate c = records.byId(choice.getCandidateId());
    records.requireScope(c);
    choice.decide(currentUser.username(), approve, remarks, clock.instant());
    String text = "Package " + choice.getProductCode() + " version " + choice.getProductVersionNo();
    if (approve) {
      c.resolvePackage(choice.getProductCode(), choice.getProductVersionNo());
      reevaluation.reevaluate(c, CheckTrigger.MANUAL);
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getId(),
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        text + (approve ? " approved" : " rejected") + " for " + c.getRenewalRef());
    notices.users(
        List.of(choice.getCreatedBy()),
        RenewalCodes.EVENT_PACKAGE_DECIDED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + ": package choice " + (approve ? "approved" : "rejected"),
            text + (approve ? " applies to the renewal" : " was rejected: " + remarks)));
    return choice;
  }

  /**
   * Choices of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return choices
   */
  @Transactional(readOnly = true)
  public List<PackageChoice> of(Long companyId, String renewalRef) {
    return choices.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  /**
   * Choices waiting for approval.
   *
   * @return pending choices
   */
  @Transactional(readOnly = true)
  public List<Pending> pending() {
    return choices.findByStatusOrderByIdAsc(ApprovalStatus.PENDING).stream()
        .map(
            p -> {
              RenewalCandidate c = records.byId(p.getCandidateId());
              return new Pending(p, c.getRenewalRef(), c.getSnapshot().clientName());
            })
        .toList();
  }

  /**
   * A choice waiting for approval with its renewal.
   *
   * @param choice choice
   * @param renewalRef renewal reference
   * @param clientName client
   */
  public record Pending(PackageChoice choice, String renewalRef, String clientName) {}
}
