package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccountRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The wording of a record in its history (audit trail): the labels of the receipt type, branch,
 * payment type, bank account and reason as the screens show them, never their codes
 * (FRS.CSH.02.01.14).
 */
@Component
public class RecordWording {

  private final LovService lovs;
  private final BranchRepository branches;
  private final CashBankAccountRepository bankAccounts;

  /**
   * Creates the wording.
   *
   * @param lovs lists of values (receipt types, reasons)
   * @param branches branches
   * @param bankAccounts bank accounts
   */
  public RecordWording(
      LovService lovs, BranchRepository branches, CashBankAccountRepository bankAccounts) {
    this.lovs = lovs;
    this.branches = branches;
    this.bankAccounts = bankAccounts;
  }

  /**
   * The details of a creation record in words: "Premium Payment at Head Office - Makati, payor
   * Garcia, Antonio, Cash PHP 22,268.75 to DFLB Savings (BDO Ortigas), accounts BI-HO-2026-000002
   * 22,268.75".
   *
   * @param r record
   * @return details
   */
  public String summary(ReceiptRecord r) {
    List<String> parts = new ArrayList<>();
    parts.add(typeAndBranch(r));
    RecordParty party = r.getParty();
    if (party != null && present(party.payorName())) {
      parts.add("payor " + party.payorName());
    }
    if (r.getTender() != null) {
      parts.add(paid(r.getCompanyId(), r.getTender()));
    }
    if (!r.getAccounts().isEmpty()) {
      parts.add("accounts " + accounts(r.getAccounts()));
    }
    return String.join(", ", parts);
  }

  /**
   * The reason of a cancellation or reinstatement record in words: "Discrepancy in check amount:
   * Double issuance of the AR; partial reinstatement of invoice BI-HO-2026-000002".
   *
   * @param kind cancellation or reinstatement
   * @param reason reason, may be null
   * @return words, empty when there is no reason
   */
  public String reason(RecordKind kind, RecordReason reason) {
    if (reason == null) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    String why = reasonText(kind, reason);
    if (!why.isEmpty()) {
      parts.add(why);
    }
    if (reason.reinstatementType() != null) {
      String which = DisplayFormat.words(reason.reinstatementType()) + " reinstatement";
      parts.add(present(reason.invoiceNo()) ? which + " of invoice " + reason.invoiceNo() : which);
    }
    return String.join("; ", parts);
  }

  private String reasonText(RecordKind kind, RecordReason reason) {
    String text = present(reason.reasonText()) ? reason.reasonText() : "";
    if (reason.reasonCode() == null) {
      return text;
    }
    String lov = kind == RecordKind.CANCELLATION ? "RECEIPT_CANCEL_REASON" : "REINSTATEMENT_REASON";
    String label = lovs.label(lov, reason.reasonCode());
    return text.isEmpty() ? label : label + ": " + text;
  }

  private String typeAndBranch(ReceiptRecord r) {
    String type = typeLabel(r);
    String branch = branchName(r.getBranchId());
    if (type == null) {
      return branch;
    }
    return branch.isEmpty() ? type : type + " at " + branch;
  }

  private String paid(Long companyId, RecordTender tender) {
    StringBuilder paid = new StringBuilder();
    if (tender.tenderType() != null) {
      paid.append(DisplayFormat.label(tender.tenderType())).append(' ');
    }
    if (tender.currency() != null) {
      paid.append(tender.currency()).append(' ');
    }
    paid.append(DisplayFormat.amount(tender.amount()));
    String bank = bankName(companyId, tender.bankAccount());
    if (!bank.isEmpty()) {
      paid.append(" to ").append(bank);
    }
    return paid.toString().strip();
  }

  private static String accounts(List<RecordAccount> accounts) {
    List<String> lines = new ArrayList<>();
    for (RecordAccount a : accounts) {
      lines.add(a.reference() + " " + DisplayFormat.amount(a.amount()));
    }
    return String.join(", ", lines);
  }

  private static boolean present(String text) {
    return text != null && !text.isBlank();
  }

  private String typeLabel(ReceiptRecord r) {
    if (r.getReceiptType() == null) {
      return null;
    }
    String lov = r.getReceiptKind() == ReceiptKind.OR ? "OR_TYPE" : "AR_CLASS";
    return lovs.label(lov, r.getReceiptType());
  }

  private String branchName(Long branchId) {
    if (branchId == null) {
      return "";
    }
    return branches.findById(branchId).map(Branch::getName).orElse("");
  }

  private String bankName(Long companyId, String code) {
    if (!present(code)) {
      return "";
    }
    return bankAccounts
        .findByCompanyIdAndCode(companyId, code)
        .map(CashBankAccount::getName)
        .orElse(code);
  }
}
