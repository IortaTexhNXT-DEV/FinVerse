package com.iortatechnxt.brokerverse.screening.risk.service;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskProfile;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.RiskProfileChange;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntry;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntryRepository;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskSource;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual update of the risk-profile tagging with justification and evidence (SNSRP-304; FR-SS-035):
 * the change takes effect at once in the client master through {@code
 * crm.service.ClientRiskService} (source MANUAL) and is written to the risk-profile history with
 * the justification, the evidence and the case.
 */
@Service
@Transactional
public class RiskOverrideService {

  /** Longest justification (FR-SS-035). */
  public static final int MAX_JUSTIFICATION = 2000;

  private static final String RATING_LIST = "KYC_RISK_RATING";
  private static final String TAG_LIST = "CLIENT_TAG";

  private final ClientService clients;
  private final ClientRiskService clientRisk;
  private final AttachmentService attachments;
  private final RiskProfileEntryRepository history;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients client master (read)
   * @param clientRisk client master risk service
   * @param attachments evidence documents
   * @param history risk-profile history
   * @param lovs lists of values
   * @param clock clock
   */
  public RiskOverrideService(
      ClientService clients,
      ClientRiskService clientRisk,
      AttachmentService attachments,
      RiskProfileEntryRepository history,
      LovService lovs,
      Clock clock) {
    this.clients = clients;
    this.clientRisk = clientRisk;
    this.attachments = attachments;
    this.history = history;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Applies a manual change.
   *
   * @param clientId the client
   * @param change rating, tags, justification, evidence, match and case
   * @return the history row
   */
  public RiskProfileEntry override(Long clientId, ManualRiskChange change) {
    validate(change);
    change.evidenceAttachmentIds().forEach(attachments::get);
    Client client = clients.requireUsable(clientId);
    ClientRiskProfile profile =
        clientRisk.applyRiskProfile(
            clientId,
            new RiskProfileChange(
                change.riskRating(),
                change.addTags(),
                change.removeTags(),
                ClientRiskService.SOURCE_MANUAL,
                change.justification().trim(),
                change.reference()));
    String evidence =
        change.evidenceAttachmentIds().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    return history.save(
        new RiskProfileEntry(
            new RiskProfileEntry.ClientChange(
                client.getCompanyId(),
                clientId,
                client.getCode(),
                profile.previousRating(),
                profile.riskRating(),
                profile.tagsAdded(),
                profile.tagsRemoved(),
                profile.activeTags(),
                profile.kycReviewDue()),
            new RiskProfileEntry.Cause(
                RiskSource.MANUAL,
                null,
                null,
                null,
                change.matchId(),
                null,
                change.justification().trim(),
                evidence,
                change.caseId()),
            clock.instant()));
  }

  /**
   * The risk-profile history of a client, newest first.
   *
   * @param clientId the client
   * @return history rows
   */
  @Transactional(readOnly = true)
  public List<RiskProfileEntry> history(Long clientId) {
    return history.findByClientIdOrderByEffectiveAtDescIdDesc(clientId);
  }

  private void validate(ManualRiskChange change) {
    validateText(change);
    if (change.riskRating() == null || !codes(RATING_LIST).contains(change.riskRating())) {
      throw new BusinessRuleException("SCR_RISK_RATING_INVALID", "Select a valid risk rating");
    }
    Set<String> tags = codes(TAG_LIST);
    if (!Stream.concat(change.addTags().stream(), change.removeTags().stream())
        .allMatch(tags::contains)) {
      throw new BusinessRuleException("SCR_RISK_TAG_INVALID", "Select valid client tags");
    }
  }

  private static void validateText(ManualRiskChange change) {
    if (change.justification() == null || change.justification().isBlank()) {
      throw new BusinessRuleException(
          "SCR_JUSTIFICATION_REQUIRED", "Enter the justification of the change");
    }
    if (change.justification().length() > MAX_JUSTIFICATION) {
      throw new BusinessRuleException(
          "SCR_JUSTIFICATION_TOO_LONG", "The justification is limited to 2000 characters");
    }
    if (change.evidenceAttachmentIds().isEmpty()) {
      throw new BusinessRuleException(
          "SCR_EVIDENCE_REQUIRED", "Attach at least one evidence document");
    }
  }

  private Set<String> codes(String list) {
    return lovs.activeValues(list, LocalDate.now(clock)).stream()
        .map(LovValue::getCode)
        .collect(Collectors.toSet());
  }
}
