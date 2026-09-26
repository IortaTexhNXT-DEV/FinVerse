package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.SettlementAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.StatusAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.ValueAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.ClaimsSetupApprovalSource;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.HandlerSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.MatrixSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccess;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService.StatusOption;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Claims Setup of wave CL1-B (BRCLM.010/012/014, FR-CL-040/041/043): status and settlement type
 * attributes and matrix rows take effect only after another user authorizes them; the phase, the
 * follow-up days and the outcome are validated; the handler register gives the unit.
 */
@IntegrationTest
class ClaimsSetupIT {

  private static final String UH = "clmuh";
  private static final String CHECKER = "approver";

  @Autowired private AttributeSetupService attributes;
  @Autowired private MatrixSetupService matrix;
  @Autowired private HandlerSetupService handlers;
  @Autowired private ClaimsSetupApprovalSource approvals;
  @Autowired private ClaimStatusService statuses;
  @Autowired private ClaimClosureService closures;
  @Autowired private LovService lovs;
  @Autowired private BrokerClaimFixtures fixtures;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private final List<Long> created = new ArrayList<>();

  private String newValue(String type, String label) {
    String code = "T_" + BrokerClaimFixtures.unique();
    Long id =
        as.run(
                UH,
                () ->
                    lovs.create(
                        type,
                        code,
                        new LovDetails(label, 900, null, LocalDate.of(2020, 1, 1), null)))
            .getId();
    as.run(CHECKER, () -> lovs.authorize(id));
    created.add(id);
    return code;
  }

  /**
   * Leaves the seeded lists as delivered (other tests count the 18 statuses, the 10 types and their
   * attributes): the test values are deactivated, their attributes and test matrix rows removed.
   */
  @AfterEach
  void removeTestValues() {
    for (Long id : created) {
      String code =
          jdbc.queryForObject("select code from lov_value where id = ?", String.class, id);
      jdbc.update("delete from bcl_lov_attribute where code = ?", code);
      jdbc.update("delete from bcl_status_access where status_code = ?", code);
      as.run(UH, () -> lovs.deactivate(id));
    }
    created.clear();
  }

  @Test
  void theSeededAttributesAreListed() {
    assertThat(as.run(UH, () -> attributes.list(ClaimCodes.LOV_STATUS)))
        .filteredOn(v -> v.code().equals("BDOI_PREMIUM_REMITTANCE"))
        .singleElement()
        .satisfies(
            v -> {
              assertThat(v.attributes()).containsEntry("phase", "IN_PROGRESS");
              assertThat(v.attributes()).containsEntry("awaiting_premium_remittance", "true");
            });
    assertThat(as.run(UH, () -> attributes.list(ClaimCodes.LOV_SETTLEMENT_TYPE)))
        .hasSizeGreaterThanOrEqualTo(10);
    assertThatThrownBy(() -> as.run(UH, () -> attributes.list("BCL_ADJUSTER")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void aNewStatusIsUsableOnlyWithAPhaseAnAuthorizationAndAMatrixRow() {
    String status = newValue(ClaimCodes.LOV_STATUS, "With Insurer - For Reinspection");
    assertThatThrownBy(
            () ->
                as.run(
                    UH,
                    () ->
                        attributes.proposeStatus(
                            status, new StatusAttributes(null, "INSURER", "", false))))
        .hasMessage("Set the phase of the status");
    for (String days : new String[] {"366", "2.5", "0"}) {
      assertThatThrownBy(
              () ->
                  as.run(
                      UH,
                      () ->
                          attributes.proposeStatus(
                              status, new StatusAttributes("IN_PROGRESS", "INSURER", days, false))))
          .hasMessage("Enter a whole number of days");
    }
    ValueAttributes pending =
        as.run(
            UH,
            () ->
                attributes.proposeStatus(
                    status, new StatusAttributes("in_progress", "insurer", "5", false)));
    assertThat(pending.pending())
        .containsEntry("phase", "IN_PROGRESS")
        .containsEntry("follow_up_days", "5");
    assertThat(pending.pendingBy()).isEqualTo(UH);
    assertThat(pending.attributes()).isEmpty();
    assertThat(approvals.pendingFor(ApprovalViewer.system()))
        .anyMatch(a -> a.reference().equals(ClaimCodes.LOV_STATUS + ":" + status));
    assertThatThrownBy(() -> as.run(UH, () -> attributes.authorize(ClaimCodes.LOV_STATUS, status)))
        .isInstanceOf(BusinessRuleException.class);
    ValueAttributes applied =
        as.run(CHECKER, () -> attributes.authorize(ClaimCodes.LOV_STATUS, status));
    assertThat(applied.attributes())
        .containsEntry("phase", "IN_PROGRESS")
        .containsEntry("follow_up_days", "5");
    assertThat(applied.pending()).isEmpty();

    StatusAccess row = as.run(UH, () -> matrix.add(status, "CLM_OFFICER", "NON_MOTOR_HO"));
    assertThat(row.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(() -> as.run(UH, () -> matrix.add(status, "CLM_OFFICER", "NON_MOTOR_HO")))
        .hasMessage("The matrix already has this status, role and unit");
    Long company = fixtures.company();
    Long claim =
        fixtures.recorded(
            fixtures.spec("clmofficer2", BrokerClaimFixtures.today().minusDays(3)),
            "NEW_COMPLETE_DOCS");
    assertThat(as.run("clmofficer2", () -> statuses.allowedStatuses(company, claim)))
        .extracting(StatusOption::code)
        .doesNotContain(status);
    as.run(CHECKER, () -> matrix.authorize(row.getId()));
    assertThat(as.run("clmofficer2", () -> statuses.allowedStatuses(company, claim)))
        .extracting(StatusOption::code)
        .contains(status);
    Long motorClaim =
        fixtures.recorded(
            fixtures.spec("clmofficer", BrokerClaimFixtures.today().minusDays(3)),
            "NEW_COMPLETE_DOCS");
    assertThat(as.run("clmofficer", () -> statuses.allowedStatuses(company, motorClaim)))
        .extracting(StatusOption::code)
        .doesNotContain(status);

    as.run("clmofficer2", () -> statuses.change(company, claim, status, null));
    assertThat(fixtures.column(claim, "next_follow_up_date", LocalDate.class))
        .isEqualTo(BrokerClaimFixtures.today().plusDays(5));
    as.run(UH, () -> matrix.deactivate(row.getId()));
    assertThat(as.run(UH, () -> matrix.roles()))
        .extracting(r -> r.get("code"))
        .contains("CLM_OFFICER", "CLM_TL", "CLM_TH");
  }

  @Test
  void settlementTypeAttributesChangeWithoutABuild() {
    String type = newValue(ClaimCodes.LOV_SETTLEMENT_TYPE, "Settled - Test Repair");
    assertThatThrownBy(
            () ->
                as.run(
                    UH,
                    () ->
                        attributes.proposeSettlement(
                            type, new SettlementAttributes(" ", true, true))))
        .hasMessage("Set the outcome of the settlement type");
    as.run(
        UH,
        () -> attributes.proposeSettlement(type, new SettlementAttributes("SETTLED", true, true)));
    as.run(CHECKER, () -> attributes.authorize(ClaimCodes.LOV_SETTLEMENT_TYPE, type));
    assertThatThrownBy(
            () ->
                as.run(
                    UH,
                    () ->
                        attributes.proposeSettlement(
                            type, new SettlementAttributes("SETTLED", true, true))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        UH,
        () -> attributes.proposeSettlement(type, new SettlementAttributes("SETTLED", false, true)));
    as.run(UH, () -> attributes.reject(ClaimCodes.LOV_SETTLEMENT_TYPE, type));
    as.run(
        UH,
        () -> attributes.proposeSettlement(type, new SettlementAttributes("SETTLED", false, true)));
    as.run(CHECKER, () -> attributes.authorize(ClaimCodes.LOV_SETTLEMENT_TYPE, type));

    Long company = fixtures.company();
    Long claim =
        fixtures.recorded(
            fixtures.spec("clmofficer", BrokerClaimFixtures.today().minusDays(2)),
            "NEW_COMPLETE_DOCS");
    as.run(
        "clmtl",
        () ->
            closures.settle(
                company,
                claim,
                new ClaimClosureService.Settlement(
                    type, new BigDecimal("500"), BrokerClaimFixtures.today(), null)));
    assertThat(fixtures.column(claim, "phase", String.class)).isEqualTo("NEW");
    assertThat(fixtures.column(claim, "settlement_type_code", String.class)).isEqualTo(type);
  }

  @Test
  void theHandlerRegisterAndTheClaimsLists() {
    assertThat(as.run(UH, () -> handlers.handlers()))
        .extracting(h -> h.getUsername())
        .contains("clmofficer", "clmtl");
    assertThat(as.run(UH, () -> handlers.claimsUsers())).contains("clmrisk", "clmofficer");
    assertThatThrownBy(() -> as.run(UH, () -> handlers.save("clmrisk", "", null, true)))
        .hasMessage("Select the claims unit");
    assertThatThrownBy(() -> as.run(UH, () -> handlers.save("ao", "MOTOR_HO", null, true)))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(
            as.run(UH, () -> handlers.save("clmbranch", "BRANCH_CEBU", "Cebu Claims", true))
                .getTeam())
        .isEqualTo("Cebu Claims");
    assertThat(as.run(UH, () -> handlers.claimsLists()))
        .extracting(t -> t.getCode())
        .contains(ClaimCodes.LOV_STATUS, ClaimCodes.LOV_ADJUSTER, ClaimCodes.LOV_CATASTROPHE)
        .allMatch(c -> c.startsWith("BCL_"));
  }
}
