package com.iortatechnxt.brokerverse.disbursement;

import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.APPROVER;
import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.LEADER;
import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.PROCESSOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.CwtDirection;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.LineOrigin;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest.FundingTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEdit;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher.VoucherTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine.LineValues;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.CwtTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.ReceiptTag;
import com.iortatechnxt.brokerverse.disbursement.service.CheckStaleJob;
import com.iortatechnxt.brokerverse.disbursement.service.FundingService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentActions;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import com.iortatechnxt.brokerverse.disbursement.service.StatusEditService;
import com.iortatechnxt.brokerverse.disbursement.service.TagService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherBulk;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherBulk.ItemResult;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherService.Allocation;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountStatus;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.BankAccountService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Processing rules of Disbursement: payee maker-checker and deletion (DIS 2.2), editable proforma
 * and expense allocation (DIS 2.7.6, 2.7.10), status edits (DIS 2.8.5), tags (DIS 2.10-2.11), stale
 * checks and their re-issue (DIS 3.26.2), bulk approval and rejection (DIS 2.19-2.21), dual
 * approval of account funding (DIS 2.17) and the bank account and check series changes (DIS
 * 2.23-2.24).
 */
@IntegrationTest
class DisbursementProcessingIT {

  @Autowired private DisbursementFixtures fx;
  @Autowired private PayeeService payees;
  @Autowired private VoucherService vouchers;
  @Autowired private VoucherActions actions;
  @Autowired private VoucherBulk bulk;
  @Autowired private InstrumentActions instrumentActions;
  @Autowired private InstrumentService instruments;
  @Autowired private StatusEditService edits;
  @Autowired private TagService tags;
  @Autowired private CheckStaleJob staleJob;
  @Autowired private RequestIntakeService intake;
  @Autowired private FundingService funding;
  @Autowired private BankAccountQueryService banks;
  @Autowired private BankAccountService bankService;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void payeesNeedTheCheckerAndOnlyUnusedDraftsAreDeleted() {
    String code = "PAY-" + BookingFixtures.token();
    Payee draft =
        fx.as(
            LEADER,
            () ->
                payees.create(
                    fx.company(),
                    code,
                    DisbursementFixtures.details(
                        "SUPPLIER", "Draft supplier", DisbursementMode.CHECK),
                    List.of(),
                    PayeeSource.MANUAL));
    assertThat(draft.getStage()).isEqualTo(PayeeStage.DRAFT);
    assertThatThrownBy(
            () ->
                fx.as(
                    LEADER,
                    () ->
                        payees.create(
                            fx.company(),
                            code,
                            DisbursementFixtures.details(
                                "SUPPLIER", "Twice", DisbursementMode.CHECK),
                            List.of(),
                            PayeeSource.MANUAL)))
        .isInstanceOf(RuntimeException.class);
    fx.as(LEADER, () -> payees.submit(draft.getId()));
    assertThatThrownBy(() -> fx.as(LEADER, () -> payees.authorize(draft.getId())))
        .isInstanceOf(BusinessRuleException.class);
    Payee active = fx.as(APPROVER, () -> payees.authorize(draft.getId()));
    assertThat(active.getStage()).isEqualTo(PayeeStage.ACTIVE);

    Payee amended =
        fx.as(
            LEADER,
            () ->
                payees.update(
                    draft.getId(),
                    DisbursementFixtures.details("SUPPLIER", "Renamed", DisbursementMode.CHECK)));
    assertThat(amended.getStage()).isEqualTo(PayeeStage.FOR_AUTHORIZATION);
    fx.as(APPROVER, () -> payees.authorize(draft.getId()));
    fx.as(LEADER, () -> payees.deactivate(draft.getId()));
    assertThat(fx.as(APPROVER, () -> payees.authorize(draft.getId())).getStage())
        .isEqualTo(PayeeStage.INACTIVE);
    assertThatThrownBy(() -> fx.as(LEADER, () -> Boolean.valueOf(delete(draft.getId()))))
        .isInstanceOf(BusinessRuleException.class);

    Payee other =
        fx.as(
            LEADER,
            () ->
                payees.create(
                    fx.company(),
                    "DEL-" + BookingFixtures.token(),
                    DisbursementFixtures.details("OTHER", "To delete", DisbursementMode.CHECK),
                    List.of(),
                    PayeeSource.MANUAL));
    fx.as(LEADER, () -> Boolean.valueOf(delete(other.getId())));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dsb_payee where id = ?", Integer.class, other.getId()))
        .isZero();
  }

  private boolean delete(Long id) {
    payees.delete(id);
    return true;
  }

  @Test
  void anEditedProformaIsPostedAsGivenAndTheCheckGoesThroughItsStatuses() {
    Payee supplier = fx.supplierPayee();
    Voucher v = fx.voucherOf(fx.encoded("SUPPLIER", supplier, "1000.00"));
    BankAccount bank = fx.as(PROCESSOR, () -> banks.get(v.getBankAccountId()));
    Voucher withEwt =
        fx.as(
            PROCESSOR,
            () ->
                vouchers.updateTerms(
                    v.getId(),
                    new VoucherTerms(
                        DisbursementMode.CHECK,
                        bank.getId(),
                        null,
                        new BigDecimal("20.00"),
                        "Supplies",
                        LocalDate.now(),
                        null,
                        null)));
    assertThat(withEwt.getNet()).isEqualByComparingTo("980.00");
    assertThat(withEwt.getLines()).extracting(l -> l.getComponent()).contains("EWT");

    Voucher allocated =
        fx.as(
            PROCESSOR,
            () ->
                vouchers.allocate(
                    v.getId(),
                    List.of(
                        new Allocation("2501", null, new BigDecimal("600.00"), "Part 1"),
                        new Allocation("2501", null, new BigDecimal("400.00"), "Part 2"))));
    assertThat(allocated.isProformaEdited()).isTrue();
    assertThat(allocated.getLines())
        .filteredOn(l -> l.getOrigin() == LineOrigin.ALLOCATION)
        .hasSize(2);
    assertThatThrownBy(
            () ->
                fx.as(
                    PROCESSOR,
                    () ->
                        vouchers.editProforma(
                            v.getId(),
                            List.of(
                                new LineValues(
                                    BalanceSide.DEBIT,
                                    "2501",
                                    null,
                                    null,
                                    null,
                                    BigDecimal.TEN,
                                    null,
                                    LineOrigin.EDITED,
                                    null)))))
        .isInstanceOf(BusinessRuleException.class);

    fx.as(PROCESSOR, () -> actions.submit(v.getId(), null));
    fx.as(LEADER, () -> actions.submitForApproval(v.getId(), null));
    Voucher approved = fx.as(APPROVER, () -> actions.approve(v.getId(), null));
    assertThat(approved.getPostingStatus()).isEqualTo(PostingStatus.POSTED);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from jnl_batch where source_reference = ?",
                Integer.class,
                "DV:" + approved.getDvNo()))
        .isEqualTo(1);

    Instrument check =
        fx.as(PROCESSOR, () -> instrumentActions.print(v.getId(), Change.user("Print")));
    assertThat(fx.as(PROCESSOR, () -> instrumentActions.document(v.getId()))).isNotEmpty();
    StatusEdit edit =
        fx.as(
            PROCESSOR,
            () -> edits.request(v.getId(), InstrumentStatus.RELEASED, "Released yesterday"));
    assertThatThrownBy(
            () -> fx.as(PROCESSOR, () -> edits.request(v.getId(), InstrumentStatus.RELEASED, "x")))
        .isInstanceOf(BusinessRuleException.class);
    fx.as(LEADER, () -> edits.approve(edit.getId()));
    assertThat(fx.as(PROCESSOR, () -> instruments.forVoucher(v.getId())).getStatus())
        .isEqualTo(InstrumentStatus.RELEASED);

    fx.as(
        PROCESSOR,
        () ->
            tags.receipt(
                v.getId(),
                new ReceiptTag(
                    "OR-" + BookingFixtures.token(),
                    LocalDate.now(),
                    LocalDate.now(),
                    null,
                    null)));
    fx.as(
        PROCESSOR,
        () ->
            tags.cwt(
                v.getId(),
                new CwtTag(
                    CwtDirection.RELEASED,
                    "2307-" + BookingFixtures.token(),
                    LocalDate.now().minusMonths(2),
                    LocalDate.now(),
                    LocalDate.now(),
                    new BigDecimal("20.00"),
                    null,
                    null)));
    assertThat(fx.as(PROCESSOR, () -> tags.of(v.getId()))).hasSize(2);

    jdbc.update(
        "update dsb_instrument set printed_on = ? where id = ?",
        LocalDate.now().minusDays(200),
        check.getId());
    staleJob.execute(LocalDate.now());
    assertThat(fx.as(PROCESSOR, () -> instruments.forVoucher(v.getId())).getStatus())
        .isEqualTo(InstrumentStatus.STALE);
    IntakeRequest reissue = fx.as(PROCESSOR, () -> instrumentActions.reissue(v.getId()));
    assertThat(reissue.getDisbursementType()).isEqualTo("STALE_REISSUE");
    assertThat(reissue.getStatus()).isEqualTo(RequestStatus.IN_VOUCHER);
  }

  @Test
  void vouchersAreApprovedInBulkOrRejectedBackToTheirSource() {
    Payee client = fx.clientPayee();
    Voucher ready = fx.voucherOf(fx.encoded("REFUND", client, "300.00"));
    fx.as(PROCESSOR, () -> actions.routeToApprover(ready.getId(), "Straight"));
    Voucher rejected = fx.voucherOf(fx.encoded("REFUND", client, "150.00"));
    fx.as(PROCESSOR, () -> actions.routeToApprover(rejected.getId(), null));

    List<ItemResult> results =
        fx.as(APPROVER, () -> bulk.approve(List.of(ready.getId(), -1L), "Bulk"));
    assertThat(results).extracting(ItemResult::ok).containsExactly(true, false);
    assertThat(fx.voucher(ready.getId()).getStage()).isEqualTo(VoucherStage.APPROVED);

    fx.as(APPROVER, () -> actions.reject(rejected.getId(), "INCOMPLETE_DOCUMENTS", "No RFP"));
    Voucher done = fx.voucher(rejected.getId());
    assertThat(done.getStage()).isEqualTo(VoucherStage.REJECTED);
    assertThat(fx.as(PROCESSOR, () -> intake.get(done.getRequestId())).getStatus())
        .isEqualTo(RequestStatus.RETURNED);
    Voucher cancelled = fx.voucherOf(fx.encoded("OTHER", client, "50.00"));
    fx.as(PROCESSOR, () -> actions.cancel(cancelled.getId(), "DUPLICATE", null));
    assertThat(fx.voucher(cancelled.getId()).getStage()).isEqualTo(VoucherStage.CANCELLED);
  }

  @Test
  void fundingNeedsAVerifierAndTwoDifferentApprovers() {
    List<BankAccount> php = fx.as(LEADER, () -> banks.listActive(fx.company(), "PHP"));
    FundingRequest f =
        fx.as(
            LEADER,
            () ->
                funding.create(
                    fx.company(),
                    new FundingTerms(
                        php.get(1).getId(),
                        php.get(0).getId(),
                        new BigDecimal("1000.00"),
                        "PHP",
                        "Weekly funding",
                        LocalDate.now(),
                        null)));
    fx.as(LEADER, () -> funding.submit(f.getId()));
    assertThatThrownBy(() -> fx.as(LEADER, () -> funding.verify(f.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    fx.as("disbtl2", () -> funding.verify(f.getId(), "OK"));
    fx.as(APPROVER, () -> funding.approve(f.getId(), "First", null));
    assertThatThrownBy(() -> fx.as(APPROVER, () -> funding.approve(f.getId(), "Again", null)))
        .isInstanceOf(BusinessRuleException.class);
    FundingRequest done =
        fx.as(
            "disbappr2",
            () -> funding.approve(f.getId(), "Second", "BOB-" + BookingFixtures.token()));
    assertThat(done.getStage()).isEqualTo(FundingStage.APPROVED);
    assertThat(done.getJournalNo()).isNotBlank();
  }

  @Test
  void bankAccountsAreTaggedInactiveWithFourEyesAndCheckSeriesEdited() {
    BankAccount bpi = fx.as(LEADER, () -> banks.getByCode(fx.company(), "BPI-SA"));
    try {
      fx.as(LEADER, () -> bankService.requestStatus(bpi.getId(), BankAccountStatus.INACTIVE));
      assertThatThrownBy(() -> fx.as(LEADER, () -> bankService.authorize(bpi.getId())))
          .isInstanceOf(BusinessRuleException.class);
      BankAccount inactive = fx.as(APPROVER, () -> bankService.authorize(bpi.getId()));
      assertThat(inactive.getStatus()).isEqualTo(BankAccountStatus.INACTIVE);
      assertThat(inactive.isActive()).isFalse();
    } finally {
      fx.as(LEADER, () -> bankService.requestStatus(bpi.getId(), BankAccountStatus.ACTIVE));
      fx.as(APPROVER, () -> bankService.authorize(bpi.getId()));
    }
    assertThat(fx.as(LEADER, () -> banks.get(bpi.getId())).isActive()).isTrue();

    long first = 7_000_000L + System.nanoTime() % 1_000_000L;
    ChequeBook book =
        fx.as(
            LEADER,
            () -> bankService.addChequeBook(bpi.getId(), first, first + 49, LocalDate.now()));
    ChequeBook edited =
        fx.as(LEADER, () -> bankService.editChequeBook(book.getId(), first + 1, first + 50));
    assertThat(edited.getPreviousRange()).isEqualTo(first + "-" + (first + 49));
    assertThat(edited.getEditedBy()).isEqualTo(LEADER);
    fx.as(LEADER, () -> bankService.cancelChequeBook(book.getId()));
  }
}
