package com.iortatechnxt.brokerverse.receivables.seed;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Company, branches and helpers shared by the receivables seed data builders.
 *
 * @param companyId seed company
 * @param headOfficeId head office branch
 * @param branchIds all branches (head office first)
 */
public record SeedContext(Long companyId, Long headOfficeId, List<Long> branchIds) {

  /** Maker of seed receipts. */
  public static final String MAKER = "accountant";

  /** Checker of seed receipts. */
  public static final String CHECKER = "checker";

  /** Finance manager (reconciliation). */
  public static final String MANAGER = "fmanager";

  /** Main seed bank account (BDO current account, PHP). */
  public static final String BANK = "1111";

  /** Second PHP bank account. */
  public static final String SAVINGS = "1112";

  /** Dollar bank account. */
  public static final String DOLLAR_BANK = "1113";

  /** Last value date used by the seed data (all value dates stay within Jan-Sep 2026). */
  public static final LocalDate LAST_DATE = LocalDate.of(2026, 9, 22);

  /** Every fourth document is raised by a branch other than the head office. */
  private static final int BRANCH_CYCLE = 4;

  /** Canonical constructor copying the list. */
  public SeedContext {
    branchIds = List.copyOf(branchIds);
  }

  /**
   * Branch used for the n-th document (mostly head office).
   *
   * @param n sequence
   * @return branch id
   */
  public Long branch(int n) {
    return n % BRANCH_CYCLE == BRANCH_CYCLE - 1
        ? branchIds.get(n % branchIds.size())
        : headOfficeId;
  }

  /**
   * Runs an action as a SIT/UAT user (services record the user as maker / checker).
   *
   * @param username user
   * @param action action
   * @param <T> result type
   * @return result
   */
  public static <T> T as(String username, Supplier<T> action) {
    var previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(username, null, List.of()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
