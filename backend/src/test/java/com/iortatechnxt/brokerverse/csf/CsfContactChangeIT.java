package com.iortatechnxt.brokerverse.csf;

import static com.iortatechnxt.brokerverse.csf.CsfFixtures.AGENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.SUPERVISOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService.ChangeRequest;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService.ReferralRequest;
import com.iortatechnxt.brokerverse.csf.service.CsfRetentionProvider;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.VerificationView;
import com.iortatechnxt.brokerverse.csf.service.LegacySyncJob;
import com.iortatechnxt.brokerverse.csf.service.ServicingViewService;
import com.iortatechnxt.brokerverse.csf.service.VerificationService;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Caller verification, contact changes, refusals and referrals, and the legacy write-back of the
 * Customer Servicing Facility (FR-CSF-020, 021, 022, 040; test plan TC-CSF-020 to 022, 040).
 */
@IntegrationTest
class CsfContactChangeIT {

  @Autowired private CsfFixtures fx;
  @Autowired private VerificationService verifications;
  @Autowired private ContactChangeService changes;
  @Autowired private ServicingViewService views;
  @Autowired private ClientService clients;
  @Autowired private LegacySyncJob syncJob;
  @Autowired private SystemParameterService parameters;
  @Autowired private CsfRetentionProvider retention;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private VerificationView verify(Client c, int matches) {
    return as.run(
        AGENT, () -> verifications.verify(fx.company(), c.getId(), CsfFixtures.checks(matches)));
  }

  private static ChangeRequest change(Long verificationId, Map<String, String> values) {
    return new ChangeRequest(verificationId, "CLIENT_REQUEST", "Called the hotline", values);
  }

  @Test
  void twoMatchesPassAndOneFailsAndTheThirdFailureRaisesAnAlert() {
    Client c = fx.client(true);
    VerificationView passed = verify(c, 2);
    assertThat(passed.result()).isEqualTo("PASSED");
    assertThat(passed.required()).isEqualTo(2);
    assertThat(passed.checks()).hasSize(4);
    assertThat(verifications.current(c.getId())).isPresent();

    Client suspect = fx.client(true);
    for (int i = 0; i < 3; i++) {
      assertThat(verify(suspect, 1).result()).isEqualTo("FAILED");
    }
    assertThat(verifications.current(suspect.getId())).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = ? and entity_id = ?",
                Long.class,
                "CSF_VERIFICATION_FAILED_REPEAT",
                String.valueOf(suspect.getId())))
        .isEqualTo(1L);
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        verifications.verify(
                            fx.company(),
                            c.getId(),
                            new VerificationService.VerifyRequest(
                                "HOTLINE", java.util.List.of(), null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Record the result of the verification checks");
  }

  @Test
  void aVerifiedAgentChangesTheMobileAndTheHistoryShowsBothNumbers() {
    Client c = fx.client(true);
    String before = c.getMobile();
    VerificationView v = verify(c, 3);
    ChangeView done =
        as.run(
            AGENT,
            () ->
                changes.apply(
                    fx.company(), c.getId(), change(v.id(), Map.of("MOBILE", "09175551234"))));
    assertThat(done.status()).isEqualTo("APPLIED");
    assertThat(done.changeNo()).startsWith("CSF-");
    assertThat(done.syncStatus()).isEqualTo("NOT_CONFIGURED");
    assertThat(done.verificationResult()).isEqualTo("PASSED");
    assertThat(done.fields())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.field()).isEqualTo("MOBILE");
              assertThat(f.oldValue()).isEqualTo(before);
              assertThat(f.newValue()).isEqualTo("09175551234");
            });
    assertThat(clients.get(c.getId()).getMobile()).isEqualTo("09175551234");
    assertThat(as.run(AGENT, () -> views.summary(fx.company(), c.getId())).contact().mobile())
        .isEqualTo("09175551234");
    assertThat(as.run(AGENT, () -> changes.history(fx.company(), c.getId())))
        .extracting(ChangeView::changeNo)
        .containsExactly(done.changeNo());
    assertThat(
            jdbc.queryForList(
                "select o.status from csf_sync_outbox o join csf_contact_change c on c.id = o.change_id"
                    + " where c.change_no = ?",
                String.class,
                done.changeNo()))
        .containsExactly("NOT_CONFIGURED", "NOT_CONFIGURED");
    assertThat(
            jdbc.queryForObject(
                "select summary from audit_log where entity_type = 'Client' and entity_id = ?"
                    + " and summary like 'Contact of %' order by id desc limit 1",
                String.class, c.getProspectCode()))
        .contains(before, "09175551234", done.changeNo(), "Client request");
  }

  @Test
  void aChangeNeedsAPassedVerificationOfTheSameClientAndAReason() {
    Client c = fx.client(true);
    Client other = fx.client(true);
    VerificationView failed = verify(c, 0);
    VerificationView otherPass = verify(other, 4);
    Map<String, String> email = Map.of("EMAIL", "new.address@csf-client.ph");
    assertThatThrownBy(
            () -> as.run(AGENT, () -> changes.apply(fx.company(), c.getId(), change(null, email))))
        .hasMessage("Verify the caller before changing the contact details");
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () -> changes.apply(fx.company(), c.getId(), change(failed.id(), email))))
        .hasMessage("The caller could not be verified. The contact details cannot be changed");
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () -> changes.apply(fx.company(), c.getId(), change(otherPass.id(), email))))
        .hasMessage("Verify the caller before changing the contact details");
    VerificationView ok = verify(c, 2);
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        changes.apply(
                            fx.company(),
                            c.getId(),
                            new ChangeRequest(ok.id(), null, null, email))))
        .hasMessage("Select the reason for the change");
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        changes.apply(
                            fx.company(),
                            c.getId(),
                            change(ok.id(), Map.of("EMAIL", "not-an-email")))))
        .hasMessage("Enter a valid e-mail address");
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        changes.apply(
                            fx.company(),
                            c.getId(),
                            change(ok.id(), Map.of("MOBILE", c.getMobile())))))
        .hasMessage("Change at least one contact detail");
  }

  @Test
  void aCivilStatusChangeIsRefusedNamingTheFulfilmentUnitAndKeptAsRefused() {
    Client c = fx.client(true);
    VerificationView v = verify(c, 4);
    Map<String, String> values = new LinkedHashMap<>();
    values.put("CIVIL_STATUS", "MARRIED");
    values.put("PHONE", null);
    assertThatThrownBy(
            () ->
                as.run(AGENT, () -> changes.apply(fx.company(), c.getId(), change(v.id(), values))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Civil status cannot be changed here. Refer the client to the fulfilment unit");
    assertThat(as.run(AGENT, () -> changes.history(fx.company(), c.getId())))
        .singleElement()
        .satisfies(
            h -> {
              assertThat(h.status()).isEqualTo("REFUSED");
              assertThat(h.syncStatus()).isEqualTo("NOT_REQUIRED");
              assertThat(h.fields()).extracting("field").containsExactly("CIVIL_STATUS");
            });
  }

  @Test
  void aReferralIsHandedToTheFulfilmentUnit() {
    Client c = fx.client(true);
    ChangeView referral =
        as.run(
            AGENT,
            () ->
                changes.refer(
                    fx.company(),
                    c.getId(),
                    new ReferralRequest(
                        "HOTLINE", Map.of("NAME", "Maria Santos-Reyes"), "Married last month")));
    assertThat(referral.status()).isEqualTo("REFERRED");
    assertThat(referral.handoffStatus()).isEqualTo("OPEN");
    assertThat(
            jdbc.queryForObject(
                "select summary from ops_handoff where port = 'CSF_FULFILMENT_REFERRAL' and source_ref = ?",
                String.class,
                referral.changeNo()))
        .contains("Name", c.getCode(), referral.changeNo());
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        changes.refer(
                            fx.company(),
                            c.getId(),
                            new ReferralRequest("HOTLINE", Map.of(), null))))
        .hasMessage("Select the information the client wants to change");
  }

  @Test
  void withTheSyncOnTheJobTriesTheQueuedChangesAndAlertsOnAFailure() {
    Client c = fx.client(true);
    VerificationView v = verify(c, 2);
    JobOutcome off = syncJob.execute(LocalDate.of(2026, 9, 28));
    assertThat(off.itemsProcessed()).isZero();
    parameters.update("CSF_LEGACY_SYNC_ENABLED", "true");
    try {
      ChangeView done =
          as.run(
              SUPERVISOR,
              () ->
                  changes.apply(
                      fx.company(), c.getId(), change(v.id(), Map.of("PHONE", "(02) 8888-1234"))));
      assertThat(done.syncStatus()).isEqualTo("QUEUED");
      JobOutcome run = syncJob.execute(LocalDate.of(2026, 9, 28));
      assertThat(run.message()).contains("failed");
      assertThat(as.run(AGENT, () -> changes.history(fx.company(), c.getId())).get(0).syncStatus())
          .isEqualTo("FAILED");
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from alt_alert where exception_code = 'CSF_SYNC_FAILED'"
                      + " and entity_id = ?",
                  Long.class,
                  done.changeNo()))
          .isPositive();
    } finally {
      parameters.update("CSF_LEGACY_SYNC_ENABLED", "false");
    }
    assertThat(
            retention.countEligible(
                new RetentionCriteria(Set.of("APPLIED", "REFUSED"), LocalDate.of(2099, 1, 1))))
        .isPositive();
    assertThat(
            retention.eligible(
                new RetentionCriteria(Set.of("APPLIED"), LocalDate.of(2099, 1, 1)), 5))
        .isNotEmpty();
  }
}
