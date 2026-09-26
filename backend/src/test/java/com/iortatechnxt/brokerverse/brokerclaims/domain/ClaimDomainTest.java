package com.iortatechnxt.brokerverse.brokerclaims.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The claim identity, phases and the status change event (BRCLM.010/035, CL0 contracts). */
class ClaimDomainTest {

  private static final Instant AT = Instant.parse("2026-09-26T01:00:00Z");

  @Test
  void aNewClaimStartsInPhaseNewWithoutStatus() {
    Claim claim =
        new Claim(
            1L,
            "BCL-2026-000001",
            new Claim.Origin(ClaimSource.BDOI_NOTICE, "clmofficer", "MOTOR_HO", 2L, null),
            new CoverSnapshot(),
            new LossDetails());
    assertThat(claim.getProgress().getPhase()).isEqualTo(ClaimPhase.NEW);
    assertThat(claim.getProgress().getStatusCode()).isNull();
    assertThat(claim.isClosed()).isFalse();
    assertThat(claim.getCompanyId()).isEqualTo(1L);
    assertThat(claim.getClaimNo()).isEqualTo("BCL-2026-000001");
    assertThat(claim.getSource()).isEqualTo(ClaimSource.BDOI_NOTICE);
    assertThat(claim.getBranchId()).isEqualTo(2L);
    assertThat(claim.getLegacyRef()).isNull();
    assertThat(claim.getCover()).isNotNull();
    assertThat(claim.getLoss()).isNotNull();

    claim.assignTo("clmbranch", "BRANCH_CEBU");
    assertThat(claim.getHandler()).isEqualTo("clmbranch");
    assertThat(claim.getUnitCode()).isEqualTo("BRANCH_CEBU");
  }

  @Test
  void onlyClosedClaimsAreNotOutstanding() {
    assertThat(ClaimPhase.NEW.isOutstanding()).isTrue();
    assertThat(ClaimPhase.IN_PROGRESS.isOutstanding()).isTrue();
    assertThat(ClaimPhase.TEMP_CLOSED.isOutstanding()).isTrue();
    assertThat(ClaimPhase.CLOSED.isOutstanding()).isFalse();
    assertThat(ClaimPhase.TEMP_CLOSED.stageCode()).isEqualTo("TEMP_CLOSED");
  }

  @Test
  void statusChangedEventTellsPhaseAndStatusEntries() {
    ClaimStatusChanged first =
        new ClaimStatusChanged(
            7L, 1L, "BCL-1", null, "NEW_COMPLETE_DOCS", null, ClaimPhase.NEW, "clmofficer", AT);
    assertThat(first.phaseChanged()).isTrue();
    assertThat(first.entered(ClaimPhase.NEW)).isTrue();
    assertThat(first.enteredStatus("NEW_COMPLETE_DOCS")).isTrue();

    ClaimStatusChanged remit =
        new ClaimStatusChanged(
            7L,
            1L,
            "BCL-1",
            "INSURER_REVIEW",
            "BDOI_PREMIUM_REMITTANCE",
            ClaimPhase.IN_PROGRESS,
            ClaimPhase.IN_PROGRESS,
            "clmtl",
            AT);
    assertThat(remit.phaseChanged()).isFalse();
    assertThat(remit.entered(ClaimPhase.IN_PROGRESS)).isFalse();
    assertThat(remit.enteredStatus("BDOI_PREMIUM_REMITTANCE")).isTrue();
    assertThat(remit.enteredStatus("INSURER_REVIEW")).isFalse();
  }

  @Test
  void lovAttributesReadFlags() {
    ClaimLovAttribute flag =
        new ClaimLovAttribute(
            ClaimCodes.LOV_STATUS,
            "BDOI_PREMIUM_REMITTANCE",
            ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE,
            "TRUE");
    assertThat(flag.isTrue()).isTrue();
    flag.changeValue("false");
    assertThat(flag.isTrue()).isFalse();
    assertThat(flag.getTypeCode()).isEqualTo(ClaimCodes.LOV_STATUS);
    assertThat(flag.getCode()).isEqualTo("BDOI_PREMIUM_REMITTANCE");
    assertThat(flag.getAttribute()).isEqualTo(ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE);
    assertThat(flag.getValue()).isEqualTo("false");
  }
}
