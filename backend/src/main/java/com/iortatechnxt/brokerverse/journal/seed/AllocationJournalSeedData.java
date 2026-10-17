package com.iortatechnxt.brokerverse.journal.seed;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * SEED PROFILE ONLY: the monthly allocation of the prepaid insurance of the seed company FVI on the
 * seed date - an ADJUSTMENT journal that credits Prepaid Expenses (1601) and debits the expense,
 * keyed by the accountant and authorized by the checker through the journal services - so the
 * Allocation JV Details report has rows from day one (conformance gap G03). Idempotent: nothing is
 * created when an allocation of the seed exists.
 */
@Component
@Profile("seed")
@Order(96)
public class AllocationJournalSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AllocationJournalSeedData.class);
  private static final String COMPANY = "FVI";
  private static final String BRANCH = "HO";
  private static final String MAKER = "accountant";
  private static final String CHECKER = "checker";
  private static final String SOURCE = "SEED_ALLOCATION";
  private static final String PREPAID = "1601";
  private static final String EXPENSE = "5609";
  private static final String COST_CENTER = "FIN";
  private static final BigDecimal MONTHLY = new BigDecimal("12500.00");

  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final JournalBatchRepository batches;
  private final JournalEntryService entries;
  private final JournalAuthorizationService authorization;
  private final UserDetailsService users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param branches branches
   * @param batches journals (idempotency)
   * @param entries manual journals
   * @param authorization journal authorization
   * @param users seed users
   * @param clock clock
   */
  public AllocationJournalSeedData(
      CompanyRepository companies,
      BranchRepository branches,
      JournalBatchRepository batches,
      JournalEntryService entries,
      JournalAuthorizationService authorization,
      UserDetailsService users,
      Clock clock) {
    this.companies = companies;
    this.branches = branches;
    this.batches = batches;
    this.entries = entries;
    this.authorization = authorization;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    companies
        .findByCode(COMPANY)
        .ifPresent(
            company ->
                branches
                    .findByCompanyIdAndCode(company.getId(), BRANCH)
                    .ifPresent(branch -> load(company, branch)));
  }

  private void load(Company company, Branch branch) {
    boolean loaded =
        !batches
            .findByCompanyIdAndSourceModuleAndSourceReferenceStartingWithOrderByIdAsc(
                company.getId(), SOURCE, "ALLOC-")
            .isEmpty();
    if (loaded) {
      return;
    }
    try {
      LocalDate today = BusinessClock.today(clock);
      String reference = "ALLOC-" + today.getYear() + "-" + today.getMonthValue();
      JournalRequest request =
          new JournalRequest(
              company.getId(),
              branch.getId(),
              JournalType.ADJUSTMENT,
              today,
              "PHP",
              "Allocation of the prepaid insurance premium for the month",
              reference,
              List.of(
                  line(EXPENSE, BalanceSide.DEBIT, COST_CENTER, reference),
                  line(PREPAID, BalanceSide.CREDIT, null, reference)));
      JournalBatch draft = as(MAKER, () -> entries.createDraft(request, SOURCE, reference));
      as(MAKER, () -> entries.submit(draft.getId()));
      JournalBatch posted = as(CHECKER, () -> authorization.approve(draft.getId()));
      LOG.info("Seed allocation journal {} posted", posted.getBatchNo());
    } catch (RuntimeException ex) {
      LOG.warn("Seed allocation journal not posted: {}", ex.getMessage());
    }
  }

  private static JournalLineRequest line(
      String account, BalanceSide side, String costCenter, String reference) {
    return new JournalLineRequest(
        account,
        side,
        MONTHLY,
        null,
        null,
        null,
        costCenter,
        null,
        null,
        reference,
        "Prepaid insurance allocated for the month");
  }

  private <T> T as(String user, Supplier<T> action) {
    UserDetails details = users.loadUserByUsername(user);
    SecurityContext previous = SecurityContextHolder.getContext();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            details.getUsername(), null, details.getAuthorities()));
    SecurityContextHolder.setContext(context);
    try {
      return action.get();
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }
}
