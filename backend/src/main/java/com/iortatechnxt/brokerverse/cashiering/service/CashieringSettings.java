package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business parameters and organisation facts of cashiering: the 2% CWT application percent
 * (CSHID.020), commission realization (OPERATIONS_DESIGN section 5), the collection accounts of the
 * {@code @BANK} role, Head Office (CSHID.006) and the BOOK rate (CSHID.012-014).
 */
@Component
@Transactional(readOnly = true)
public class CashieringSettings {

  /** Module code on ledger movements, journals and hand-offs. */
  public static final String MODULE = "CASHIERING";

  private static final BigDecimal DEFAULT_CWT_PERCENT = new BigDecimal("98");

  private final SystemParameterService parameters;
  private final BranchRepository branches;
  private final BookRates bookRates;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   * @param branches branches
   * @param bookRates BOOK rates
   */
  public CashieringSettings(
      SystemParameterService parameters, BranchRepository branches, BookRates bookRates) {
    this.parameters = parameters;
    this.branches = branches;
    this.bookRates = bookRates;
  }

  /**
   * Share of the premium applied for 2% CWT accounts (CSHID.020).
   *
   * @return percent, default 98
   */
  public BigDecimal cwtApplicationPercent() {
    String value = parameters.text("CWT_APPLICATION_PERCENT", "").strip();
    return value.isEmpty() ? DEFAULT_CWT_PERCENT : new BigDecimal(value);
  }

  /**
   * Whether commission is realized when premium is applied.
   *
   * @return true for ON_COLLECTION (the default)
   */
  public boolean realizeOnCollection() {
    return "ON_COLLECTION"
        .equals(parameters.text("OPS_COMMISSION_REALIZATION", "ON_COLLECTION").strip());
  }

  /**
   * The GL account of the {@code @BANK} role for a mode of payment.
   *
   * @param mode mode of payment
   * @return account code, null when not configured (the posting then fails with a clear error)
   */
  public String collectionAccount(PaymentMode mode) {
    String key = mode == PaymentMode.CASH ? "CASH_ON_HAND_ACCOUNT" : "CASH_BANK_ACCOUNT";
    String value = parameters.text(key, "").strip();
    return value.isEmpty() ? null : value;
  }

  /**
   * A branch of the company.
   *
   * @param companyId company
   * @param branchId branch
   * @return branch
   */
  public Branch branch(Long companyId, Long branchId) {
    Branch branch =
        branches
            .findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    if (!branch.getCompany().getId().equals(companyId)) {
      throw new BusinessRuleException(
          "BRANCH_OF_OTHER_COMPANY", "Branch " + branch.getCode() + " is not of this company");
    }
    return branch;
  }

  /**
   * The Head Office branch of a company (OR issuance, CSHID.006).
   *
   * @param companyId company
   * @return head office
   */
  public Branch headOffice(Long companyId) {
    return branches.findByCompanyIdOrderByCode(companyId).stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_HEAD_OFFICE",
                    "The company has no Head Office branch for official receipts"));
  }

  /**
   * The BOOK rate of a currency (CSHID.012-014).
   *
   * @param companyId company
   * @param currency currency
   * @param date value date
   * @return rate, 2 decimals (1 for the base currency)
   */
  public BigDecimal bookRate(Long companyId, String currency, LocalDate date) {
    return bookRates.rate(companyId, currency, date);
  }
}
