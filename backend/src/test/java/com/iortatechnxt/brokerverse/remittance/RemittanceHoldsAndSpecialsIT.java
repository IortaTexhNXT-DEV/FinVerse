package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.HoldExpiryJob;
import com.iortatechnxt.brokerverse.remittance.service.HoldService;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService.NewRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Marketing holds with approval, extension and expiry, and special remittances (MKTID.002-009). */
@IntegrationTest
class RemittanceHoldsAndSpecialsIT {

  private static final String MKTCOLL = "mktcoll";
  private static final String MKTTL = "mkttl";
  private static final String REMITTL = "remittl";

  @Autowired private RemittanceFixtures fx;
  @Autowired private HoldService holds;
  @Autowired private HoldExpiryJob expiry;
  @Autowired private SpecialRemittanceService specials;
  @Autowired private BatchService batches;
  @Autowired private InvoiceTagRepository tags;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private HoldRequest hold(OpsInvoice invoice, LocalDate until) {
    return as.run(
        MKTCOLL,
        () ->
            holds.create(
                fx.company(),
                invoice.getInvoiceNo(),
                new Terms("OTHERS", "Client dispute", until),
                true,
                RequestSource.SCREEN));
  }

  @Test
  void anApprovedHoldKeepsTheInvoiceOutOfExtractionUntilItExpires() {
    OpsInvoice paid = fx.paidInvoice();
    HoldRequest requested = hold(paid, LocalDate.now().plusDays(10));
    assertThat(requested.getRequestNo()).startsWith("HLD-");
    assertThat(requested.getStage()).isEqualTo(HoldStage.FOR_APPROVAL);
    assertThat(fx.reload(paid.getInvoiceNo()).getRemittanceStatus())
        .isEqualTo(RemittanceStatus.REQUESTED_FOR_HOLD);
    assertThatThrownBy(() -> hold(paid, LocalDate.now().plusDays(5)))
        .isInstanceOf(BusinessRuleException.class);
    Long id = requested.getId();
    assertThatThrownBy(() -> as.run(MKTCOLL, () -> holds.approve(id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("another user");

    HoldRequest active = as.run(MKTTL, () -> holds.approve(id, "OK"));
    assertThat(active.getStage()).isEqualTo(HoldStage.ACTIVE);
    OpsInvoice held = fx.reload(paid.getInvoiceNo());
    assertThat(held.isHoldFlag()).isTrue();
    assertThat(held.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);

    fx.extract(paid.getInvoiceNo());
    assertThat(fx.lineOf(paid.getInvoiceNo())).isNull();
    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(paid.getInvoiceNo()))
        .hasValueSatisfying(
            t -> {
              assertThat(t.getTag()).isEqualTo(ExtractionTag.UNEXTRACTED_DUE);
              assertThat(t.getReasons()).contains("ON_HOLD");
            });

    assertThat(as.run(MKTTL, () -> holds.assign(id, "remit")).getAssignedProcessor())
        .isEqualTo("remit");
    assertThatThrownBy(() -> as.run(MKTTL, () -> holds.assign(id, "ao")))
        .isInstanceOf(BusinessRuleException.class);
    LocalDate later = LocalDate.now().plusDays(20);
    as.run(MKTCOLL, () -> holds.extend(id, later, "Still disputed"));
    HoldRequest extended = as.run(MKTTL, () -> holds.decideExtension(id, true, "OK"));
    assertThat(extended.getHoldUntil()).isEqualTo(later);
    assertThat(extended.getExtensionCount()).isEqualTo(1);
    assertThat(extended.getStage()).isEqualTo(HoldStage.ACTIVE);

    jdbc.update(
        "update rem_hold_request set hold_until = ? where id = ?",
        LocalDate.now().minusDays(1),
        id);
    expiry.execute(LocalDate.now());
    assertThat(as.run(MKTTL, () -> holds.get(id)).getStage()).isEqualTo(HoldStage.RELEASED);
    assertThat(fx.reload(paid.getInvoiceNo()).isHoldFlag()).isFalse();
    fx.extract(paid.getInvoiceNo());
    assertThat(fx.lineOf(paid.getInvoiceNo())).isNotNull();
  }

  @Test
  void holdsAreRejectedCancelledReleasedAndWarnedBeforeExpiry() {
    OpsInvoice first = fx.paidInvoice();
    Long rejected = hold(first, LocalDate.now().plusDays(3)).getId();
    assertThat(as.run(MKTTL, () -> holds.reject(rejected, "Not needed")).getStage())
        .isEqualTo(HoldStage.REJECTED);
    assertThat(fx.reload(first.getInvoiceNo()).getRemittanceStatus())
        .isEqualTo(RemittanceStatus.UNPROCESSED);

    Long draft =
        as.run(
                MKTCOLL,
                () ->
                    holds.create(
                        fx.company(),
                        first.getInvoiceNo(),
                        new Terms("OTHERS", null, LocalDate.now().plusDays(2)),
                        false,
                        RequestSource.SCREEN))
            .getId();
    as.run(
        MKTCOLL,
        () -> holds.update(draft, new Terms("OTHERS", "Changed", LocalDate.now().plusDays(4))));
    assertThat(as.run(MKTCOLL, () -> holds.cancel(draft)).getStage())
        .isEqualTo(HoldStage.CANCELLED);
    assertThatThrownBy(
            () ->
                as.run(
                    MKTCOLL,
                    () ->
                        holds.create(
                            fx.company(),
                            first.getInvoiceNo(),
                            new Terms("OTHERS", null, LocalDate.now()),
                            false,
                            RequestSource.SCREEN)))
        .isInstanceOf(BusinessRuleException.class);

    OpsInvoice second = fx.paidInvoice();
    Long cancelled = hold(second, LocalDate.now().plusDays(1)).getId();
    as.run(MKTTL, () -> holds.approve(cancelled, null));
    expiry.execute(LocalDate.now());
    assertThat(as.run(MKTTL, () -> holds.get(cancelled)).isExpiryNotified()).isTrue();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and entity_id = ?",
                Integer.class,
                MKTCOLL,
                cancelled.toString()))
        .isPositive();
    as.run(MKTCOLL, () -> holds.requestCancel(cancelled, "Resolved"));
    assertThat(as.run(MKTTL, () -> holds.decideCancel(cancelled, true, null)).getStage())
        .isEqualTo(HoldStage.RELEASED);
    assertThat(fx.reload(second.getInvoiceNo()).isHoldFlag()).isFalse();

    OpsInvoice third = fx.paidInvoice();
    Long released = hold(third, LocalDate.now().plusDays(9)).getId();
    as.run(MKTTL, () -> holds.approve(released, null));
    as.run(MKTCOLL, () -> holds.extend(released, LocalDate.now().plusDays(12), null));
    as.run(MKTTL, () -> holds.decideExtension(released, false, "No"));
    assertThat(as.run(MKTTL, () -> holds.get(released)).getExtensionCount()).isZero();
    assertThat(as.run(MKTCOLL, () -> holds.release(released, "Paid")).getStage())
        .isEqualTo(HoldStage.RELEASED);
    assertThat(holds.ofInvoice(third.getInvoiceNo())).hasSize(1);
  }

  @Test
  void aSpecialRemittanceIsValidatedApprovedAndPushedThroughItsOwnBatch() {
    OpsInvoice unpaid = fx.invoice();
    assertThatThrownBy(
            () ->
                as.run(
                    MKTCOLL,
                    () ->
                        specials.request(
                            fx.company(),
                            new NewRequest(unpaid.getInvoiceNo(), "RENEWAL", null),
                            RequestSource.SCREEN)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be remitted specially");

    OpsInvoice paid = fx.paidInvoice();
    SpecialRemittance request =
        as.run(
            MKTCOLL,
            () ->
                specials.request(
                    fx.company(),
                    new NewRequest(paid.getInvoiceNo(), "INSTALLMENT_DUE", "Installment due"),
                    RequestSource.SCREEN));
    assertThat(request.getRequestNo()).startsWith("SPR-");
    assertThat(request.getStage()).isEqualTo(SpecialStage.FOR_APPROVAL);
    assertThat(request.getValidationNote()).startsWith("Validated");
    Long id = request.getId();
    assertThatThrownBy(
            () ->
                as.run(
                    MKTCOLL,
                    () ->
                        specials.request(
                            fx.company(),
                            new NewRequest(paid.getInvoiceNo(), "RENEWAL", null),
                            RequestSource.SCREEN)))
        .isInstanceOf(BusinessRuleException.class);

    SpecialRemittance approved = as.run(REMITTL, () -> specials.approve(id, "Urgent"));
    assertThat(approved.getStage()).isEqualTo(SpecialStage.IN_PROCESS_REMITTANCE);
    RemittanceBatch batch = fx.batchOf(paid.getInvoiceNo());
    assertThat(batch.getBatchNo()).isEqualTo(approved.getBatchNo());
    assertThat(batch.getRemittanceType()).isEqualTo(RemittanceType.SPECIAL);
    assertThat(batch.getSpecialRequestNo()).isEqualTo(approved.getRequestNo());

    as.run("remit", () -> batches.submit(batch.getId(), null));
    as.run(REMITTL, () -> batches.approve(batch.getId(), null));
    SpecialRemittance pushed = as.run(REMITTL, () -> specials.get(id));
    assertThat(pushed.getStage()).isEqualTo(SpecialStage.PUSHED_TO_DISBURSEMENT);
    assertThat(pushed.getPushedAt()).isNotNull();
    assertThat(fx.batchOf(paid.getInvoiceNo()).getStage()).isEqualTo(BatchStage.APPROVED);
  }

  @Test
  void aRejectedOrReturnedSpecialRemittanceEnds() {
    OpsInvoice paid = fx.paidInvoice();
    Long rejected =
        as.run(
                "mkttl",
                () ->
                    specials.request(
                        fx.company(),
                        new NewRequest(paid.getInvoiceNo(), "IMMEDIATE_OR", null),
                        RequestSource.SCREEN))
            .getId();
    assertThat(as.run(REMITTL, () -> specials.reject(rejected, "OTHERS", "No basis")).getStage())
        .isEqualTo(SpecialStage.REJECTED);

    Long returned =
        as.run(
                MKTCOLL,
                () ->
                    specials.request(
                        fx.company(),
                        new NewRequest(paid.getInvoiceNo(), "INSTALLMENT_DUE", null),
                        RequestSource.SCREEN))
            .getId();
    SpecialRemittance approved = as.run(REMITTL, () -> specials.approve(returned, null));
    RemittanceBatch batch = fx.batchOf(paid.getInvoiceNo());
    as.run("remit", () -> batches.returnBatch(batch.getId(), "OTHERS", null));
    assertThat(as.run(REMITTL, () -> specials.get(returned)).getStage())
        .isEqualTo(SpecialStage.RETURNED);
    assertThat(specials.ofInvoice(paid.getInvoiceNo())).hasSize(2);
    assertThat(approved.getBatchNo()).isEqualTo(batch.getBatchNo());
  }
}
