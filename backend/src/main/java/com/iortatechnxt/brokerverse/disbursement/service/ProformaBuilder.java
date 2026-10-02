package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingRule;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingRuleLine;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.accounting.service.JournalLineBuilder;
import com.iortatechnxt.brokerverse.accounting.service.RuleResolver;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.coa.service.PostingContext;
import com.iortatechnxt.brokerverse.coa.service.PostingEligibilityService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.LineOrigin;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine.LineValues;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The proforma entry of a voucher (DIS 2.7.6; design 1.3, 6): the {@code DISB_VOUCHER} event of the
 * voucher (gross in the component of its disbursement type, PAID net and EWT, the paying account as
 * {@code @PAY_ACCOUNT} and the expense account of an OTHER payment as {@code @EXPENSE}) run through
 * the configured accounting rule without posting. GL accounts are never chosen here: the paying
 * account is the bank account's GL account, or the checks-outstanding clearing account of parameter
 * {@code DISB_CHECK_CLEARING_ACCOUNT} for checks while {@code DISB_CHECK_CLEARING} is ON (DIS
 * 3.27.1). Lines edited by the processor are checked against the posting eligibility of their
 * accounts and must balance.
 */
@Component
public class ProformaBuilder {

  /** Amount component of the net paid. */
  static final String PAID = "PAID";

  /** Amount component of the withholding tax. */
  static final String EWT = "EWT";

  private static final String ON = "ON";

  private final RuleResolver rules;
  private final JournalLineBuilder lineBuilder;
  private final BankAccountQueryService banks;
  private final SystemParameterService parameters;
  private final BookRates rates;
  private final ChartOfAccountsService chart;
  private final PostingEligibilityService eligibility;

  /**
   * Creates the builder.
   *
   * @param rules accounting rules
   * @param lineBuilder rule line builder
   * @param banks bank accounts
   * @param parameters business parameters
   * @param rates Operations BOOK rates
   * @param chart chart of accounts
   * @param eligibility posting eligibility
   */
  public ProformaBuilder(
      RuleResolver rules,
      JournalLineBuilder lineBuilder,
      BankAccountQueryService banks,
      SystemParameterService parameters,
      BookRates rates,
      ChartOfAccountsService chart,
      PostingEligibilityService eligibility) {
    this.rules = rules;
    this.lineBuilder = lineBuilder;
    this.banks = banks;
    this.parameters = parameters;
    this.rates = rates;
    this.chart = chart;
    this.eligibility = eligibility;
  }

  /**
   * The {@code DISB_VOUCHER} event of a voucher at the BOOK rate of its value date.
   *
   * @param v voucher (mode, bank account and value date set)
   * @param negate true for the cancellation of an approved voucher (DIS 2.20.0)
   * @return event
   */
  public BusinessEvent event(Voucher v, boolean negate) {
    BigDecimal sign = negate ? BigDecimal.ONE.negate() : BigDecimal.ONE;
    Map<String, BigDecimal> amounts = new HashMap<>();
    amounts.put(v.getDisbursementType(), v.getGross().multiply(sign));
    amounts.put(PAID, v.getNet().multiply(sign));
    if (v.getEwt().signum() > 0) {
      amounts.put(EWT, v.getEwt().multiply(sign));
    }
    Map<String, String> accounts = new HashMap<>();
    accounts.put("PAY_ACCOUNT", payAccount(v));
    if (v.getExpenseAccount() != null && !v.getExpenseAccount().isBlank()) {
      accounts.put("EXPENSE", v.getExpenseAccount());
    }
    BusinessEvent event =
        new BusinessEvent(
            DisbursementSettings.EVENT_VOUCHER,
            v.getCompanyId(),
            v.getBranchId(),
            v.getValueDate(),
            v.getCurrency(),
            DisbursementSettings.MODULE,
            sourceRef(v, negate),
            v.getDvNo(),
            v.getPayeeCode(),
            null,
            v.getCostCenter(),
            v.getPurpose() == null ? "DV " + v.getDvNo() : v.getPurpose(),
            amounts,
            accounts);
    return rates.price(event);
  }

  /**
   * Source reference of the voucher posting or its reversal (idempotency key).
   *
   * @param v voucher
   * @param cancel reversal
   * @return {@code DV:<no>} or {@code DV:<no>:CANCEL}
   */
  static String sourceRef(Voucher v, boolean cancel) {
    return "DV:" + v.getDvNo() + (cancel ? ":CANCEL" : "");
  }

  /**
   * The proforma lines the rule gives for the voucher (origin RULE).
   *
   * @param v voucher
   * @return lines
   */
  public List<LineValues> fromRule(Voucher v) {
    BusinessEvent event = event(v, false);
    AccountingRule rule = rules.resolve(event);
    List<JournalLineRequest> built = lineBuilder.build(rule, event);
    List<AccountingRuleLine> used =
        rule.getLines().stream()
            .filter(l -> event.amount(l.getAmountComponent()).signum() != 0)
            .toList();
    List<LineValues> out = new ArrayList<>();
    for (int i = 0; i < built.size(); i++) {
      JournalLineRequest l = built.get(i);
      out.add(
          new LineValues(
              l.side(),
              l.accountCode(),
              l.partyCode(),
              l.costCenter(),
              l.businessLine(),
              l.amount(),
              used.get(i).getAmountComponent(),
              LineOrigin.RULE,
              l.narration()));
    }
    return out;
  }

  /**
   * Checks lines edited by the processor (DIS 2.7.6): positive amounts, eligible accounts, and a
   * balanced entry.
   *
   * @param v voucher
   * @param lines lines
   */
  public void validate(Voucher v, List<LineValues> lines) {
    if (lines.isEmpty()) {
      throw new BusinessRuleException("DV_ENTRY_EMPTY", "The proforma entry has no line");
    }
    BigDecimal balance = BigDecimal.ZERO;
    List<String> errors = new ArrayList<>();
    for (LineValues l : lines) {
      if (positive(l)) {
        balance =
            l.side() == BalanceSide.DEBIT ? balance.add(l.amount()) : balance.subtract(l.amount());
        errors.addAll(lineErrors(v, l));
      } else {
        errors.add("Line amounts must be positive");
      }
    }
    if (balance.signum() != 0) {
      errors.add("Debits and credits differ by " + balance.abs().toPlainString());
    }
    if (!errors.isEmpty()) {
      throw new BusinessRuleException("DV_ENTRY_INVALID", String.join("; ", errors));
    }
  }

  private static boolean positive(LineValues l) {
    return l.amount() != null && l.amount().signum() > 0;
  }

  private List<String> lineErrors(Voucher v, LineValues l) {
    GlAccount account = chart.getByCode(v.getCompanyId(), l.accountCode().strip());
    return eligibility.violations(account, context(v, l));
  }

  private static PostingContext context(Voucher v, LineValues l) {
    return new PostingContext(
        v.getBranchId(),
        v.getCurrency(),
        v.getValueDate(),
        false,
        Set.of(),
        l.costCenter() != null,
        l.businessLine() != null,
        l.partyCode() != null);
  }

  /**
   * The stored lines as journal lines, with sides swapped for a reversal.
   *
   * @param v voucher
   * @param rate exchange rate
   * @param reverse swap debit and credit
   * @return journal lines
   */
  public List<JournalLineRequest> journalLines(Voucher v, BigDecimal rate, boolean reverse) {
    return v.getLines().stream()
        .map(
            l ->
                new JournalLineRequest(
                    l.getAccountCode(),
                    reverse ? l.getSide().opposite() : l.getSide(),
                    l.getAmount(),
                    v.getCurrency(),
                    rate,
                    v.getBranchId(),
                    l.getCostCenter(),
                    l.getBusinessLine(),
                    l.getPartyCode(),
                    v.getDvNo(),
                    l.getNarration()))
        .toList();
  }

  private String payAccount(Voucher v) {
    if (v.getBankAccountId() == null) {
      throw new BusinessRuleException("DV_INCOMPLETE", "Select the paying bank account");
    }
    if (v.getMode() == DisbursementMode.CHECK && clearingOn()) {
      return parameters.text(DisbursementSettings.CHECK_CLEARING_ACCOUNT, "").strip();
    }
    return banks.get(v.getBankAccountId()).getGlAccountCode();
  }

  /**
   * Whether issued checks go through the checks-outstanding clearing account (DIS 3.27.1).
   *
   * @return true when {@code DISB_CHECK_CLEARING} is ON
   */
  public boolean clearingOn() {
    return ON.equals(parameters.text(DisbursementSettings.CHECK_CLEARING, ON).strip());
  }
}
