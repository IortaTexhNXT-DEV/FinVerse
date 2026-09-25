package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLineValues;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Checks of a correction line (ACSL 2.9.0): a postable account of the chart, a side, a positive
 * amount with two decimals, a sub-ledger party on control accounts, a known invoice and a ledger
 * component of the invoice ledger. Posting validations (period, account status, dimensions) are the
 * journal's own when the correction posts.
 */
@Component
public class CorrectionLineRules {

  private static final List<String> COMPONENTS =
      Arrays.stream(LedgerComponent.values()).map(Enum::name).toList();

  private static final int MAX_NARRATION = 250;

  private final CorrectionJournals journals;
  private final InvoiceLedgerQueryService ledger;
  private final OrganizationService organization;

  /**
   * Creates the rules.
   *
   * @param journals accounts
   * @param ledger Operations invoice ledger
   * @param organization branches
   */
  public CorrectionLineRules(
      CorrectionJournals journals,
      InvoiceLedgerQueryService ledger,
      OrganizationService organization) {
    this.journals = journals;
    this.ledger = ledger;
    this.organization = organization;
  }

  /**
   * Checks and normalises a line.
   *
   * @param c correction
   * @param v line as entered
   * @return the line to keep
   */
  public CorrectionLineValues check(Correction c, CorrectionLineValues v) {
    if (v.side() == null
        || v.amount() == null
        || v.amount().signum() <= 0
        || v.amount().scale() > 2) {
      throw new BusinessRuleException(
          "ACSL_LINE_INVALID", "Every line needs a side and a positive amount with two decimals");
    }
    String code = Acsl.blankToNull(v.accountCode());
    if (code == null) {
      throw new BusinessRuleException("ACSL_LINE_INVALID", "Every line needs a GL account");
    }
    GlAccount account = journals.account(c.getCompanyId(), code);
    String party = Acsl.blankToNull(v.partyCode());
    if (account.isControlAccount() && party == null) {
      throw new BusinessRuleException(
          "ACSL_PARTY_REQUIRED", "Control account " + code + " needs the sub-ledger party");
    }
    String invoice = Acsl.blankToNull(v.invoiceNo());
    String component = component(v.component(), invoice);
    if (invoice != null && ledger.find(invoice).isEmpty()) {
      throw new BusinessRuleException(
          "ACSL_INVOICE_UNKNOWN", "Invoice " + invoice + " is not in the ledger");
    }
    return new CorrectionLineValues(
        code,
        v.side(),
        v.amount().setScale(2),
        party,
        invoice,
        component,
        Acsl.blankToNull(v.costCenter()),
        Acsl.blankToNull(v.businessLine()),
        narration(c, v),
        v.origin() == null ? LineOrigin.MANUAL : v.origin(),
        Acsl.blankToNull(v.originalBatchNo()),
        v.originalLineNo());
  }

  /**
   * The head office of a company (branch of corrections without journal or invoice).
   *
   * @param companyId company
   * @return branch id
   */
  public Long headOffice(Long companyId) {
    List<Branch> branches = organization.listBranches(companyId);
    return branches.stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .or(() -> branches.stream().findFirst())
        .map(Branch::getId)
        .orElseThrow(
            () -> new BusinessRuleException("ACSL_NO_BRANCH", "The company has no branch"));
  }

  private static String component(String value, String invoice) {
    String component = Acsl.blankToNull(value);
    if (component == null) {
      return null;
    }
    String upper = component.toUpperCase(Locale.ROOT);
    if (!COMPONENTS.contains(upper) || invoice == null) {
      throw new BusinessRuleException(
          "ACSL_COMPONENT_INVALID",
          "A ledger component ("
              + String.join(", ", COMPONENTS)
              + ") needs the invoice it corrects");
    }
    return upper;
  }

  private static String narration(Correction c, CorrectionLineValues v) {
    String text = Acsl.blankToNull(v.narration());
    String narration = text == null ? c.getCorrectionNo() + " " + c.getDescription() : text;
    return narration.length() > MAX_NARRATION ? narration.substring(0, MAX_NARRATION) : narration;
  }

  /**
   * Signed change of an invoice ledger component for a line (ACSL 2.9.1): a debit raises a
   * receivable component and lowers a payable one (DTIP, VAT on commission).
   *
   * @param component component
   * @param debitAmount line amount, debit positive
   * @return change of the component's balance
   */
  public static BigDecimal ledgerChange(LedgerComponent component, BigDecimal debitAmount) {
    boolean payable =
        component == LedgerComponent.DTIP || component == LedgerComponent.COMMISSION_VAT;
    return payable ? debitAmount.negate() : debitAmount;
  }
}
