package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.payables.domain.NotificationFormat;
import com.iortatechnxt.finverse.payables.domain.PaymentCategory;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.payables.domain.VoucherStatus;
import com.iortatechnxt.finverse.payables.service.PaymentApprovalService;
import com.iortatechnxt.finverse.payables.service.PaymentCommand;
import com.iortatechnxt.finverse.payables.service.PaymentNotificationService;
import com.iortatechnxt.finverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
class PaymentVoucherIT {

  private static final LocalDate PAY_DATE = LocalDate.of(2026, 9, 10);

  @Autowired private PaymentVoucherService vouchers;
  @Autowired private PaymentApprovalService approvals;
  @Autowired private PaymentNotificationService notifications;
  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private PayablesFixtures fx;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  @Test
  void chequePaymentPostsMatchesAndCanBeVoidedWithReinstatement() {
    SupplierInvoice a = fx.approvedInvoice("G-0001", "1000.00", PayablesFixtures.DATE);
    SupplierInvoice b = fx.approvedInvoice("G-0001", "2000.00", PayablesFixtures.DATE);
    PaymentCommand cmd =
        fx.paymentCommand(
            "G-0001",
            PaymentMode.CHEQUE,
            "BDO-CA",
            PAY_DATE,
            null,
            Map.of(
                a.getOpenItemId(),
                new BigDecimal("1100.00"),
                b.getOpenItemId(),
                new BigDecimal("500.00")));
    PaymentVoucher draft = as.run("accountant", () -> vouchers.create(cmd));
    assertThat(draft.getCategory()).isEqualTo(PaymentCategory.SUPPLIER);
    assertThat(draft.getAmount()).isEqualByComparingTo("1600.00");
    assertThat(vouchers.payableItems(fx.companyId(), "G-0001", null))
        .filteredOn(p -> p.item().getId().equals(b.getOpenItemId()))
        .singleElement()
        .satisfies(p -> assertThat(p.available()).isEqualByComparingTo("1700.00"));

    as.run("accountant", () -> vouchers.submit(draft.getId()));
    PaymentVoucher paid = as.run("checker", () -> approvals.approve(draft.getId()));
    assertThat(paid.getStatus()).isEqualTo(VoucherStatus.APPROVED);
    assertThat(paid.getChequeNo()).matches("\\d{6}");
    assertThat(fx.posted(paid.getJournalBatchNo(), "2501")).isEqualByComparingTo("1600.00");
    assertThat(fx.posted(paid.getJournalBatchNo(), "1111")).isEqualByComparingTo("-1600.00");
    assertThat(openItems.get(a.getOpenItemId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
    assertThat(openItems.get(b.getOpenItemId()).outstanding()).isEqualByComparingTo("1700.00");
    assertThat(openItems.get(paid.getOpenItemId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);

    PaymentVoucher voided =
        as.run("checker", () -> approvals.voidCheque(draft.getId(), PAY_DATE.plusDays(2), "Stale"));
    assertThat(voided.getStatus()).isEqualTo(VoucherStatus.VOIDED);
    assertThat(fx.posted(voided.getVoidBatchNo(), "1111")).isEqualByComparingTo("1600.00");
    List<OpenItem> reinstated =
        openItems.partyItems(fx.companyId(), a.getPartyId()).stream()
            .filter(i -> i.getDocumentNo().equals(a.getDocumentNo()))
            .toList();
    assertThat(reinstated).hasSize(2);
    assertThat(reinstated.get(1).outstanding()).isEqualByComparingTo("1100.00");
    assertThat(reinstated.get(1).getDueDate()).isEqualTo(a.getDueDate());
  }

  @Test
  void presentedChequesCannotBeVoidedAndOverpaymentIsRefused() {
    SupplierInvoice inv = fx.approvedInvoice("G-0002", "300.00", PayablesFixtures.DATE);
    PaymentVoucher paid =
        fx.approvedPayment(
            fx.paymentCommand(
                "G-0002",
                PaymentMode.CHEQUE,
                "BDO-CA",
                PAY_DATE,
                null,
                Map.of(inv.getOpenItemId(), new BigDecimal("100.00"))));
    as.run("accountant", () -> approvals.markPresented(paid.getId(), PAY_DATE.plusDays(1)));
    assertThatThrownBy(
            () -> as.run("checker", () -> approvals.voidCheque(paid.getId(), PAY_DATE, "x")))
        .isInstanceOf(BusinessRuleException.class);

    PaymentCommand tooMuch =
        fx.paymentCommand(
            "G-0002",
            PaymentMode.CHEQUE,
            "BDO-CA",
            PAY_DATE,
            null,
            Map.of(inv.getOpenItemId(), new BigDecimal("999.00")));
    assertThatThrownBy(() -> as.run("accountant", () -> vouchers.create(tooMuch)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("exceeds");
    PaymentCommand usd =
        fx.paymentCommand(
            "G-0002",
            PaymentMode.BANK_TRANSFER,
            "BDO-USD",
            PAY_DATE,
            null,
            Map.of(inv.getOpenItemId(), new BigDecimal("1.00")));
    assertThatThrownBy(() -> as.run("accountant", () -> vouchers.create(usd)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("PHP");
  }

  @Test
  void bankTransferIsAdvisedInTheNotificationFileAndCannotBeVoided() {
    SupplierInvoice inv = fx.approvedInvoice("S-0002", "4000.00", PAY_DATE);
    PaymentVoucher paid =
        fx.approvedPayment(
            fx.paymentCommand(
                "S-0002",
                PaymentMode.BANK_TRANSFER,
                "BPI-SA",
                PAY_DATE.plusDays(1),
                null,
                Map.of(inv.getOpenItemId(), new BigDecimal("4400.00"))));
    assertThat(paid.getChequeNo()).isNull();
    assertThat(fx.posted(paid.getJournalBatchNo(), "1112")).isEqualByComparingTo("-4400.00");
    assertThatThrownBy(
            () -> as.run("checker", () -> approvals.voidCheque(paid.getId(), PAY_DATE, "x")))
        .isInstanceOf(BusinessRuleException.class);

    var file =
        as.run(
            "accountant",
            () ->
                notifications.generate(
                    fx.bankId("BPI-SA"),
                    PAY_DATE,
                    PAY_DATE.plusDays(1),
                    false,
                    NotificationFormat.FIXED_WIDTH));
    String text = new String(file.content(), StandardCharsets.US_ASCII);
    assertThat(text).contains(paid.getVoucherNo()).contains("S-0002").startsWith("01");
    assertThat(text.lines().filter(l -> l.startsWith("02")).findFirst()).isPresent();
    var csv =
        as.run(
            "accountant",
            () ->
                notifications.generate(
                    fx.bankId("BPI-SA"), PAY_DATE, PAY_DATE.plusDays(1), true, null));
    assertThat(csv.fileName()).endsWith(".csv");
    assertThat(new String(csv.content(), StandardCharsets.US_ASCII)).contains("4400.00");
  }

  @Test
  void commissionPayablesOfAnAgentArePaidWithTheCommissionEvent() {
    var agent = parties.getByCode(fx.companyId(), "A-0001");
    String doc = PayablesFixtures.unique("COMM");
    OpenItem commission =
        as.run(
            "accountant",
            () ->
                openItems.record(
                    new OpenItemValues(
                        fx.companyId(),
                        data.branch("HO").getId(),
                        agent.getId(),
                        agent.getCode(),
                        ItemDirection.CREDIT,
                        "COMMISSION",
                        doc,
                        PayablesFixtures.DATE,
                        PayablesFixtures.DATE,
                        "PHP",
                        new BigDecimal("750.00"),
                        new BigDecimal("750.00"),
                        "TEST",
                        doc,
                        null,
                        "Commission payable")));
    PaymentVoucher paid =
        fx.approvedPayment(
            fx.paymentCommand(
                "A-0001",
                PaymentMode.CHEQUE,
                "BDO-CA",
                PAY_DATE,
                null,
                Map.of(commission.getId(), new BigDecimal("750.00"))));
    assertThat(paid.getCategory()).isEqualTo(PaymentCategory.COMMISSION);
    assertThat(fx.posted(paid.getJournalBatchNo(), "2300")).isEqualByComparingTo("750.00");
    assertThat(openItems.get(commission.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
    assertThat(
            vouchers
                .search(
                    fx.companyId(),
                    VoucherStatus.APPROVED,
                    "A-0001",
                    PAY_DATE,
                    PAY_DATE,
                    PageRequest.of(0, 10))
                .getContent())
        .isNotEmpty();
  }

  @Test
  void rejectAndCancelFollowTheVoucherLifecycle() {
    SupplierInvoice inv = fx.approvedInvoice("S-0001", "100.00", PAY_DATE);
    PaymentCommand cmd =
        fx.paymentCommand(
            "S-0001",
            PaymentMode.CHEQUE,
            "BDO-CA",
            PAY_DATE,
            null,
            Map.of(inv.getOpenItemId(), new BigDecimal("50.00")));
    PaymentVoucher v = as.run("accountant", () -> vouchers.submit(vouchers.create(cmd).getId()));
    assertThatThrownBy(() -> as.run("accountant", () -> approvals.approve(v.getId())))
        .isInstanceOf(BusinessRuleException.class);
    PaymentVoucher rejected = as.run("checker", () -> vouchers.reject(v.getId(), "Wrong bank"));
    assertThat(rejected.getStatus()).isEqualTo(VoucherStatus.DRAFT);
    PaymentCommand changed =
        fx.paymentCommand(
            "S-0001",
            PaymentMode.BANK_TRANSFER,
            "BPI-SA",
            PAY_DATE,
            null,
            Map.of(inv.getOpenItemId(), new BigDecimal("60.00")));
    PaymentVoucher updated = as.run("accountant", () -> vouchers.update(v.getId(), changed));
    assertThat(updated.getAmount()).isEqualByComparingTo("60.00");
    assertThat(as.run("accountant", () -> vouchers.cancel(v.getId(), "Not needed")).getStatus())
        .isEqualTo(VoucherStatus.CANCELLED);
    assertThat(vouchers.get(v.getId()).getAllocations()).hasSize(1);
  }
}
