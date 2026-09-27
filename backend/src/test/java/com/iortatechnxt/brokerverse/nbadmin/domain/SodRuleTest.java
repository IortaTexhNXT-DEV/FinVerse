package com.iortatechnxt.brokerverse.nbadmin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule.PendingAction;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A separation-of-duties rule and its maker-checker states. */
class SodRuleTest {

  private static final Instant NOW = Instant.parse("2026-09-27T01:00:00Z");

  private static SodRule rule() {
    return new SodRule("SOD-000001", "UAM_REQUESTOR", "UAM_APPROVER", "Requester and approver");
  }

  @Test
  void aRuleForbidsHoldingBothProfilesInEitherOrder() {
    SodRule r = rule();
    assertThat(r.forbids(Set.of("UAM_REQUESTOR", "UAM_APPROVER", "MKT_AO"))).isTrue();
    assertThat(r.forbids(Set.of("UAM_REQUESTOR", "MKT_AO"))).isFalse();
    assertThat(r.samePair("UAM_APPROVER", "UAM_REQUESTOR")).isTrue();
    assertThat(r.samePair("UAM_APPROVER", "MKT_AO")).isFalse();
  }

  @Test
  void creationAndDeactivationWaitForAnotherUser() {
    SodRule r = rule();
    assertThat(r.isPending()).isTrue();
    assertThat(r.getPendingAction()).isEqualTo(PendingAction.CREATE);
    assertThatThrownBy(() -> r.authorizePending("badmin", "badmin", NOW))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    assertThatThrownBy(r::requestDeactivation).extracting("code").isEqualTo("SOD_RULE_NOT_PENDING");
    r.authorizePending("infosec", "badmin", NOW);
    assertThat(r.getRecordStatus()).isEqualTo(RecordStatus.ACTIVE);
    assertThat(r.isPending()).isFalse();
    assertThatThrownBy(() -> r.authorizePending("infosec", "badmin", NOW))
        .extracting("code")
        .isEqualTo("SOD_RULE_NOT_PENDING");
    assertThatThrownBy(() -> r.rejectPending("infosec", "badmin"))
        .extracting("code")
        .isEqualTo("SOD_RULE_NOT_PENDING");

    r.requestDeactivation();
    assertThat(r.getPendingAction()).isEqualTo(PendingAction.DEACTIVATE);
    r.rejectPending("infosec", "badmin");
    assertThat(r.isActive()).isTrue();
    r.requestDeactivation();
    assertThatThrownBy(() -> r.rejectPending("badmin", "badmin"))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    r.authorizePending("infosec", "badmin", NOW);
    assertThat(r.getRecordStatus()).isEqualTo(RecordStatus.INACTIVE);
  }

  @Test
  void aRejectedNewRuleIsClosed() {
    SodRule r = rule();
    r.rejectPending("infosec", "badmin");
    assertThat(r.getRecordStatus()).isEqualTo(RecordStatus.INACTIVE);
    assertThat(r.isPending()).isFalse();
  }
}
