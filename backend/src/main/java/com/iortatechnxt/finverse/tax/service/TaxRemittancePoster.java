package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.finverse.tax.domain.RemittanceFacts;
import com.iortatechnxt.finverse.tax.domain.RemittancePosting;
import com.iortatechnxt.finverse.tax.domain.TaxForm;
import com.iortatechnxt.finverse.tax.domain.TaxReturn;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Accounts for the payment of a filed return with the {@code TAX_REMITTANCE} event, in the payment
 * transaction.
 *
 * <ul>
 *   <li>Forms with a credit account (VAT): Dr tax payable (output VAT of the period), Cr credit
 *       account (input VAT and carried-over excess, up to the output VAT), Cr bank (VAT payable).
 *       An excess of credits stays on the credit account and is carried to the next quarter.
 *   <li>Other forms: Dr tax payable, Cr bank, for the amount payable; for 1601-EQ this is the
 *       quarter's withholding net of the monthly 0619-E remittances, which already cleared their
 *       share of the payable.
 * </ul>
 *
 * Nothing is posted for a return without tax due. The journal is dated the payment date, booked at
 * the bank account's branch (head office when the bank has none), in base currency, with source
 * reference {@code TAXRET:<return id>} (idempotent).
 */
@Component
public class TaxRemittancePoster {

  /** Accounting event type. */
  public static final String EVENT = "TAX_REMITTANCE";

  private final AccountingEventPublisher publisher;
  private final BankAccountQueryService banks;
  private final OrganizationService organization;

  /**
   * Creates the poster.
   *
   * @param publisher accounting engine
   * @param banks company bank accounts
   * @param organization company and branches
   */
  public TaxRemittancePoster(
      AccountingEventPublisher publisher,
      BankAccountQueryService banks,
      OrganizationService organization) {
    this.publisher = publisher;
    this.banks = banks;
    this.organization = organization;
  }

  /**
   * Posts the remittance.
   *
   * @param r filed return
   * @param form its form
   * @param facts payment facts
   * @return posting result
   */
  public RemittancePosting post(TaxReturn r, TaxForm form, RemittanceFacts facts) {
    boolean withCredits = form.getCreditAccountCode() != null;
    BigDecimal payable = withCredits ? r.getTaxDue() : r.getAmountPayable();
    BigDecimal amount = r.getAmountPayable();
    // Credits are applied up to the tax due; an excess stays on the credit account (carry-over).
    BigDecimal credit = withCredits ? payable.subtract(amount) : Money.zero();
    if (payable.signum() <= 0) {
      return new RemittancePosting(Money.zero(), Money.zero(), Money.zero(), null);
    }
    Map<String, String> roles = new HashMap<>();
    roles.put("TAX_PAYABLE", form.getPayableAccountCode());
    if (withCredits) {
      roles.put("TAX_CREDIT", form.getCreditAccountCode());
    }
    Long branchId = headOffice(r.getCompanyId());
    if (amount.signum() > 0) {
      BankAccount bank = requireBank(r.getCompanyId(), facts.bankAccountCode());
      roles.put("BANK", bank.getGlAccountCode());
      branchId = bank.getBranchId() == null ? branchId : bank.getBranchId();
    }
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("TAX_PAYABLE", payable);
    amounts.put("TAX_CREDIT", credit);
    amounts.put("AMOUNT", amount);
    String batchNo =
        publisher
            .publish(
                new BusinessEvent(
                    EVENT,
                    r.getCompanyId(),
                    branchId,
                    facts.paidOn(),
                    organization.getCompany(r.getCompanyId()).getBaseCurrency(),
                    TaxSourceQueries.MODULE,
                    "TAXRET:" + r.getId(),
                    r.getReturnNo(),
                    null,
                    null,
                    null,
                    "Remittance of "
                        + r.getFormCode()
                        + " "
                        + r.period().label()
                        + " ref "
                        + facts.reference(),
                    amounts,
                    roles))
            .getBatchNo();
    return new RemittancePosting(payable, credit, amount, batchNo);
  }

  private BankAccount requireBank(Long companyId, String code) {
    if (code == null || code.isBlank()) {
      throw new BusinessRuleException(
          "BANK_ACCOUNT_REQUIRED", "Select the bank account the tax is paid from");
    }
    BankAccount bank = banks.getByCode(companyId, code);
    return banks.requireActive(bank.getId());
  }

  private Long headOffice(Long companyId) {
    return organization.listBranches(companyId).stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .map(Branch::getId)
        .orElseThrow(
            () -> new BusinessRuleException("NO_HEAD_OFFICE", "The company has no head office"));
  }
}
