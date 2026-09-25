package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.LineOrigin;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher.VoucherTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine.LineValues;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing of a voucher by the processor (DIS 2.7.1-2.7.6, 2.7.10, 3.30.0): the terms (mode of
 * payment allowed for the payee, active paying account in the voucher currency, payee account for
 * credit modes, withholding tax, purpose, value date, cost centre, expense account of an OTHER
 * payment), the proforma entry rebuilt from the rule, edited line by line or built from an expense
 * allocation. Changes are allowed while the voucher is with the processor (DIS 3.27.0 addendum:
 * posted entries are never edited).
 */
@Service
@Transactional
public class VoucherService {

  private static final Set<DisbursementMode> CREDIT_MODES =
      EnumSet.of(DisbursementMode.CTA, DisbursementMode.TT, DisbursementMode.ONLINE_BANKING);
  private static final String OTHER = "OTHER";

  private final VoucherRepository vouchers;
  private final PayeeQueryService payees;
  private final BankAccountQueryService banks;
  private final ProformaBuilder proforma;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param vouchers vouchers
   * @param payees payees
   * @param banks bank accounts
   * @param proforma proforma entry
   * @param audit audit trail
   */
  public VoucherService(
      VoucherRepository vouchers,
      PayeeQueryService payees,
      BankAccountQueryService banks,
      ProformaBuilder proforma,
      AuditTrailService audit) {
    this.vouchers = vouchers;
    this.payees = payees;
    this.banks = banks;
    this.proforma = proforma;
    this.audit = audit;
  }

  /**
   * A voucher with its proforma lines.
   *
   * @param id voucher
   * @return voucher
   */
  @Transactional(readOnly = true)
  public Voucher get(Long id) {
    return vouchers
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.VOUCHER, id));
  }

  /**
   * Sets the processing terms (DIS 2.7.1-2.7.4) and rebuilds the proforma from the rule.
   *
   * @param id voucher
   * @param terms terms
   * @return voucher
   */
  public Voucher updateTerms(Long id, VoucherTerms terms) {
    Voucher v = withProcessor(id);
    Payee payee = payees.get(v.getPayeeId());
    if (!payee.allows(terms.mode())) {
      throw new BusinessRuleException(
          "DV_MODE", "Mode " + terms.mode() + " is not allowed for payee " + payee.getPayeeCode());
    }
    BankAccount bank = banks.requireActive(terms.bankAccountId());
    if (!bank.getCurrency().equals(v.getCurrency())) {
      throw new BusinessRuleException(
          "DV_BANK_CURRENCY", "The paying account must be in " + v.getCurrency());
    }
    if (terms.payeeAccountId() != null
        && payee.getAccounts().stream()
            .noneMatch(a -> a.isActive() && a.getId().equals(terms.payeeAccountId()))) {
      throw new BusinessRuleException("DV_PAYEE_ACCOUNT", "Select an active account of the payee");
    }
    v.setTerms(terms);
    v.replaceLines(proforma.fromRule(v), false);
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        "Terms: " + terms.mode() + ", account " + bank.getCode() + ", EWT " + v.getEwt());
    return v;
  }

  /**
   * Saves the proforma edited by the processor (DIS 2.7.6): lines that differ from the rule are
   * marked EDITED and the voucher shows the approver that its entry was edited.
   *
   * @param id voucher
   * @param lines lines in order
   * @return voucher
   */
  public Voucher editProforma(Long id, List<LineValues> lines) {
    Voucher v = withProcessor(id);
    proforma.validate(v, lines);
    List<LineValues> rule = proforma.fromRule(v);
    List<LineValues> marked = new ArrayList<>();
    boolean edited = lines.size() != rule.size();
    for (LineValues l : lines) {
      boolean same = rule.stream().anyMatch(r -> sameLine(r, l));
      edited |= !same;
      marked.add(same ? withOrigin(l, LineOrigin.RULE) : withOrigin(l, LineOrigin.EDITED));
    }
    v.replaceLines(marked, edited);
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        edited ? "Proforma entry edited" : "Proforma entry saved as built by the rule");
    return v;
  }

  /**
   * Rebuilds the proforma from the rule, dropping the edits.
   *
   * @param id voucher
   * @return voucher
   */
  public Voucher resetProforma(Long id) {
    Voucher v = withProcessor(id);
    v.replaceLines(proforma.fromRule(v), false);
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        "Proforma rebuilt from rule");
    return v;
  }

  /**
   * Builds the debit side from an expense allocation (DIS 2.7.10, 3.30.0): the lines of the DV type
   * are replaced by one line per allocation row (account, cost centre, amount), which must add up
   * to the gross amount.
   *
   * @param id voucher
   * @param rows allocation rows
   * @return voucher
   */
  public Voucher allocate(Long id, List<Allocation> rows) {
    Voucher v = withProcessor(id);
    BigDecimal total =
        rows.stream().map(Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (rows.isEmpty() || total.compareTo(v.getGross()) != 0) {
      throw new BusinessRuleException(
          "DV_ALLOCATION_TOTAL",
          "The allocation must add up to the gross amount " + v.getGross().toPlainString());
    }
    List<LineValues> lines = new ArrayList<>();
    for (Allocation a : rows) {
      lines.add(
          new LineValues(
              BalanceSide.DEBIT,
              a.accountCode().strip(),
              null,
              a.costCenter(),
              null,
              a.amount(),
              v.getDisbursementType(),
              LineOrigin.ALLOCATION,
              a.narration()));
    }
    proforma.fromRule(v).stream()
        .filter(l -> !v.getDisbursementType().equals(l.component()))
        .forEach(lines::add);
    proforma.validate(v, lines);
    v.replaceLines(lines, true);
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        "Expense allocation of " + rows.size() + " line(s)");
    return v;
  }

  /**
   * What is still missing or wrong before the voucher can move on (DIS 2.7.4).
   *
   * @param v voucher
   * @return labels, empty when complete
   */
  @Transactional(readOnly = true)
  public List<String> missing(Voucher v) {
    List<String> out = new ArrayList<>(v.missing());
    if (v.getMode() != null
        && CREDIT_MODES.contains(v.getMode())
        && v.getPayeeAccountId() == null) {
      out.add("payee bank account");
    }
    if (OTHER.equals(v.getDisbursementType())
        && (v.getExpenseAccount() == null || v.getExpenseAccount().isBlank())) {
      out.add("expense account");
    }
    return out;
  }

  /**
   * Fails when the voucher is incomplete (DIS 2.7.4: cannot push with missing or wrong data).
   *
   * @param v voucher
   */
  public void requireComplete(Voucher v) {
    List<String> missing = missing(v);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "DV_INCOMPLETE", "Complete DV " + v.getDvNo() + ": " + String.join(", ", missing));
    }
  }

  private Voucher withProcessor(Long id) {
    Voucher v = get(id);
    if (v.getStage() != VoucherStage.IN_PROCESS) {
      throw new BusinessRuleException(
          "DV_NOT_EDITABLE", "DV " + v.getDvNo() + " is " + v.getStage() + "; return it first");
    }
    return v;
  }

  private static boolean sameLine(LineValues a, LineValues b) {
    return a.side() == b.side()
        && a.accountCode().equals(b.accountCode().strip())
        && a.amount().compareTo(b.amount()) == 0
        && Objects.equals(a.partyCode(), blankToNull(b.partyCode()))
        && Objects.equals(a.costCenter(), blankToNull(b.costCenter()));
  }

  private static LineValues withOrigin(LineValues l, LineOrigin origin) {
    return new LineValues(
        l.side(),
        l.accountCode().strip(),
        blankToNull(l.partyCode()),
        blankToNull(l.costCenter()),
        blankToNull(l.businessLine()),
        l.amount(),
        l.component(),
        origin,
        l.narration());
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * The payee bank account of a voucher.
   *
   * @param v voucher
   * @return account, null when none
   */
  @Transactional(readOnly = true)
  public PayeeAccount payeeAccount(Voucher v) {
    return v.getPayeeAccountId() == null
        ? null
        : payees.get(v.getPayeeId()).getAccounts().stream()
            .filter(a -> a.getId().equals(v.getPayeeAccountId()))
            .findFirst()
            .orElse(null);
  }

  /**
   * One row of an expense allocation (DIS 2.7.10).
   *
   * @param accountCode expense account
   * @param costCenter cost centre
   * @param amount amount
   * @param narration narration
   */
  public record Allocation(
      String accountCode, String costCenter, BigDecimal amount, String narration) {}
}
