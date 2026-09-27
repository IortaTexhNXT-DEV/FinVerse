package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hr;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbContactRole;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleOutcome;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.home.service.EbHomeService;
import com.iortatechnxt.brokerverse.eb.programme.service.EbClientRecords;
import com.iortatechnxt.brokerverse.eb.programme.service.EbProgrammeRetentionProvider;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeInput;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery.ProgrammeCriteria;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeRow;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeService;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeView;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeViewService;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Programmes and cycles (FR-EB-020, 021; wave E1-B): creation with lines and contacts, validation,
 * maintenance, cycles with their business type and work case, the requirement steps with the BOR
 * gate, closing as lost, the work list, EB Home, the client 360 records and retention.
 */
@IntegrationTest
class EbProgrammeIT {

  @Autowired private EbFixtures fx;
  @Autowired private ProgrammeService programmes;
  @Autowired private ProgrammeViewService views;
  @Autowired private ProgrammeQuery query;
  @Autowired private CycleService cycles;
  @Autowired private BorService bor;
  @Autowired private EbProgrammeRepository programmeRepository;
  @Autowired private EbCycleRepository cycleRepository;
  @Autowired private WorkCaseRepository workCases;
  @Autowired private WorkflowService workflow;
  @Autowired private EbHomeService home;
  @Autowired private EbClientRecords clientRecords;
  @Autowired private EbProgrammeRetentionProvider retention;
  @Autowired private AsUser as;

  @Test
  void aProgrammeIsCreatedWithItsLinesContactsNumberAndStatus() {
    EbProgramme existing =
        fx.programme(
            true,
            List.of(
                hmo(LocalDate.of(2027, 3, 1), "ARN-2026-" + EbFixtures.token()),
                new EbProgrammeLine.Data("GLI", null, "INS-LAC", null, null, null, null, 80)));
    assertThat(existing.getProgrammeNo()).matches("EBP-\\d{4}-\\d{6}");
    assertThat(existing.getStatus()).isEqualTo(EbProgrammeStatus.ACTIVE);
    assertThat(existing.getAccountOfficer()).isEqualTo(AO);
    ProgrammeView view = views.view(fx.company(), existing.getId());
    assertThat(view.lines())
        .extracting(ProgrammeView.LineView::benefitLine)
        .containsExactly("HMO", "GLI");
    assertThat(view.contacts()).hasSize(1);
    assertThat(view.client().code()).isEqualTo(EbFixtures.CLIENT);
    assertThat(view.currentCycleId()).isNull();

    EbProgramme prospect =
        fx.programme(
            false,
            List.of(new EbProgrammeLine.Data("GPA", null, null, null, null, null, null, 20)));
    assertThat(prospect.getStatus()).isEqualTo(EbProgrammeStatus.PROSPECT);

    List<ClientRecord> records = clientRecords.recordsOf(fx.client().getId());
    assertThat(records)
        .anyMatch(
            r ->
                r.reference().equals(existing.getProgrammeNo())
                    && r.link().endsWith(existing.getId().toString()));
  }

  @Test
  void programmeDataIsValidated() {
    Long clientId = fx.client().getId();
    assertThatThrownBy(() -> create(input(clientId, "BDO", List.of(), List.of(hr()))))
        .extracting("code")
        .isEqualTo("EB_LINE_REQUIRED");
    assertThatThrownBy(() -> create(input(clientId, "BDO", List.of(hmo(null, null)), List.of())))
        .extracting("code")
        .isEqualTo("EB_CONTACT_REQUIRED");
    assertThatThrownBy(
            () ->
                create(
                    input(
                        clientId,
                        "BDO",
                        List.of(hmo(null, null)),
                        List.of(
                            new EbProgrammeContact.Data(
                                "Hr",
                                "not-an-address",
                                null,
                                EbContactRole.HR_HEAD,
                                true,
                                false)))))
        .extracting("code")
        .isEqualTo("EMAIL_ADDRESS_INVALID");
    assertThatThrownBy(
            () -> create(input(clientId, "NOPE", List.of(hmo(null, null)), List.of(hr()))))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(
            () ->
                create(
                    input(
                        clientId,
                        "BDO",
                        List.of(
                            new EbProgrammeLine.Data(
                                "HMO",
                                null,
                                null,
                                null,
                                null,
                                LocalDate.of(2027, 1, 1),
                                LocalDate.of(2026, 1, 1),
                                5)),
                        List.of(hr()))))
        .extracting("code")
        .isEqualTo("EB_LINE_PERIOD_INVALID");
    assertThatThrownBy(() -> create(input(null, "BDO", List.of(hmo(null, null)), List.of(hr()))))
        .extracting("code")
        .isEqualTo("EB_CLIENT_REQUIRED");
    ProgrammeInput notAnAo =
        new ProgrammeInput(
            clientId,
            "X",
            "BDO",
            EbFunding.EMPLOYER,
            "ao",
            null,
            true,
            List.of(hmo(null, null)),
            List.of(hr()));
    assertThatThrownBy(() -> create(notAnAo)).extracting("code").isEqualTo("EB_AO_INVALID");
  }

  private EbProgramme create(ProgrammeInput input) {
    return as.run(AO, () -> programmes.create(fx.company(), input));
  }

  private static ProgrammeInput input(
      Long clientId,
      String team,
      List<EbProgrammeLine.Data> lines,
      List<EbProgrammeContact.Data> contacts) {
    return new ProgrammeInput(
        clientId,
        "Benefits " + EbFixtures.token(),
        team,
        EbFunding.EMPLOYER,
        null,
        null,
        true,
        lines,
        contacts);
  }

  @Test
  void linesContactsAndProfileAreMaintained() {
    EbProgramme p = fx.programme(true, List.of(hmo(LocalDate.of(2027, 5, 1), null)));
    Long company = fx.company();
    as.run(
        AO,
        () ->
            programmes.update(
                company,
                p.getId(),
                new EbProgramme.Profile("Renamed", "SM", EbFunding.VOLUNTARY, null, "U1", false)));
    as.run(
        AO,
        () ->
            programmes.addLine(
                company,
                p.getId(),
                new EbProgrammeLine.Data("GPA", null, null, null, null, null, null, 10)));
    as.run(
        AO,
        () ->
            programmes.updateLine(
                company,
                p.getId(),
                2,
                new EbProgrammeLine.Data("GPA", null, "INS-LAC", "GPA-1", null, null, null, 12)));
    as.run(AO, () -> programmes.deactivateLine(company, p.getId(), 2));
    assertThatThrownBy(() -> as.run(AO, () -> programmes.deactivateLine(company, p.getId(), 1)))
        .extracting("code")
        .isEqualTo("EB_LINE_REQUIRED");
    as.run(AO, () -> programmes.addContact(company, p.getId(), hr()));
    ProgrammeView view = views.view(company, p.getId());
    Long first = view.contacts().get(0).id();
    Long second = view.contacts().get(1).id();
    as.run(
        AO,
        () ->
            programmes.updateContact(
                company,
                p.getId(),
                second,
                new EbProgrammeContact.Data(
                    "Finance",
                    "finance@client.example",
                    "0917",
                    EbContactRole.FINANCE,
                    false,
                    true)));
    as.run(AO, () -> programmes.deactivateContact(company, p.getId(), first));
    assertThatThrownBy(
            () -> as.run(AO, () -> programmes.deactivateContact(company, p.getId(), second)))
        .extracting("code")
        .isEqualTo("EB_CONTACT_REQUIRED");

    ProgrammeView after = views.view(company, p.getId());
    assertThat(after.name()).isEqualTo("Renamed");
    assertThat(after.teamCode()).isEqualTo("SM");
    assertThat(after.accountOfficer()).isEqualTo(AO);
    assertThat(after.renewalEligible()).isFalse();
    assertThat(after.lines().get(1).active()).isFalse();
    assertThat(after.lines().get(1).incumbentInsurer()).isEqualTo("INS-LAC");
    assertThat(after.contacts().get(1).role()).isEqualTo("FINANCE");
    assertThat(after.contacts().get(0).active()).isFalse();
  }

  @Test
  void cyclesCarryTheirBusinessTypeWorkCaseAndOneOpenCyclePerYear() {
    EbProgramme p = fx.programme(false, List.of(hmo(LocalDate.of(2027, 1, 1), null)));
    Long company = fx.company();
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        cycles.open(
                            company, p.getId(), new CycleService.OpenCycle(null, 2027, null))))
        .extracting("code")
        .isEqualTo("EB_BUSINESS_TYPE_REQUIRED");
    assertThatThrownBy(() -> fx.cycle(p, BusinessType.RENEWAL, 2027))
        .extracting("code")
        .isEqualTo("EB_NOT_RENEWAL_ELIGIBLE");
    EbCycle nb = fx.cycle(p, BusinessType.NEW_BUSINESS, 2027);
    assertThat(nb.getCycleNo()).matches("EBC-\\d{4}-\\d{6}");
    assertThat(nb.getBusinessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    WorkCase wc =
        workCases
            .findByEntityTypeAndEntityId(EbCodes.ENTITY_CYCLE, nb.getId().toString())
            .orElseThrow();
    assertThat(wc.getStageCode()).isEqualTo("OPEN");
    assertThatThrownBy(() -> fx.cycle(p, BusinessType.NEW_BUSINESS, 2027))
        .extracting("code")
        .isEqualTo("EB_CYCLE_OPEN_EXISTS");

    assertThatThrownBy(() -> as.run(AO, () -> cycles.stayWithIncumbent(company, nb.getId())))
        .extracting("code")
        .isEqualTo("EB_NOT_A_RENEWAL");
    as.run(AO, () -> cycles.start(company, nb.getId()));
    assertThat(cycleRepository.findById(nb.getId()).orElseThrow().getStage())
        .isEqualTo(EbCycleStage.REQUIREMENTS);
    assertThatThrownBy(() -> as.run(AO, () -> cycles.start(company, nb.getId())))
        .extracting("code")
        .isEqualTo("EB_CYCLE_STAGE");

    assertThatThrownBy(() -> as.run(AO, () -> cycles.remarket(company, nb.getId())))
        .extracting("code")
        .isEqualTo("EB_BOR_REQUIRED");
    EbBor version = as.run(AO, () -> bor.upload(company, nb.getId(), pdf("bor.pdf")));
    as.run(
        "ebproc",
        () ->
            bor.validate(
                company,
                version.getId(),
                new EbBor.Checklist(
                    true, true, true, LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31))));
    as.run(AO, () -> cycles.remarket(company, nb.getId()));
    EbCycle marketed = cycleRepository.findById(nb.getId()).orElseThrow();
    assertThat(marketed.getStage()).isEqualTo(EbCycleStage.FRANCHISE);
    assertThat(marketed.isRemarketing()).isTrue();
  }

  @Test
  void aRenewalStaysWithTheIncumbentAndALostCycleRecordsItsOutcome() {
    EbProgramme p = fx.programme(true, List.of(hmo(LocalDate.of(2028, 2, 1), null)));
    EbCycle renewal = fx.cycle(p, BusinessType.RENEWAL, 2028);
    assertThatThrownBy(() -> as.run(AO, () -> cycles.start(fx.company(), renewal.getId())))
        .extracting("code")
        .isEqualTo("EB_RENEWAL_STARTS_WITH_RA");
    fx.move(renewal, "send_ra", "record_feedback");
    as.run(AO, () -> cycles.stayWithIncumbent(fx.company(), renewal.getId()));
    assertThat(cycleRepository.findById(renewal.getId()).orElseThrow().getStage())
        .isEqualTo(EbCycleStage.INCUMBENT_TERMS);

    WorkCase wc =
        workCases
            .findByEntityTypeAndEntityId(EbCodes.ENTITY_CYCLE, renewal.getId().toString())
            .orElseThrow();
    as.run(
        AO,
        () ->
            workflow.genericTransition(
                wc.getId(), "not_renewed", new TransitionNote("PRICE", "Too high")));
    EbCycle closed = cycleRepository.findById(renewal.getId()).orElseThrow();
    assertThat(closed.getOutcome()).isEqualTo(EbCycleOutcome.NOT_RENEWED);
    assertThat(closed.getOutcomeReason()).isEqualTo("PRICE");
    assertThat(closed.isOpen()).isFalse();
    assertThat(programmeRepository.findById(p.getId()).orElseThrow().getStatus())
        .isEqualTo(EbProgrammeStatus.LAPSED);
    assertThatThrownBy(
            () -> as.run(AO, () -> cycles.stayWithIncumbent(fx.company(), renewal.getId())))
        .extracting("code")
        .isEqualTo("EB_CYCLE_CLOSED");

    EbProgramme prospect =
        fx.programme(
            false,
            List.of(new EbProgrammeLine.Data("GPA", null, null, null, null, null, null, 15)));
    EbCycle nb = fx.cycle(prospect, BusinessType.NEW_BUSINESS, 2029);
    WorkCase nbCase =
        workCases
            .findByEntityTypeAndEntityId(EbCodes.ENTITY_CYCLE, nb.getId().toString())
            .orElseThrow();
    as.run(
        AO,
        () ->
            workflow.genericTransition(
                nbCase.getId(), "close_lost", new TransitionNote("NO_RESPONSE", null)));
    assertThat(cycleRepository.findById(nb.getId()).orElseThrow().getOutcome())
        .isEqualTo(EbCycleOutcome.LOST);
    assertThat(programmeRepository.findById(prospect.getId()).orElseThrow().getStatus())
        .isEqualTo(EbProgrammeStatus.LOST);
    assertThat(
            retention.eligible(
                new RetentionCriteria(Set.of("LOST"), LocalDate.now().plusDays(1)), 5000))
        .anyMatch(c -> c.reference().equals(prospect.getProgrammeNo()));
    assertThat(
            retention.countEligible(
                new RetentionCriteria(Set.of("LOST"), LocalDate.now().plusDays(1))))
        .isPositive();
    assertThat(retention.recordType()).isEqualTo("EB_PROGRAMME");
  }

  @Test
  void theWorkListFiltersByTabStageAndSearchAndHomeCounts() {
    LocalDate soon = LocalDate.now().plusDays(60);
    EbProgramme due = fx.programme(true, List.of(hmo(soon, null)));
    EbProgramme inProgress = fx.programme(false, List.of(hmo(null, null)));
    EbCycle cycle = fx.cycle(inProgress, BusinessType.NEW_BUSINESS, 2030);
    fx.move(cycle, "start");

    assertThat(numbers(ProgrammeQuery.Tab.RENEWAL_DUE, null, due.getProgrammeNo()))
        .containsExactly(due.getProgrammeNo());
    assertThat(numbers(ProgrammeQuery.Tab.RENEWAL_DUE, null, inProgress.getProgrammeNo()))
        .isEmpty();
    assertThat(numbers(ProgrammeQuery.Tab.IN_PROGRESS, null, inProgress.getProgrammeNo()))
        .containsExactly(inProgress.getProgrammeNo());
    assertThat(numbers(ProgrammeQuery.Tab.ALL, "REQUIREMENTS", inProgress.getProgrammeNo()))
        .containsExactly(inProgress.getProgrammeNo());
    assertThat(numbers(ProgrammeQuery.Tab.WITH_CLIENT, null, inProgress.getProgrammeNo()))
        .isEmpty();
    ProgrammeRow row =
        query
            .search(
                fx.company(),
                new ProgrammeCriteria(
                    ProgrammeQuery.Tab.ALL, null, AO, "BDO", inProgress.getName()),
                0,
                10)
            .content()
            .get(0);
    assertThat(row.cycle().cycleNo()).isEqualTo(cycle.getCycleNo());
    assertThat(row.cycle().stage()).isEqualTo("REQUIREMENTS");
    assertThat(row.lines()).isEqualTo("HMO");

    var counts = home.counts(fx.company());
    assertThat(counts)
        .containsKeys("raDue", "awaitingFeedback", "withClient", "pendingItemsOverdue");
    assertThat(counts.get("raDue")).isPositive();
  }

  private List<String> numbers(ProgrammeQuery.Tab tab, String stage, String text) {
    return query
        .search(fx.company(), new ProgrammeCriteria(tab, stage, null, null, text), 0, 20)
        .content()
        .stream()
        .map(ProgrammeRow::programmeNo)
        .toList();
  }
}
