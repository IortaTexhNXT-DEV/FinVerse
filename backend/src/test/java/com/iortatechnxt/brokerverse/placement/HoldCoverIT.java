package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverExpiryJob;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.ExpiryRun;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.HoldCoverConfirmation;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.HoldCoverRequest;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService;
import com.iortatechnxt.brokerverse.placement.service.WorkbenchTab;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** Hold cover request, insurer confirmation and expiry monitor (BRNB.072/103). */
@IntegrationTest
class HoldCoverIT {

  @Autowired private HoldCoverService holdCovers;
  @Autowired private HoldCoverExpiryJob job;
  @Autowired private PlacementQueryService queries;
  @Autowired private AccountQueryService accounts;
  @Autowired private MessageService messages;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  @Test
  void aRequestIsSentConfirmedAndMonitoredUntilItExpires() {
    String arn = fx.placed(fx.motor());
    LocalDate start = LocalDate.of(2026, 10, 1);
    HoldCover requested =
        as.run("proc", () -> holdCovers.request(arn, new HoldCoverRequest(start, null)));
    assertThat(requested.getExpiryDate()).isEqualTo(start.plusDays(30));
    Account account = accounts.requireByArn(arn);
    assertThat(account.getLifecycle().getHoldCoverStatus()).isEqualTo(HoldCoverStatus.REQUESTED);
    assertThat(messages.forRecord(AccountService.ENTITY, String.valueOf(account.getId())))
        .anyMatch(m -> m.getPurpose().equals("HOLD_COVER"));
    assertThatThrownBy(
            () ->
                as.run(
                    "proc", () -> holdCovers.request(arn, new HoldCoverRequest(null, List.of()))))
        .extracting("code")
        .isEqualTo("HOLD_COVER_OPEN");
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        holdCovers.confirm(arn, new HoldCoverConfirmation(null, " ", null, null))))
        .extracting("code")
        .isEqualTo("HOLD_COVER_REFERENCE");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        holdCovers.confirm(
                            arn,
                            new HoldCoverConfirmation(
                                null, "HC-1", null, LocalDate.of(2026, 1, 1)))))
        .extracting("code")
        .isEqualTo("HOLD_COVER_DATES");

    HoldCover confirmed =
        as.run(
            "ao",
            () ->
                holdCovers.confirm(
                    arn,
                    new HoldCoverConfirmation(
                        "", "HC-" + PlacementTestData.token(), LocalDate.of(2026, 10, 2), null)));
    assertThat(confirmed.getStatus()).isEqualTo(HoldCoverStatus.CONFIRMED);
    assertThat(accounts.requireByArn(arn).getLifecycle().getHoldCoverRef())
        .isEqualTo(confirmed.getInsurerRef());
    assertThat(queries.holdCover(arn)).isPresent();

    ExpiryRun alert = holdCovers.runExpiry(confirmed.getExpiryDate().minusDays(1));
    assertThat(alert.alerted()).isPositive();
    assertThat(queries.holdCover(arn).orElseThrow().getAlertedOn()).isNotNull();
    assertThat(
            queries.workbench(
                fx.company(), WorkbenchTab.HOLD_COVER_EXPIRING, null, PageRequest.of(0, 50)))
        .isNotNull();
    ExpiryRun expiry = holdCovers.runExpiry(confirmed.getExpiryDate().plusDays(1));
    assertThat(expiry.expired()).isPositive();
    assertThat(queries.holdCover(arn).orElseThrow().getStatus()).isEqualTo(HoldCoverStatus.EXPIRED);
    assertThat(accounts.requireByArn(arn).getLifecycle().getHoldCoverStatus())
        .isEqualTo(HoldCoverStatus.EXPIRED);
    assertThatThrownBy(() -> as.run("proc", () -> holdCovers.decline(arn, null)))
        .extracting("code")
        .isEqualTo("HOLD_COVER_NONE");
    assertThat(job.name()).isEqualTo("HOLD_COVER_EXPIRY");
    assertThat(job.description()).isNotBlank();
    assertThat(job.cron()).isNotBlank();
  }

  @Test
  void confirmationsWithoutRequestAndDeclines() {
    String arn = fx.ready(fx.liability());
    HoldCover recorded =
        as.run(
            "ao",
            () ->
                holdCovers.confirm(arn, new HoldCoverConfirmation("INS-MGIC", "HC-X", null, null)));
    assertThat(recorded.getStatus()).isEqualTo(HoldCoverStatus.CONFIRMED);
    String other = fx.ready(fx.liability());
    as.run(
        "proc",
        () -> holdCovers.request(other, new HoldCoverRequest(null, List.of("uw@insurer.example"))));
    HoldCover declined = as.run("proc", () -> holdCovers.decline(other, "DECL-1"));
    assertThat(declined.getStatus()).isEqualTo(HoldCoverStatus.DECLINED);
    HoldCover later =
        as.run(
            "proc",
            () -> holdCovers.confirm(other, new HoldCoverConfirmation(null, "X", null, null)));
    assertThat(later.getId()).isNotEqualTo(declined.getId());
    assertThat(holdCovers.current(other).orElseThrow().getStatus())
        .isEqualTo(HoldCoverStatus.CONFIRMED);
    String awaiting = fx.awaitingPayment(fx.liability()).getArn();
    assertThatThrownBy(
            () ->
                as.run(
                    "proc", () -> holdCovers.request(awaiting, new HoldCoverRequest(null, null))))
        .extracting("code")
        .isEqualTo("HOLD_COVER_NOT_ALLOWED");
    assertThat(job.execute(LocalDate.of(2026, 9, 24)).message()).contains("expired");
  }
}
