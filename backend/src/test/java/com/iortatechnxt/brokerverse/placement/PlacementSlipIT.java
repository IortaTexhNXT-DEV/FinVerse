package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.placement.domain.InsurerReturn;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import com.iortatechnxt.brokerverse.placement.service.InsurerDirectory;
import com.iortatechnxt.brokerverse.placement.service.InsurerReturnService;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService.ItemResult;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService.Readiness;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService.SlipEmail;
import com.iortatechnxt.brokerverse.placement.service.SlipPrerequisites.Unmet;
import com.iortatechnxt.brokerverse.placement.service.WorkbenchTab;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** Placement slips, sending, insurer returns, cancel and reactivate (BRNB.033/034/062/069/071). */
@IntegrationTest
class PlacementSlipIT {

  @Autowired private PlacementSlipService slips;
  @Autowired private PlacementBatchService batch;
  @Autowired private PlacementQueryService queries;
  @Autowired private InsurerReturnService returns;
  @Autowired private InsurerDirectory insurers;
  @Autowired private MessageService messages;
  @Autowired private AccountQueryService accounts;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  private AccountStatus status(String arn) {
    return accounts.requireByArn(arn).getStatus();
  }

  @Test
  void slipsNeedEveryPrerequisiteAndGroupAccountsByInsurerBranch() {
    String awaiting = fx.awaitingPayment(fx.liability()).getArn();
    List<Readiness> readiness = slips.readiness(fx.company(), List.of(awaiting));
    assertThat(readiness.get(0).unmet())
        .extracting(Unmet::code)
        .contains("NOT_READY_FOR_PLACEMENT", "PAYMENT_NOT_CONFIRMED");
    assertThatThrownBy(() -> as.run("proc", () -> slips.generate(fx.company(), List.of(awaiting))))
        .extracting("code")
        .isEqualTo("SLIP_PREREQUISITES_UNMET");
    assertThatThrownBy(() -> as.run("proc", () -> slips.generate(fx.company(), List.of())))
        .extracting("code")
        .isEqualTo("SLIP_NO_ACCOUNTS");
    assertThatThrownBy(() -> insurers.address(fx.company(), null, null))
        .extracting("code")
        .isEqualTo("INSURER_NOT_SET");

    String one = fx.ready(fx.fire());
    String two = fx.ready(fx.motor());
    assertThat(slips.readiness(fx.company(), List.of(one, two))).allMatch(r -> r.unmet().isEmpty());
    List<PlacementSlip> generated =
        as.run("proc", () -> slips.generate(fx.company(), List.of(one, two, one)));
    assertThat(generated).hasSize(1);
    PlacementSlip slip = generated.get(0);
    assertThat(slip.getSlipNo()).startsWith("PL-");
    assertThat(slip.getAccounts()).hasSize(2);
    assertThat(slip.getStatus()).isEqualTo(SlipStatus.GENERATED);
    assertThat(
            as.run("proc", () -> slips.file(slip.getId(), PlacementSlipService.PDF).getContent()))
        .startsWith((byte) '%');
    assertThat(
            as.run("proc", () -> slips.file(slip.getId(), PlacementSlipService.XLSX).getFileName()))
        .endsWith(".xlsx");

    SlipEmail draft = slips.draft(slip.getId());
    assertThat(draft.to()).containsExactly("uw.makati@mabuhaygeneral.example");
    assertThat(draft.protect()).isTrue();
    as.run("proc", () -> slips.send(slip.getId(), draft));
    assertThat(status(one)).isEqualTo(AccountStatus.PLACED);
    assertThat(accounts.requireByArn(two).getLifecycle().getPlacementSlipRef())
        .isEqualTo(slip.getSlipNo());
    assertThat(messages.forRecord(PlacementSlipService.ENTITY, String.valueOf(slip.getId())))
        .hasSize(2);
    PlacementSlip resent =
        as.run(
            "proc",
            () ->
                slips.send(
                    slip.getId(), new SlipEmail(draft.to(), List.of(), "Resend", "Body", false)));
    assertThat(resent.getSendCount()).isEqualTo(2);
    assertThat(queries.slipsFor(one)).hasSize(1);
    assertThat(queries.currentSlip(two)).isPresent();
    assertThat(slips.search(fx.company(), SlipStatus.SENT, PageRequest.of(0, 5))).isNotEmpty();
    assertThat(slips.search(fx.company(), null, PageRequest.of(0, 5))).isNotEmpty();
  }

  @Test
  void anInsurerReturnIsResubmittedWithANewSlipVersion() {
    String arn = fx.placed(fx.motor());
    PlacementSlip first = queries.currentSlip(arn).orElseThrow();
    InsurerReturn returned =
        as.run("proc", () -> returns.recordReturn(arn, "INSURER_REQUIREMENTS", "Need photos"));
    assertThat(returned.getSlipNo()).isEqualTo(first.displayNo());
    assertThat(status(arn)).isEqualTo(AccountStatus.RETURNED_BY_INSURER);
    assertThat(queries.workbench(fx.company(), WorkbenchTab.RETURNED, arn, PageRequest.of(0, 5)))
        .hasSize(1);
    assertThatThrownBy(() -> as.run("proc", () -> slips.regenerate(first.getId())))
        .extracting("code")
        .isEqualTo("SLIP_PREREQUISITES_UNMET");

    as.run("proc", () -> returns.resubmit(arn, "Photos attached"));
    assertThat(status(arn)).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(returns.returnsOf(arn).get(0).getResolution()).isEqualTo("resubmit");
    PlacementSlip second = as.run("proc", () -> slips.regenerate(first.getId()));
    assertThat(second.getSlipNo()).isEqualTo(first.getSlipNo());
    assertThat(second.getVersionNo()).isEqualTo(2);
    assertThat(second.displayNo()).endsWith(" v2");
    assertThat(slips.get(first.getId()).getStatus()).isEqualTo(SlipStatus.SUPERSEDED);
    assertThatThrownBy(() -> as.run("proc", () -> slips.regenerate(first.getId())))
        .extracting("code")
        .isEqualTo("SLIP_SUPERSEDED");
    assertThatThrownBy(
            () -> as.run("proc", () -> slips.send(first.getId(), slips.draft(first.getId()))))
        .extracting("code")
        .isEqualTo("SLIP_SUPERSEDED");

    List<ItemResult> sent = as.run("proc", () -> batch.sendSlips(List.of(arn, "ARN-1999-000001")));
    assertThat(sent).extracting(ItemResult::ok).containsExactlyInAnyOrder(true, false);
    assertThat(status(arn)).isEqualTo(AccountStatus.PLACED);
    assertThat(queries.slipsFor(arn)).hasSize(2);
    assertThat(queries.currentSlip(arn).orElseThrow().getVersionNo()).isEqualTo(2);
  }

  @Test
  void placementsAreCancelledAndReactivatedInBulk() {
    String ready = fx.ready(fx.liability());
    String placed = fx.placed(fx.liability());
    String awaiting = fx.awaitingPayment(fx.liability()).getArn();
    List<ItemResult> cancelled =
        as.run(
            "proc",
            () -> batch.cancel(List.of(ready, placed, awaiting), "CLIENT_REQUEST", "On hold"));
    assertThat(cancelled).extracting(ItemResult::ok).containsExactly(true, true, false);
    assertThat(status(ready)).isEqualTo(AccountStatus.PLACEMENT_CANCELLED);
    assertThat(status(placed)).isEqualTo(AccountStatus.PLACEMENT_CANCELLED);
    assertThat(queries.counts(fx.company()).placementCancelled()).isPositive();
    assertThatThrownBy(() -> as.run("proc", () -> batch.cancel(List.of(), "CLIENT_REQUEST", null)))
        .extracting("code")
        .isEqualTo("PLACEMENT_NO_ACCOUNTS");

    List<ItemResult> reactivated =
        as.run("ao", () -> batch.reactivate(List.of(ready, placed), "Client confirmed"));
    assertThat(reactivated).allMatch(ItemResult::ok);
    assertThat(status(placed)).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(
            queries.workbench(
                fx.company(), WorkbenchTab.FOR_PLACEMENT, placed, PageRequest.of(0, 5)))
        .extracting(r -> r.account().getArn())
        .containsExactly(placed);
  }
}
