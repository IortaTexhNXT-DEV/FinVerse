package com.iortatechnxt.finverse.journal.demo;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.finverse.journal.service.SystemJournalService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.BranchRepository;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Demo profile only: opening balances of the demo company's PHP bank accounts on 1 January 2026, so
 * that the cash drawn by the demo investments, supplier payments and claims never overdraws them,
 * for the company or for a branch. Runs before every other demo runner ({@code @Order(5)};
 * underwriting is 10).
 *
 * <p>One {@link JournalType#OPENING} journal per branch against 3500 Retained Earnings (the
 * opening-balance account of the fixed asset and investment take-on): 1111 BDO Current
 * 150,000,000.00 in total (head office 145,000,000.00, Cebu 2,000,000.00, Davao 3,000,000.00, the
 * branches paying their own claims and expenses from it) and 1112 BPI Savings 20,000,000.00 at the
 * head office. Every line carries the reference {@value #REFERENCE}, so the demo bank statement
 * shows one balance brought forward matched to all of them. The USD account 1113 is left as it is.
 *
 * <p>Posted as system journals by the finance manager, like the fixed asset and investment take-on
 * (also OPENING journals): {@code JournalEntryService} accepts only MANUAL, ADJUSTMENT and ACCRUAL
 * journals for maker-checker entry, and a manual journal may not be back-valued beyond the
 * company's window (45 days), so a take-on dated 1 January cannot go through maker-checker. The
 * loader lives in {@code journal} (which already depends on organization) and sets the user itself:
 * {@code underwriting.demo.DemoUserContext} would make journal depend on underwriting, which
 * depends on journal through the accounting engine (a cycle).
 *
 * <p>Idempotent: {@link SystemJournalService} returns the posted journal of the same source
 * reference. When a posting is refused (e.g. January already closed in an existing demo database)
 * the loader logs a warning and leaves that branch unchanged.
 */
@Component
@Profile("demo")
@Order(5)
public class OpeningBalanceDemoData implements ApplicationRunner {

  /** Take-on date of the demo company. */
  public static final LocalDate TAKE_ON = LocalDate.of(2026, 1, 1);

  /** Source module of the opening journals. */
  public static final String SOURCE = "OPENING_DEMO";

  /** Reference of the opening lines; the source reference of a branch's journal adds its code. */
  public static final String REFERENCE = "OPENING-BANK-2026";

  private static final Logger LOG = LoggerFactory.getLogger(OpeningBalanceDemoData.class);
  private static final String COMPANY = "FVI";
  private static final String USER = "fmanager";
  private static final String PHP = "PHP";
  private static final String RETAINED_EARNINGS = "3500";
  private static final String BDO = "1111";
  private static final List<Opening> OPENINGS =
      List.of(
          new Opening("HO", BDO, new BigDecimal("145000000.00")),
          new Opening("HO", "1112", new BigDecimal("20000000.00")),
          new Opening("CEB", BDO, new BigDecimal("2000000.00")),
          new Opening("DVO", BDO, new BigDecimal("3000000.00")));

  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final SystemJournalService journals;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param branches branches
   * @param journals system journal posting
   */
  public OpeningBalanceDemoData(
      CompanyRepository companies, BranchRepository branches, SystemJournalService journals) {
    this.companies = companies;
    this.branches = branches;
    this.journals = journals;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> company = companies.findByCode(COMPANY);
    if (company.isEmpty()) {
      return;
    }
    Map<String, List<Opening>> byBranch =
        OPENINGS.stream()
            .collect(
                Collectors.groupingBy(Opening::branch, LinkedHashMap::new, Collectors.toList()));
    byBranch.forEach(
        (code, openings) ->
            branches
                .findByCompanyIdAndCode(company.get().getId(), code)
                .ifPresent(branch -> post(company.get(), branch, openings)));
  }

  private void post(Company company, Branch branch, List<Opening> openings) {
    try {
      JournalBatch batch = as(USER, () -> journals.post(request(company, branch, openings)));
      LOG.info("Demo bank opening balances {}: journal {}", branch.getCode(), batch.getBatchNo());
    } catch (BusinessRuleException ex) {
      LOG.warn("Demo bank opening balances {} not posted: {}", branch.getCode(), ex.getMessage());
    }
  }

  private static SystemJournalRequest request(
      Company company, Branch branch, List<Opening> openings) {
    BigDecimal total =
        openings.stream().map(Opening::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    List<JournalLineRequest> lines = new ArrayList<>();
    openings.forEach(o -> lines.add(line(o.account(), BalanceSide.DEBIT, o.amount())));
    lines.add(line(RETAINED_EARNINGS, BalanceSide.CREDIT, total));
    return new SystemJournalRequest(
        company.getId(),
        branch.getId(),
        JournalType.OPENING,
        TAKE_ON,
        PHP,
        "Opening bank balances brought forward",
        REFERENCE,
        SOURCE,
        REFERENCE + "-" + branch.getCode(),
        lines);
  }

  private static JournalLineRequest line(String account, BalanceSide side, BigDecimal amount) {
    return new JournalLineRequest(
        account,
        side,
        amount,
        null,
        null,
        null,
        null,
        null,
        null,
        REFERENCE,
        "Opening balance " + TAKE_ON);
  }

  private static <T> T as(String user, Supplier<T> action) {
    SecurityContext previous = SecurityContextHolder.getContext();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    SecurityContextHolder.setContext(context);
    try {
      return action.get();
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }

  /**
   * Opening balance of one bank account at one branch.
   *
   * @param branch branch code
   * @param account GL bank account
   * @param amount debit balance in PHP
   */
  private record Opening(String branch, String account, BigDecimal amount) {}
}
