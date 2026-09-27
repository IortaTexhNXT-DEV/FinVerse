package com.iortatechnxt.brokerverse.eb.setup.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRuleRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of the EB Setup: threshold rules and required documents waiting for
 * authorisation, for the holders of {@code EB_SETUP} other than their maker.
 */
@Component
@Transactional(readOnly = true)
public class SetupApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "EMPLOYEE_BENEFITS";
  private static final String LINK = "/eb/setup";

  private final EbThresholdRuleRepository rules;
  private final EbRequiredDocumentRepository required;
  private final LovService lovs;

  /**
   * Creates the source.
   *
   * @param rules threshold rules
   * @param required required documents
   * @param lovs labels
   */
  public SetupApprovalSource(
      EbThresholdRuleRepository rules, EbRequiredDocumentRepository required, LovService lovs) {
    this.rules = rules;
    this.required = required;
    this.lovs = lovs;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(EbCodes.PERMISSION_SETUP)) {
      return List.of();
    }
    List<PendingApproval> items = new ArrayList<>();
    rules.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getMaker()))
        .map(SetupApprovalSource::rule)
        .forEach(items::add);
    required.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(d -> viewer.mayApproveItemOf(d.getMaker()))
        .map(this::requirement)
        .forEach(items::add);
    return items;
  }

  private static PendingApproval rule(EbThresholdRule r) {
    return new PendingApproval(
        MODULE,
        "EB threshold rule",
        "TR-" + r.getId(),
        (r.getBenefitLine() == null ? "All lines" : r.getBenefitLine())
            + " "
            + r.getMeasure().name().replace('_', ' ').toLowerCase(Locale.ROOT),
        r.getAmount(),
        r.getCurrency(),
        r.getMaker(),
        r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt(),
        r.getCompanyId(),
        LINK + "?tab=thresholds");
  }

  private PendingApproval requirement(EbRequiredDocument d) {
    return new PendingApproval(
        MODULE,
        "EB required document",
        "RD-" + d.getId(),
        lovs.label("DOCUMENT_TYPE", d.getDocumentType())
            + " for "
            + lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, d.getProcessType()),
        null,
        null,
        d.getMaker(),
        d.getUpdatedAt() == null ? d.getCreatedAt() : d.getUpdatedAt(),
        d.getCompanyId(),
        LINK + "?tab=documents");
  }
}
