package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccountRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.EntryType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The history of a record reads in words: the labels of the screens, never codes. */
class RecordWordingTest {

  private final LovService lovs = mock(LovService.class);
  private final BranchRepository branches = mock(BranchRepository.class);
  private final CashBankAccountRepository banks = mock(CashBankAccountRepository.class);
  private final RecordWording wording = new RecordWording(lovs, branches, banks);

  @Test
  void aCreationRecordNamesTheTypeBranchPayorPaymentAndAccounts() {
    when(lovs.label("AR_CLASS", "PREMIUM")).thenReturn("Premium Payment");
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("Head Office - Makati");
    when(branches.findById(1L)).thenReturn(Optional.of(branch));
    CashBankAccount bank = mock(CashBankAccount.class);
    when(bank.getName()).thenReturn("DFLB Savings (BDO Ortigas)");
    when(banks.findByCompanyIdAndCode(7L, "DFLB_SAVINGS_ORTIGAS")).thenReturn(Optional.of(bank));
    ReceiptRecord r = new ReceiptRecord(7L, "CR-AR-000001", RecordKind.CREATION, ReceiptKind.AR);
    r.describe(
        "PREMIUM",
        1L,
        new RecordParty(
            EntryType.CLIENT, "C-1", "Garcia, Antonio", null, null, null, "Garcia, Antonio"),
        new RecordTender(
            TenderType.CASH,
            "PHP",
            "DFLB_SAVINGS_ORTIGAS",
            new BigDecimal("22268.75"),
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 10, 10),
            "Premium paid at the counter",
            null),
        List.of(new RecordAccount("BI-HO-2026-000002", new BigDecimal("22268.75"))));

    assertThat(wording.summary(r))
        .isEqualTo(
            "Premium Payment at Head Office - Makati, payor Garcia, Antonio, Cash PHP 22,268.75"
                + " to DFLB Savings (BDO Ortigas), accounts BI-HO-2026-000002 22,268.75")
        .doesNotContain("_", "[", "RecordAccount");
  }

  @Test
  void aReasonNamesTheReasonAndTheReinstatementInWords() {
    when(lovs.label(eq("RECEIPT_CANCEL_REASON"), any())).thenReturn("Discrepancy in check amount");
    when(lovs.label(eq("REINSTATEMENT_REASON"), any())).thenReturn("Payment confirmed");

    assertThat(
            wording.reason(
                RecordKind.CANCELLATION,
                new RecordReason(
                    "PRM_CHECK_AMOUNT", "Double issuance of the AR", null, null, null, null, null)))
        .isEqualTo("Discrepancy in check amount: Double issuance of the AR");
    assertThat(
            wording.reason(
                RecordKind.REINSTATEMENT,
                new RecordReason(
                    "RIN_CONFIRMED",
                    null,
                    ReinstatementType.PARTIAL,
                    "BI-HO-2026-000002",
                    null,
                    null,
                    null)))
        .isEqualTo("Payment confirmed; partial reinstatement of invoice BI-HO-2026-000002");
    assertThat(wording.reason(RecordKind.CANCELLATION, null)).isEmpty();
  }
}
