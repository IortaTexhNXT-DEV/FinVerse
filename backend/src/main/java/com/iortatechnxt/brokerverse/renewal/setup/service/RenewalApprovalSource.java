package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.ApprovalStatus;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSetRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSettingRepository;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrixRepository;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCodeRepository;
import com.iortatechnxt.brokerverse.renewal.domain.PackageChoiceRepository;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RaSentRequestRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import com.iortatechnxt.brokerverse.renewal.domain.VersionedRules;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of Renewal: Setup records and rule versions for the holders of {@code
 * RNW_SETUP}, package map entries and package choices for the Renewal processing team ({@code
 * RNW_PACKAGE_REMAP}), and corrections of the Renewal Advices already sent ({@code RNW_RA_SEND});
 * never to their maker.
 */
@Component
@Transactional(readOnly = true)
public class RenewalApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "RENEWAL";
  private static final String SETUP_LINK = "/renewal/setup";

  private final NonRenewableRiskCodeRepository riskCodes;
  private final CheckSettingRepository settings;
  private final BucketRuleSetRepository ruleSets;
  private final DecisionMatrixRepository matrices;
  private final PackageMapEntryRepository map;
  private final PackageChoiceRepository choices;
  private final RaSentRequestRepository raSent;
  private final RenewalCandidateRepository candidates;

  /**
   * Creates the source.
   *
   * @param riskCodes non-renewable risk codes
   * @param settings check settings
   * @param ruleSets bucket rule sets
   * @param matrices decision matrices
   * @param map package map
   * @param choices package choices
   * @param raSent corrections of the RAs already sent
   * @param candidates renewals
   */
  public RenewalApprovalSource(
      NonRenewableRiskCodeRepository riskCodes,
      CheckSettingRepository settings,
      BucketRuleSetRepository ruleSets,
      DecisionMatrixRepository matrices,
      PackageMapEntryRepository map,
      PackageChoiceRepository choices,
      RaSentRequestRepository raSent,
      RenewalCandidateRepository candidates) {
    this.riskCodes = riskCodes;
    this.settings = settings;
    this.ruleSets = ruleSets;
    this.matrices = matrices;
    this.map = map;
    this.choices = choices;
    this.raSent = raSent;
    this.candidates = candidates;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    if (viewer.can(RenewalCodes.SETUP)) {
      setup(viewer, items);
    }
    if (viewer.can(RenewalCodes.PACKAGE_REMAP)) {
      packages(viewer, items);
    }
    if (viewer.can(RenewalCodes.RA_SEND)) {
      raSent(viewer, items);
    }
    return items;
  }

  private void setup(ApprovalViewer viewer, List<PendingApproval> items) {
    riskCodes.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(c -> viewer.mayApproveItemOf(c.getMaker()))
        .map(
            c ->
                item(
                    "Non-renewable risk code",
                    c.getRiskCode(),
                    c.getReason(),
                    c.getMaker(),
                    c.getUpdatedAt() == null ? c.getCreatedAt() : c.getUpdatedAt(),
                    c.getCompanyId(),
                    SETUP_LINK + "?tab=risk-codes"))
        .forEach(items::add);
    settings.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(s -> viewer.mayApproveItemOf(s.getMaker()))
        .map(
            s ->
                item(
                    "Renewal check setting",
                    CheckNames.of(s.getCheckCode()),
                    s.getSeverity().label() + (s.isEnabled() ? "" : ", inactive"),
                    s.getMaker(),
                    s.getUpdatedAt() == null ? s.getCreatedAt() : s.getUpdatedAt(),
                    null,
                    SETUP_LINK + "?tab=checks"))
        .forEach(items::add);
    ruleSets.findByStatus(RuleSetStatus.SUBMITTED).stream()
        .filter(v -> viewer.mayApproveItemOf(v.getSubmittedBy()))
        .map(v -> version("Bucket rules", v, "?tab=buckets"))
        .forEach(items::add);
    matrices.findByStatus(RuleSetStatus.SUBMITTED).stream()
        .filter(v -> viewer.mayApproveItemOf(v.getSubmittedBy()))
        .map(v -> version("Decision matrix", v, "?tab=matrix"))
        .forEach(items::add);
  }

  private void packages(ApprovalViewer viewer, List<PendingApproval> items) {
    map.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(e -> viewer.mayApproveItemOf(e.getMaker()))
        .map(
            e ->
                item(
                    "Package map entry",
                    e.getLegacyPackageCode(),
                    e.isReject()
                        ? "No BIBS package"
                        : "Renews on " + e.getProductCode() + " v" + e.getProductVersionNo(),
                    e.getMaker(),
                    e.getUpdatedAt() == null ? e.getCreatedAt() : e.getUpdatedAt(),
                    e.getCompanyId(),
                    SETUP_LINK + "?tab=packages"))
        .forEach(items::add);
    choices.findByStatusOrderByIdAsc(ApprovalStatus.PENDING).stream()
        .filter(p -> viewer.mayApproveItemOf(p.getCreatedBy()))
        .forEach(
            p ->
                candidates
                    .findById(p.getCandidateId())
                    .ifPresent(
                        c ->
                            items.add(
                                item(
                                    "Renewal package choice",
                                    c.getRenewalRef(),
                                    c.getSnapshot().clientName()
                                        + ": "
                                        + p.getProductCode()
                                        + " v"
                                        + p.getProductVersionNo(),
                                    p.getCreatedBy(),
                                    p.getCreatedAt(),
                                    c.getCompanyId(),
                                    RenewalCodes.LINK + c.getRenewalRef()))));
  }

  private void raSent(ApprovalViewer viewer, List<PendingApproval> items) {
    raSent.findByStatusOrderByIdAsc(ApprovalStatus.PENDING).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getCreatedBy()))
        .map(
            r ->
                item(
                    "Renewal Advice already sent",
                    r.getLegacyRef(),
                    r.getCorrection(),
                    r.getCreatedBy(),
                    r.getCreatedAt(),
                    r.getCompanyId(),
                    "/renewal/processing?tab=ra-sent"))
        .forEach(items::add);
  }

  private static PendingApproval version(String type, VersionedRules v, String tab) {
    return item(
        type,
        "Version " + v.getVersionNo(),
        v.getDescription() == null ? "Effective " + v.getEffectiveFrom() : v.getDescription(),
        v.getSubmittedBy(),
        v.getSubmittedAt(),
        v.getCompanyId(),
        SETUP_LINK + tab);
  }

  private static PendingApproval item(
      String type,
      String reference,
      String description,
      String maker,
      java.time.Instant at,
      Long companyId,
      String link) {
    return new PendingApproval(
        MODULE, type, reference, description, null, null, maker, at, companyId, link);
  }
}
