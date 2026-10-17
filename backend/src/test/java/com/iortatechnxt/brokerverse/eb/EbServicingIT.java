package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.realPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.member.service.MasterListBulkHandler;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeInput;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeService;
import com.iortatechnxt.brokerverse.eb.member.service.RosterService;
import com.iortatechnxt.brokerverse.eb.soa.service.EbSoaService;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaRelease;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Member servicing and SOAs (FR-EB-053 to 057; wave E1-C): the master list staged by the bulk
 * upload and accepted, member changes captured against the roster, relayed, billed directly,
 * validated and applied (with the HMO card of each member added), and the SOA register with its
 * duplicate checks, validation, release and rejection.
 */
@IntegrationTest
class EbServicingIT {

  private static final String PROCESSOR = "ebproc";
  private static final int YEAR = 2026;

  @Autowired private EbFixtures fx;
  @Autowired private MasterListBulkHandler masterList;
  @Autowired private RosterService roster;
  @Autowired private MemberChangeService changes;
  @Autowired private EbSoaService soas;
  @Autowired private SoaRelease release;
  @Autowired private EbTrackedItemRepository items;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private EbProgramme programme() {
    return fx.programme(true, List.of(hmo(LocalDate.of(YEAR, 12, 31), null)));
  }

  private static BulkRow row(int no, String employee, String last, String plan) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("Employee No", employee);
    values.put("Last Name", last);
    values.put("First Name", "Juan");
    values.put("Birth Date", "1990-05-01");
    values.put("Gender", "MALE");
    values.put("Plan", plan);
    values.put("Dependants", "1");
    return new BulkRow(no, values);
  }

  private EbRosterVersion load(EbProgramme p, String job, BulkRow... rows) {
    BulkContext context =
        new BulkContext(
            fx.company(),
            job,
            LocalDate.of(YEAR, 3, 1),
            Map.of(
                MasterListBulkHandler.PROGRAMME,
                p.getId().toString(),
                MasterListBulkHandler.POLICY_YEAR,
                "" + YEAR));
    for (BulkRow r : rows) {
      assertThat(masterList.validate(r, context)).isEmpty();
      as.run(AO, () -> masterList.commit(r, context));
    }
    return as.run(AO, () -> roster.versions(fx.company(), p.getId()).get(0));
  }

  private EbRosterVersion acceptedRoster(EbProgramme p) {
    EbRosterVersion staged =
        load(
            p,
            "BLK-" + EbFixtures.token(),
            row(2, "E-001", "Cruz", "PLAN-A"),
            row(3, "E-002", "Reyes", "PLAN-A"));
    return as.run(AO, () -> roster.accept(fx.company(), staged.getId()));
  }

  @Test
  void aMasterListIsStagedReviewedAndAcceptedAsTheRoster() {
    EbProgramme p = programme();
    EbRosterVersion first = acceptedRoster(p);
    assertThat(first.getStatus()).isEqualTo(EbRosterVersion.Status.ACCEPTED);
    assertThat(first.getHeadcount()).isEqualTo(2);

    EbRosterVersion second =
        load(
            p,
            "BLK-" + EbFixtures.token(),
            row(2, "E-001", "Cruz", "PLAN-B"),
            row(3, "E-003", "Santos", "PLAN-A"));
    assertThat(second.getVersionNo()).isEqualTo(2);
    assertThat(second.getStatus()).isEqualTo(EbRosterVersion.Status.STAGED);
    RosterService.Differences diff =
        as.run(AO, () -> roster.differences(fx.company(), second.getId()));
    assertThat(diff.added()).isEqualTo(1);
    assertThat(diff.removed()).isEqualTo(1);
    assertThat(diff.planChanges()).isEqualTo(1);
    as.run(AO, () -> roster.accept(fx.company(), second.getId()));
    assertThat(as.run(AO, () -> roster.versions(fx.company(), p.getId())))
        .extracting(EbRosterVersion::getStatus)
        .containsExactly(EbRosterVersion.Status.ACCEPTED, EbRosterVersion.Status.SUPERSEDED);
    assertThat(
            as.run(
                AO,
                () ->
                    roster.members(fx.company(), second.getId(), "santos", PageRequest.of(0, 10))))
        .extracting(EbMember::getEmployeeNo)
        .containsExactly("E-003");

    EbRosterVersion third = load(p, "BLK-" + EbFixtures.token(), row(2, "E-009", "Lim", "PLAN-A"));
    assertThatThrownBy(() -> as.run(AO, () -> roster.reject(fx.company(), third.getId(), " ")))
        .extracting("code")
        .isEqualTo("EB_ROSTER_REASON_REQUIRED");
    assertThat(
            as.run(AO, () -> roster.reject(fx.company(), third.getId(), "Incomplete list"))
                .getStatus())
        .isEqualTo(EbRosterVersion.Status.REJECTED);
    assertThat(
            masterList.validate(
                row(4, "E-010", "Tan", "PLAN-A"),
                new BulkContext(fx.company(), "X", LocalDate.of(YEAR, 3, 1), Map.of())))
        .isNotEmpty();
  }

  private MemberChangeInput.Line add(String employee) {
    return new MemberChangeInput.Line(
        EbMemberChange.Action.ADD,
        employee,
        new EbMember.Data(
            "Garcia", "Ana", LocalDate.of(1995, 2, 3), "FEMALE", "SINGLE", "PLAN-A", 0),
        LocalDate.of(YEAR, 6, 1));
  }

  private MemberChangeInput.Line delete(String employee) {
    return new MemberChangeInput.Line(
        EbMemberChange.Action.DELETE, employee, null, LocalDate.of(YEAR, 6, 1));
  }

  private EbMemberChange capture(
      EbProgramme p, boolean financial, MemberChangeInput.Line... lines) {
    return as.run(
        AO,
        () ->
            changes.capture(
                fx.company(),
                p.getId(),
                new MemberChangeInput(
                    1,
                    YEAR,
                    "CLIENT",
                    financial,
                    "Monthly movements",
                    List.of(lines),
                    List.of(realPdf("request.pdf")))));
  }

  @Test
  void aMemberChangeIsCheckedRelayedBilledValidatedAndAppliedToTheRoster() {
    EbProgramme p = programme();
    assertThatThrownBy(() -> capture(p, false, add("E-100")))
        .extracting("code")
        .isEqualTo("EB_ROSTER_REQUIRED");
    acceptedRoster(p);
    assertThatThrownBy(() -> capture(p, false, delete("E-404")))
        .extracting("code")
        .isEqualTo("EB_MEMBER_UNKNOWN");
    assertThatThrownBy(() -> capture(p, false, add("E-001")))
        .extracting("code")
        .isEqualTo("EB_MEMBER_EXISTS");
    MemberChangeInput.Line late =
        new MemberChangeInput.Line(
            EbMemberChange.Action.DELETE, "E-002", null, LocalDate.of(YEAR + 1, 2, 1));
    assertThatThrownBy(() -> capture(p, false, late))
        .extracting("code")
        .isEqualTo("EB_MEMBER_EFFECTIVE_PERIOD");

    EbMemberChange change = capture(p, false, add("E-100"), delete("E-002"));
    assertThat(change.getChangeNo()).startsWith("EBM-");
    assertThat(change.getStatus()).isEqualTo(EbMemberChange.Status.CAPTURED);

    as.run(AO, () -> changes.relay(fx.company(), change.getId()));
    List<EbTrackedItem> billing = items.findByMemberChangeIdOrderByIdAsc(change.getId());
    assertThat(billing).extracting(EbTrackedItem::getItemType).containsExactly("BILLING_INVOICE");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        changes.bill(
                            fx.company(),
                            change.getId(),
                            new EbMemberChange.Billing(null, "DB-1", null, true),
                            List.of())))
        .extracting("code")
        .isEqualTo("EB_BILLING_FILE_REQUIRED");
    EbMemberChange billed =
        as.run(
            AO,
            () ->
                changes.bill(
                    fx.company(),
                    change.getId(),
                    new EbMemberChange.Billing(
                        null, "DB-" + EbFixtures.token(), new BigDecimal("1500"), true),
                    List.of(realPdf("billing.pdf"))));
    assertThat(billed.isDirectBilled()).isTrue();
    assertThat(billed.getStatus()).isEqualTo(EbMemberChange.Status.BILLED);
    assertThat(items.findByMemberChangeIdOrderByIdAsc(change.getId()).get(0).getReceivedOn())
        .isNotNull();

    as.run(PROCESSOR, () -> changes.validate(fx.company(), change.getId()));
    EbMemberChange closed = as.run(PROCESSOR, () -> changes.close(fx.company(), change.getId()));
    assertThat(closed.getStatus()).isEqualTo(EbMemberChange.Status.CLOSED);
    EbRosterVersion accepted = as.run(AO, () -> roster.accepted(p.getId(), YEAR).orElseThrow());
    assertThat(accepted.getHeadcount()).isEqualTo(2);
    List<EbTrackedItem> cards =
        items.findByMemberChangeIdOrderByIdAsc(change.getId()).stream()
            .filter(i -> "HMO_CARD".equals(i.getItemType()))
            .toList();
    assertThat(cards).hasSize(1);
    assertThat(cards.get(0).getMemberId()).isNotNull();
    assertThat(cards.get(0).getMemberRef()).isEqualTo("E-100");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from doc_attachment where entity_type = 'EbMemberChange' and entity_id = ?",
                Integer.class,
                change.getId().toString()))
        .isEqualTo(2);
  }

  @Test
  void aFinancialChangeNeedsTheBookedInvoiceOfTheLineForItsEndorsementRequest() {
    EbProgramme p = programme();
    acceptedRoster(p);
    EbMemberChange change = capture(p, true, add("E-200"));
    as.run(AO, () -> changes.relay(fx.company(), change.getId()));
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        changes.bill(
                            fx.company(),
                            change.getId(),
                            new EbMemberChange.Billing(null, "B-1", null, false),
                            List.of())))
        .extracting("code")
        .isEqualTo("EB_BILLING_AMOUNT_REQUIRED");
    as.run(
        AO,
        () ->
            changes.bill(
                fx.company(),
                change.getId(),
                new EbMemberChange.Billing(null, "B-1", new BigDecimal("800"), false),
                List.of()));
    assertThatThrownBy(
            () -> as.run(PROCESSOR, () -> changes.validate(fx.company(), change.getId())))
        .extracting("code")
        .isEqualTo("EB_NO_INVOICE");
  }

  private EbSoa receive(EbProgramme p, String soaNo, UploadedFile file) {
    return as.run(
        PROCESSOR,
        () ->
            soas.receive(
                fx.company(),
                p.getId(),
                new EbSoa.Intake(
                    "INS-MGIC",
                    soaNo,
                    LocalDate.of(YEAR, 1, 1),
                    LocalDate.of(YEAR, 3, 31),
                    new BigDecimal("125000"),
                    null,
                    null,
                    null),
                List.of(),
                file));
  }

  @Test
  void anSoaIsRegisteredOnceValidatedAndReleasedOrRejected() {
    EbProgramme p = programme();
    String soaNo = "SOA-" + EbFixtures.token();
    UploadedFile file = realPdf("soa.pdf");
    EbSoa soa = receive(p, soaNo, file);
    assertThat(soa.getSoaNo()).startsWith("EBS-");
    assertThat(soa.getStatus()).isEqualTo(EbSoa.Status.RECEIVED);
    assertThatThrownBy(() -> receive(p, soaNo.toLowerCase(), realPdf("other.pdf")))
        .extracting("code")
        .isEqualTo("EB_SOA_DUPLICATE");
    assertThatThrownBy(() -> receive(p, "SOA-" + EbFixtures.token(), file))
        .extracting("code")
        .isEqualTo("EB_SOA_DUPLICATE_FILE");
    assertThatThrownBy(
            () ->
                receive(
                    p, "SOA-" + EbFixtures.token(), new UploadedFile("soa.txt", "x".getBytes())))
        .extracting("code")
        .isEqualTo("ATTACHMENT_TYPE_NOT_ALLOWED");
    assertThatThrownBy(
            () ->
                as.run(
                    PROCESSOR,
                    () -> soas.validate(fx.company(), soa.getId(), List.of("INV-NOT-OURS"))))
        .extracting("code")
        .isEqualTo("EB_SOA_INVOICE");

    as.run(PROCESSOR, () -> soas.validate(fx.company(), soa.getId(), null));
    EbSoa released = as.run(PROCESSOR, () -> release.release(fx.company(), soa.getId()));
    assertThat(released.getStatus()).isEqualTo(EbSoa.Status.RELEASED);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = 'EB_SOA' and entity_id = ?",
                Integer.class,
                soa.getId().toString()))
        .isEqualTo(1);

    EbSoa other = receive(p, "SOA-" + EbFixtures.token(), realPdf("second.pdf"));
    assertThatThrownBy(
            () -> as.run(PROCESSOR, () -> soas.reject(fx.company(), other.getId(), null, null)))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
    assertThat(
            as.run(
                    PROCESSOR,
                    () -> soas.reject(fx.company(), other.getId(), "AMOUNT_MISMATCH", "Check"))
                .getStatus())
        .isEqualTo(EbSoa.Status.REJECTED);
    EbSoa again = receive(p, other.getInsurerSoaNo(), realPdf("third.pdf"));
    assertThat(again.getStatus()).isEqualTo(EbSoa.Status.RECEIVED);
  }
}
