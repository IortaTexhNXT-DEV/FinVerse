package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTorRepository;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Terms of Reference of a cycle (BRID-009; FR-EB-035): structured items per benefit line and
 * plan, maintained as a draft and released as a PDF stored as {@code EB_TOR}. A change after
 * release is a new version (the earlier one is superseded); requests still open move to the new
 * version and their insurers receive it ({@link InsurerRequestService#resend}).
 */
@Service
@Transactional
public class TorService {

  /** Stages in which the TOR can be prepared and released. */
  static final Set<EbCycleStage> TOR_STAGES =
      Set.of(
          EbCycleStage.REQUIREMENTS,
          EbCycleStage.INCUMBENT_TERMS,
          EbCycleStage.FRANCHISE,
          EbCycleStage.PROPOSALS,
          EbCycleStage.REVISION);

  private final EbTorRepository tors;
  private final EbRecords records;
  private final EbDocumentService documents;
  private final EbTemplates templates;
  private final EbParties parties;
  private final LovService lovs;
  private final InsurerRequestService requests;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param tors TOR versions
   * @param records cycle look-up
   * @param documents EB document register
   * @param templates TOR template and PDF
   * @param parties template values
   * @param lovs benefit lines
   * @param requests open insurer requests
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public TorService(
      EbTorRepository tors,
      EbRecords records,
      EbDocumentService documents,
      EbTemplates templates,
      EbParties parties,
      LovService lovs,
      InsurerRequestService requests,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.tors = tors;
    this.records = records;
    this.documents = documents;
    this.templates = templates;
    this.parties = parties;
    this.lovs = lovs;
    this.requests = requests;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Replaces the items of the cycle's draft TOR (a draft is started from the last release).
   *
   * @param companyId company
   * @param cycleId cycle
   * @param items items in order, at least one
   * @return the draft
   */
  public EbTor saveItems(Long companyId, Long cycleId, List<EbTorItem.Data> items) {
    EbCycle cycle = requireTorStage(companyId, cycleId);
    List<EbTorItem.Data> clean = new ArrayList<>();
    int row = 1;
    for (EbTorItem.Data item : items == null ? List.<EbTorItem.Data>of() : items) {
      clean.add(check(item, row++));
    }
    if (clean.isEmpty()) {
      throw new BusinessRuleException("EB_TOR_EMPTY", "Add the TOR items before release");
    }
    EbTor draft = draft(cycle);
    draft.replaceItems(clean);
    return draft;
  }

  private EbTorItem.Data check(EbTorItem.Data item, int row) {
    if (item.benefitLine() == null || item.benefitLine().isBlank()) {
      throw new BusinessRuleException(
          "EB_TOR_ITEM_LINE", "Item " + row + ": select the benefit line");
    }
    lovs.requireValid(EbCodes.LOV_BENEFIT_LINE, item.benefitLine(), BusinessClock.today(clock));
    if (blank(item.description()) || blank(item.requirement())) {
      throw new BusinessRuleException(
          "EB_TOR_ITEM_TEXT", "Item " + row + ": enter the item and the requirement");
    }
    return new EbTorItem.Data(
        item.benefitLine(),
        blank(item.planCode()) ? null : item.planCode().strip(),
        item.description().strip(),
        item.requirement().strip());
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private EbTor draft(EbCycle cycle) {
    Optional<EbTor> draft =
        tors.findFirstByCycleIdAndStatusOrderByVersionNoDesc(cycle.getId(), EbTor.Status.DRAFT);
    if (draft.isPresent()) {
      return draft.get();
    }
    List<EbTor> versions = tors.findByCycleIdOrderByVersionNoDesc(cycle.getId());
    int next = versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    return tors.save(new EbTor(cycle, next));
  }

  /**
   * Releases the draft TOR: the PDF is stored as {@code EB_TOR}, the earlier release is superseded
   * and open requests receive the new version.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the released version
   */
  public EbTor release(Long companyId, Long cycleId) {
    EbCycle cycle = requireTorStage(companyId, cycleId);
    EbTor draft =
        tors.findFirstByCycleIdAndStatusOrderByVersionNoDesc(cycle.getId(), EbTor.Status.DRAFT)
            .orElseThrow(
                () -> new BusinessRuleException("EB_TOR_EMPTY", "Add the TOR items before release"));
    if (draft.getItems().isEmpty()) {
      throw new BusinessRuleException("EB_TOR_EMPTY", "Add the TOR items before release");
    }
    EbProgramme programme = records.programmeOf(cycle);
    byte[] pdf = pdf(programme, cycle, draft);
    Long attachment =
        documents
            .store(
                cycle,
                new Registration(
                    EbDocumentTypes.TOR,
                    EbDocumentTypes.PROPOSAL_PROCESS,
                    EbDocumentSource.SYSTEM,
                    true,
                    "Terms of Reference version " + draft.getVersionNo()),
                List.of(new UploadedFile(cycle.getCycleNo() + "_TOR_v" + draft.getVersionNo() + ".pdf", pdf)))
            .get(0)
            .getAttachmentId();
    tors.findByCycleIdOrderByVersionNoDesc(cycle.getId()).forEach(EbTor::supersede);
    draft.release(clock.instant(), currentUser.username(), attachment);
    int resent = requests.resend(cycle, draft);
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.UPDATE,
        "TOR version " + draft.getVersionNo() + " released (" + draft.getItems().size()
            + " items; " + resent + " open request(s) updated)");
    return draft;
  }

  private byte[] pdf(EbProgramme programme, EbCycle cycle, EbTor tor) {
    Map<String, Object> values = parties.values(programme, cycle);
    values.put("torVersion", tor.getVersionNo());
    values.put(
        "torItems",
        tor.getItems().stream()
            .map(i -> i.getSortOrder() + ". " + i.getDescription() + ": " + i.getRequirement())
            .collect(Collectors.joining("\n")));
    MergedText text = templates.merge(EbCodes.TEMPLATE_TOR, values);
    List<List<String>> rows =
        tor.getItems().stream()
            .map(
                i ->
                    List.of(
                        String.valueOf(i.getSortOrder()),
                        lovs.label(EbCodes.LOV_BENEFIT_LINE, i.getBenefitLine()),
                        i.getPlanCode() == null ? "All plans" : i.getPlanCode(),
                        i.getDescription(),
                        i.getRequirement()))
            .toList();
    return templates.pdf(
        programme.getCompanyId(),
        new EbTemplates.Heading(
            "TERMS OF REFERENCE", cycle.getCycleNo() + " v" + tor.getVersionNo(), text.versionTag()),
        List.of(
            new DocumentSpec.Text(text.title(), text.text()),
            new DocumentSpec.Table(
                "Requirements",
                List.of("No.", "Benefit line", "Plan", "Item", "Requirement"),
                rows,
                List.of())),
        List.of(parties.aoName(programme)));
  }

  /**
   * The TOR versions of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return versions, latest first
   */
  @Transactional(readOnly = true)
  public List<EbTor> ofCycle(Long companyId, Long cycleId) {
    return tors.findByCycleIdOrderByVersionNoDesc(records.cycle(companyId, cycleId).getId());
  }

  private EbCycle requireTorStage(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (!TOR_STAGES.contains(cycle.getStage())) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE",
          "The TOR of cycle " + cycle.getCycleNo() + " can no longer be changed at this stage");
    }
    return cycle;
  }
}
