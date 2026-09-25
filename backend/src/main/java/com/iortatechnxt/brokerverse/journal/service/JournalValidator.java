package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.coa.domain.NegativeBalancePolicy;
import com.iortatechnxt.brokerverse.coa.service.PostingContext;
import com.iortatechnxt.brokerverse.coa.service.PostingEligibilityService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates a journal batch before submission and again before posting.
 *
 * <p>Checks, in order: organisation (active company/branches), accounting period, value-date
 * window, line count and balance, account posting controls, currencies and dimensions, and for
 * manual journals the negative balance control (FRBS 2.5.4). All violations are reported together.
 */
@Component
public class JournalValidator {

  private static final int MIN_LINES = 2;

  private final OrganizationService organization;
  private final PeriodService periods;
  private final PostingEligibilityService eligibility;
  private final DimensionService dimensions;
  private final NegativeBalanceCheck negativeBalances;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param organization organization service
   * @param periods period service
   * @param eligibility account eligibility rules
   * @param dimensions dimension service
   * @param negativeBalances negative balance control (FRBS 2.5.4)
   * @param clock clock
   */
  public JournalValidator(
      OrganizationService organization,
      PeriodService periods,
      PostingEligibilityService eligibility,
      DimensionService dimensions,
      NegativeBalanceCheck negativeBalances,
      Clock clock) {
    this.organization = organization;
    this.periods = periods;
    this.eligibility = eligibility;
    this.dimensions = dimensions;
    this.negativeBalances = negativeBalances;
    this.clock = clock;
  }

  /**
   * Validates a batch; throws with all violations when invalid.
   *
   * @param batch batch
   * @param makerRoles role codes of the maker (for GL access codes)
   */
  public void validate(JournalBatch batch, Set<String> makerRoles) {
    Company company = organization.requireActiveCompany(batch.getCompanyId());
    organization.requireActiveBranch(batch.getBranchId());
    boolean manual = !batch.getJournalType().isSystemGenerated();
    periods.requirePostingPeriod(
        batch.getCompanyId(), batch.getValueDate(), batch.getJournalType().isPrivileged());
    List<String> errors = new ArrayList<>();
    if (manual) {
      checkValueDateWindow(company, batch.getValueDate(), errors);
    }
    if (batch.getLines().size() < MIN_LINES) {
      errors.add("A journal needs at least one debit and one credit line");
    }
    if (!batch.isBalanced()) {
      errors.add(
          "Journal is not balanced: debit "
              + batch.getTotalDebit()
              + ", credit "
              + batch.getTotalCredit());
    }
    for (JournalLine line : batch.getLines()) {
      errors.addAll(lineErrors(batch, line, manual, makerRoles));
    }
    if (manual && errors.isEmpty()) {
      negativeBalances.check(batch).stream()
          .filter(f -> f.policy() == NegativeBalancePolicy.BLOCK)
          .map(NegativeBalanceCheck.Finding::message)
          .forEach(errors::add);
    }
    if (!errors.isEmpty()) {
      throw new BusinessRuleException("JOURNAL_INVALID", String.join("; ", errors));
    }
  }

  /**
   * Non-blocking warnings of a manual journal (FRBS 2.5.5, 2.8.4): accounts with policy WARN left
   * with a negative balance. Shown to the maker before submission and to the checker before
   * approval; they never stop the posting.
   *
   * @param batch batch
   * @return warning messages, empty when none
   */
  public List<String> warnings(JournalBatch batch) {
    if (batch.getJournalType().isSystemGenerated() || batch.getLines().isEmpty()) {
      return List.of();
    }
    return negativeBalances.check(batch).stream()
        .filter(f -> f.policy() == NegativeBalancePolicy.WARN)
        .map(NegativeBalanceCheck.Finding::message)
        .toList();
  }

  private List<String> lineErrors(
      JournalBatch batch, JournalLine line, boolean manual, Set<String> makerRoles) {
    if (!line.getBranchId().equals(batch.getBranchId())) {
      organization.requireActiveBranch(line.getBranchId());
    }
    dimensions.validateOptional(
        batch.getCompanyId(), DimensionType.COST_CENTER, line.getCostCenter());
    dimensions.validateOptional(
        batch.getCompanyId(), DimensionType.BUSINESS_LINE, line.getBusinessLine());
    PostingContext ctx =
        new PostingContext(
            line.getBranchId(),
            line.getCurrency(),
            batch.getValueDate(),
            manual,
            makerRoles,
            line.getCostCenter() != null,
            line.getBusinessLine() != null,
            line.getPartyCode() != null);
    List<String> errors = new ArrayList<>(eligibility.violations(line.getAccount(), ctx));
    if (!line.getAccount().getCompanyId().equals(batch.getCompanyId())) {
      errors.add("Account " + line.getAccount().getCode() + " belongs to another company");
    }
    return errors.stream().map(e -> "Line " + line.getLineNo() + ": " + e).toList();
  }

  private void checkValueDateWindow(Company company, LocalDate valueDate, List<String> errors) {
    LocalDate today = LocalDate.now(clock);
    if (valueDate.isBefore(today.minusDays(company.getBackValueDays()))) {
      errors.add(
          "Value date "
              + valueDate
              + " is earlier than the permitted "
              + company.getBackValueDays()
              + " back-dated days");
    }
    if (valueDate.isAfter(today.plusDays(company.getForwardValueDays()))) {
      errors.add(
          "Value date "
              + valueDate
              + " is later than the permitted "
              + company.getForwardValueDays()
              + " forward-dated days");
    }
  }
}
