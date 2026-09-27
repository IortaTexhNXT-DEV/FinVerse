package com.iortatechnxt.brokerverse.eb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.eb.domain.EbActivity;
import com.iortatechnxt.brokerverse.eb.domain.EbActivityRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbContactRole;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleOutcome;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStageChanged;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Employee Benefits foundation (wave E0): V1030 seeds (roles, grants, lists, document types,
 * parameters, workflows, templates), V1033 core records with their entities, the cycle stage mirror
 * and event, the EB parameters and the report category.
 */
@IntegrationTest
@RecordApplicationEvents
class EbFoundationIT {

  private static final String CLIENT = "CL-2026-000001";

  @Autowired private EbProgrammeRepository programmes;
  @Autowired private EbCycleRepository cycles;
  @Autowired private EbDocumentRepository documents;
  @Autowired private EbActivityRepository activities;
  @Autowired private EbParameters parameters;
  @Autowired private DocumentService documentService;
  @Autowired private WorkflowService workflow;
  @Autowired private ClientService clients;
  @Autowired private LovService lovs;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;
  @Autowired private ApplicationEvents events;
  @Autowired private AsUser as;

  private static String token() {
    return Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase();
  }

  private List<String> grants(String role) {
    return jdbc.queryForList(
        "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
            + " where r.code = ?",
        String.class,
        role);
  }

  @Test
  void rolesAndGrantsFollowTheMatrixWithoutPortalRights() {
    assertThat(grants("EB_AO")).contains("EB_VIEW", "EB_MARKET", "EB_REPORT_VIEW", "WORK_VIEW");
    assertThat(grants("EB_TL")).contains("EB_COMPARATIVE_APPROVE", "WORK_ASSIGN");
    assertThat(grants("EB_MANAGEMENT"))
        .contains("EB_THRESHOLD_APPROVE")
        .doesNotContain("EB_MARKET");
    assertThat(grants("EB_PROCESSOR")).contains("EB_PROCESS", "ACCOUNT_PROCESS");
    assertThat(grants("EB_PROC_SUPERVISOR")).contains("EB_PROCESS", "WORK_ASSIGN");
    assertThat(grants("EB_COLLECTION")).contains("EB_COLLECT", "CLX_VIEW", "CLX_WORK");
    assertThat(grants("BUSINESS_ADMIN")).contains("EB_SETUP", "EB_VIEW");
    assertThat(grants("AUDITOR")).contains("EB_VIEW", "EB_REPORT_VIEW");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_role_permission where permission like 'PORTAL%'",
                Long.class))
        .isZero();
    List<String> enumNames = Arrays.stream(Permission.values()).map(Enum::name).toList();
    jdbc.queryForList(
            "select distinct permission from sec_role_permission where permission like 'EB\\_%'",
            String.class)
        .forEach(p -> assertThat(enumNames).contains(p));
  }

  @Test
  void listsDocumentTypesParametersWorkflowsAndTemplatesAreSeeded() {
    assertThat(lovs.label(EbCodes.LOV_BENEFIT_LINE, "HMO"))
        .isEqualTo("Health Maintenance Organization (HMO)");
    assertThat(lovs.label(EbCodes.LOV_TEAM, "SOLICITED")).isEqualTo("Solicited");
    EbDocumentTypes.ALL.forEach(t -> assertThat(lovs.label("DOCUMENT_TYPE", t)).isNotBlank());
    assertThat(lovs.label("DOCUMENT_TYPE", EbDocumentTypes.RENEWAL_ADVICE))
        .isEqualTo("Renewal advice");

    assertThat(parameters.raLeadDays()).isEqualTo(135);
    assertThat(parameters.raReminderDays()).containsExactly(120, 105, 90);
    assertThat(parameters.proposalReplyDays()).isEqualTo(5);
    assertThat(parameters.franchiseTatDays()).isEqualTo(5);
    assertThat(parameters.franchiseAdviceDays()).isEqualTo(2);
    assertThat(parameters.comparativeDays()).isEqualTo(3);
    assertThat(parameters.followUpDays()).isEqualTo(5);
    assertThat(parameters.followUpMax()).isEqualTo(3);
    assertThat(parameters.adjustmentRequiresPayment()).isFalse();
    assertThat(parameters.tatDays(TatActivity.SOA_VALIDATION)).isEqualTo(3);
    assertThat(parameters.tatDays(TatActivity.POLICY_SOA)).isEqualTo(10);
    for (TatActivity activity : TatActivity.values()) {
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from sys_parameter where param_key = ?",
                  Long.class,
                  activity.parameter()))
          .as(activity.parameter())
          .isEqualTo(1L);
    }

    assertThat(stages(EbCodes.WORKFLOW_CYCLE)).hasSize(EbCycleStage.values().length);
    Arrays.stream(EbCycleStage.values())
        .forEach(s -> assertThat(stages(EbCodes.WORKFLOW_CYCLE)).contains(s.name()));
    assertThat(stages(EbCodes.WORKFLOW_FRANCHISE)).hasSize(6);
    assertThat(stages(EbCodes.WORKFLOW_MEMBER_CHANGE)).hasSize(6);
    assertThat(stages(EbCodes.WORKFLOW_SOA)).hasSize(4);
    assertThat(stages("PORTAL_UPLOAD_REVIEW")).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from doc_template where code like 'EB\\_%'", Long.class))
        .isEqualTo(10L);
    assertThat(
            jdbc.queryForObject(
                "select param_value from sys_parameter where param_key = 'BOOKING_BILLING_NO_LINES'",
                String.class))
        .isEqualTo("HMO,GLI,GPA");
  }

  private List<String> stages(String workflowCode) {
    return jdbc.queryForList(
        "select stage_code from wf_stage where workflow_code = ?", String.class, workflowCode);
  }

  @Test
  void programmesAndCyclesPersistAndTheCycleMirrorsItsWorkCase() {
    Long company = data.company().getId();
    Client client = clients.requireByCode(company, CLIENT);
    String no = "EBP-2026-" + token();
    EbProgramme saved =
        tx.execute(
            s -> {
              EbProgramme p =
                  new EbProgramme(
                      company,
                      no,
                      new EbProgramme.ClientRef(
                          client.getId(), client.getClientCode(), client.getDisplayName()),
                      new EbProgramme.Profile(
                          "Group health " + no, "BDO", EbFunding.EMPLOYER, "ao", null, true));
              p.addLine(
                  new EbProgrammeLine.Data(
                      "HMO",
                      null,
                      "INS-MGIC",
                      "HMO-POL-1",
                      "ARN-2025-000123",
                      LocalDate.of(2026, 1, 1),
                      LocalDate.of(2027, 1, 1),
                      120));
              p.addLine(new EbProgrammeLine.Data("GLI", null, null, null, null, null, null, 120));
              p.addContact(
                  new EbProgrammeContact.Data(
                      "Hr Head", "hr@example.ph", null, EbContactRole.HR_HEAD, true, false));
              return programmes.save(p);
            });
    EbProgramme loaded = programmes.findByProgrammeNo(no).orElseThrow();
    assertThat(loaded.getLines())
        .extracting(EbProgrammeLine::getBenefitLine)
        .containsExactly("HMO", "GLI");
    assertThat(loaded.getLines().get(0).getLineNo()).isEqualTo(1);
    assertThat(loaded.line(2).getHeadcount()).isEqualTo(120);
    assertThat(loaded.getStatus()).isEqualTo(EbProgrammeStatus.PROSPECT);
    assertThatThrownBy(() -> loaded.line(9)).extracting("code").isEqualTo("EB_LINE_NOT_FOUND");
    assertThatThrownBy(() -> new EbCycle(company, "EBC-X", saved.getId(), null, 2027, null))
        .extracting("code")
        .isEqualTo("EB_BUSINESS_TYPE_REQUIRED");

    EbCycle cycle = openCycle(company, saved.getId());
    as.run(
        "ao",
        () ->
            workflow.systemTransition(
                EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "send_ra", TransitionNote.NONE));
    assertThat(cycles.findById(cycle.getId()).orElseThrow().getStage())
        .isEqualTo(EbCycleStage.RA_SENT);
    assertThat(events.stream(EbCycleStageChanged.class))
        .anyMatch(e -> e.cycleId().equals(cycle.getId()) && e.to() == EbCycleStage.RA_SENT);

    as.run(
        "ao",
        () ->
            workflow.systemTransition(
                EbCodes.ENTITY_CYCLE,
                cycle.getId().toString(),
                "not_renewed",
                new TransitionNote("PRICE", "Client moved")));
    tx.executeWithoutResult(
        s -> {
          EbCycle closed = cycles.findById(cycle.getId()).orElseThrow();
          closed.recordOutcome(EbCycleOutcome.NOT_RENEWED, "PRICE", "Client moved");
        });
    EbCycle closed = cycles.findById(cycle.getId()).orElseThrow();
    assertThat(closed.getStage()).isEqualTo(EbCycleStage.NOT_RENEWED);
    assertThat(closed.isOpen()).isFalse();
    assertThat(closed.getOutcome()).isEqualTo(EbCycleOutcome.NOT_RENEWED);
    assertThat(cycles.findOpen(saved.getId(), 2027)).isEmpty();

    EbCycle next = openCycle(company, saved.getId());
    tx.executeWithoutResult(
        s -> cycles.findById(next.getId()).orElseThrow().recordAccount("ARN-2027-" + token()));
    String arn =
        tx.execute(s -> cycles.findById(next.getId()).orElseThrow().getAccountArns().get(0));
    assertThat(cycles.findByAccountArn(arn))
        .get()
        .extracting(EbCycle::getId)
        .isEqualTo(next.getId());
    assertThat(cycles.findOpen(saved.getId(), 2027)).isPresent();
  }

  private EbCycle openCycle(Long company, Long programmeId) {
    return tx.execute(
        s -> {
          EbCycle c =
              cycles.save(
                  new EbCycle(
                      company,
                      "EBC-2026-" + token(),
                      programmeId,
                      BusinessType.RENEWAL,
                      2027,
                      LocalDate.of(2027, 1, 1)));
          workflow.start(
              new StartCase(
                  company,
                  EbCodes.WORKFLOW_CYCLE,
                  new CaseRecord(
                      EbCodes.ENTITY_CYCLE,
                      c.getId().toString(),
                      c.getCycleNo(),
                      "Renewal 2027",
                      "/eb/programmes/" + programmeId,
                      "BDO"),
                  null));
          return c;
        });
  }

  @Test
  void documentsAndActivityStampsAreRegistered() {
    Long company = data.company().getId();
    Client client = clients.requireByCode(company, CLIENT);
    EbProgramme programme =
        programmes.save(
            new EbProgramme(
                company,
                "EBP-2026-" + token(),
                new EbProgramme.ClientRef(client.getId(), client.getClientCode(), "Client"),
                new EbProgramme.Profile("Docs", "SM", EbFunding.VOLUNTARY, "ao", null, false)));
    Long attachmentId =
        as.run(
                "ao",
                () ->
                    documentService.upload(
                        new AttachmentTarget(
                            EbCodes.ENTITY_PROGRAMME, programme.getId().toString()),
                        List.of(
                            new UploadedFile(
                                "tor.pdf", "%PDF-1.4 tor".getBytes(StandardCharsets.US_ASCII))),
                        new UploadOptions(
                            EbDocumentTypes.TOR,
                            false,
                            null,
                            null,
                            EbDocumentTypes.PROPOSAL_PROCESS)))
            .get(0)
            .getId();
    EbDocument.Place place = new EbDocument.Place(programme.getId(), null);
    EbDocument tor =
        documents.save(
            new EbDocument(
                company,
                place,
                EbDocumentTypes.TOR,
                EbDocumentTypes.PROPOSAL_PROCESS,
                1,
                attachmentId,
                EbDocumentSource.AO));
    tor.supersede();
    documents.save(tor);
    assertThat(documents.findByProgrammeIdOrderByIdAsc(programme.getId()))
        .extracting(EbDocument::getStatus)
        .containsExactly(EbDocumentStatus.SUPERSEDED);

    Instant received = Instant.parse("2026-09-01T01:00:00Z");
    EbActivity stamp =
        activities.save(
            new EbActivity(
                company, place, TatActivity.FRANCHISE_DECISION, "EBF-1", received, "INS-MGIC"));
    assertThatThrownBy(() -> stamp.release(received.minusSeconds(60), null))
        .extracting("code")
        .isEqualTo("EB_ACTIVITY_RELEASE_BEFORE_RECEIPT");
    assertThat(
            activities.findFirstByActivityAndReferenceAndReleasedAtIsNullOrderByIdDesc(
                TatActivity.FRANCHISE_DECISION, "EBF-1"))
        .isPresent();
    stamp.release(received.plusSeconds(3600), "Approved");
    activities.save(stamp);
    assertThat(activities.findByProgrammeIdOrderByReceivedAtAscIdAsc(programme.getId()))
        .extracting(EbActivity::getReleasedAt)
        .containsExactly(received.plusSeconds(3600));
  }

  @Test
  void theReportCategoryAndMetadataFactoryExist() {
    ReportMetadata metadata =
        ReportMetadata.employeeBenefits("EB-TEST", "Test", "Test report", List.of());
    assertThat(metadata.category()).isEqualTo(ReportCategory.EMPLOYEE_BENEFITS);
    assertThat(metadata.category().label()).isEqualTo("Employee Benefits");
    assertThat(metadata.permission()).isEqualTo(Permission.EB_REPORT_VIEW);
  }
}
