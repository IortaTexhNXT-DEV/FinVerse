package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientTag;
import com.iortatechnxt.brokerverse.crm.domain.ClientTagRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client risk profile set by sanction screening and risk profiling (SNSRP-302, 304;
 * SANCTION_SCREENING_DESIGN section 9): the KYC risk rating and the tags {@code PEP} / {@code
 * WATCHLIST_REVIEW}. Screening never writes crm tables; it calls this service.
 *
 * <p>The rating is validated against {@code KYC_RISK_RATING} and set without touching the other
 * profile fields; when it becomes {@code HIGH} the next periodic KYC review is brought forward by
 * {@link KycReviewPolicy} (BRNB.110). Tags are added or ended through {@link ClientNotesService},
 * so their history and banner follow as for a manual tag; adding an active tag or ending an absent
 * one is ignored (idempotent). The change is audited with its source and reference.
 */
@Service
@Transactional
public class ClientRiskService {

  /** Source of a change decided by the risk rules. */
  public static final String SOURCE_RULE = "RULE";

  /** Source of a change decided by an investigator (justification mandatory, SNSRP-304). */
  public static final String SOURCE_MANUAL = "MANUAL";

  private static final String RATING_LIST = "KYC_RISK_RATING";

  private final ClientService clients;
  private final ClientNotesService notes;
  private final ClientTagRepository tags;
  private final KycReviewPolicy reviewPolicy;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param notes tags and their history
   * @param tags tag reads
   * @param reviewPolicy periodic KYC review cycle
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public ClientRiskService(
      ClientService clients,
      ClientNotesService notes,
      ClientTagRepository tags,
      KycReviewPolicy reviewPolicy,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.clients = clients;
    this.notes = notes;
    this.tags = tags;
    this.reviewPolicy = reviewPolicy;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Applies a risk profile change to a client.
   *
   * @param clientId client (prospect or confirmed, not inactive)
   * @param change rating, tags, source, reason and reference
   * @return the profile after the change
   */
  public ClientRiskProfile applyRiskProfile(Long clientId, RiskProfileChange change) {
    validate(change);
    Client client = clients.requireUsable(clientId);
    String previous = client.getRiskRating();
    String rating = change.riskRating() == null ? previous : change.riskRating();
    if (!Objects.equals(previous, rating)) {
      client.applyRiskRating(rating, reviewDate(client, previous, rating));
    }
    Set<String> active = activeTags(clientId);
    Set<String> added = new TreeSet<>(change.addTags());
    added.removeAll(active);
    Set<String> removed = new TreeSet<>(change.removeTags());
    removed.retainAll(active);
    added.forEach(tag -> notes.addTag(clientId, tag));
    removed.forEach(tag -> notes.removeTag(clientId, tag));
    ClientRiskProfile profile =
        new ClientRiskProfile(
            clientId,
            client.getCode(),
            previous,
            rating,
            activeTags(clientId),
            added,
            removed,
            client.getKycReviewDue());
    if (profile.changed()) {
      audit.record(
          ClientService.ENTITY,
          client.getProspectCode(),
          AuditAction.UPDATE,
          describe(profile, change));
    }
    return profile;
  }

  /**
   * The active tags of a client.
   *
   * @param clientId client
   * @return tag codes, sorted
   */
  @Transactional(readOnly = true)
  public Set<String> activeTags(Long clientId) {
    return tags.findByClientIdAndActiveTrueOrderByTagCode(clientId).stream()
        .map(ClientTag::getTagCode)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private void validate(RiskProfileChange change) {
    if (!SOURCE_RULE.equals(change.source()) && !SOURCE_MANUAL.equals(change.source())) {
      throw new BusinessRuleException(
          "RISK_SOURCE_INVALID", "The source of a risk profile change is RULE or MANUAL");
    }
    if (SOURCE_MANUAL.equals(change.source())
        && (change.reason() == null || change.reason().isBlank())) {
      throw new BusinessRuleException(
          "RISK_JUSTIFICATION_REQUIRED", "Enter the justification of the manual risk change");
    }
    if (change.riskRating() != null) {
      lovs.requireValid(RATING_LIST, change.riskRating(), LocalDate.now(clock));
    }
  }

  private LocalDate reviewDate(Client client, String previous, String rating) {
    if (!KycReviewPolicy.HIGH_RISK.equals(rating)
        || KycReviewPolicy.HIGH_RISK.equals(previous)
        || client.getKycReviewDue() == null) {
      return null;
    }
    LocalDate verifiedOn =
        client.getKycVerifiedAt() == null
            ? LocalDate.now(clock)
            : LocalDate.ofInstant(client.getKycVerifiedAt(), clock.getZone());
    return reviewPolicy.nextReview(rating, verifiedOn);
  }

  private static String describe(ClientRiskProfile p, RiskProfileChange change) {
    StringBuilder sb =
        new StringBuilder("Risk rating ")
            .append(p.previousRating())
            .append(" -> ")
            .append(p.riskRating());
    if (!p.tagsAdded().isEmpty()) {
      sb.append("; tags added ").append(new TreeSet<>(p.tagsAdded()));
    }
    if (!p.tagsRemoved().isEmpty()) {
      sb.append("; tags ended ").append(new TreeSet<>(p.tagsRemoved()));
    }
    sb.append(" (").append(change.source());
    if (change.reference() != null) {
      sb.append(", ").append(change.reference());
    }
    if (change.reason() != null && !change.reason().isBlank()) {
      sb.append(": ").append(change.reason());
    }
    return sb.append(')').toString();
  }
}
