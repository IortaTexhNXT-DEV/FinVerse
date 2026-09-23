package com.iortatechnxt.finverse.finreport.service;

import com.iortatechnxt.finverse.coa.domain.AccountLevel;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Chart-of-accounts hierarchy used to roll balances up to <b>main accounts</b>.
 *
 * <p>Definitions used by every finance report:
 *
 * <ul>
 *   <li><b>Main account</b> = the nearest ancestor-or-self at level {@link AccountLevel#MAIN} (the
 *       PREMIA "Main A/c"). SUB and MICRO balances roll up to it. When a branch of the chart has no
 *       MAIN level, the highest non-GROUP ancestor is used so no balance is ever lost.
 *   <li><b>Sub account</b> = a postable leaf below its main account (the account itself when a main
 *       account is directly postable).
 * </ul>
 */
public final class AccountHierarchy {

  /** Guards against corrupt (cyclic) parent chains. */
  private static final int MAX_DEPTH = 32;

  private final Map<Long, AccountNode> byId = new HashMap<>();
  private final Map<String, AccountNode> byCode = new HashMap<>();
  private final Map<Long, AccountNode> mainCache = new HashMap<>();

  /**
   * Builds the hierarchy.
   *
   * @param nodes every account of the company
   */
  public AccountHierarchy(Collection<AccountNode> nodes) {
    nodes.forEach(
        n -> {
          byId.put(n.id(), n);
          byCode.put(n.code(), n);
        });
  }

  /**
   * Finds an account by id.
   *
   * @param id account id
   * @return node
   * @throws IllegalArgumentException when unknown
   */
  public AccountNode node(Long id) {
    AccountNode n = byId.get(id);
    if (n == null) {
      throw new IllegalArgumentException("Unknown account id " + id);
    }
    return n;
  }

  /**
   * Finds an account by code.
   *
   * @param code account code
   * @return node if present
   */
  public Optional<AccountNode> byCode(String code) {
    return Optional.ofNullable(byCode.get(code));
  }

  /**
   * Returns the main account an account rolls up to.
   *
   * @param accountId account (any level)
   * @return main account node
   */
  public AccountNode mainOf(Long accountId) {
    return mainCache.computeIfAbsent(accountId, this::resolveMain);
  }

  /**
   * Whether an account sits below a different main account (a sub account).
   *
   * @param accountId account
   * @return true for SUB/MICRO accounts under a main account
   */
  public boolean isSubAccount(Long accountId) {
    return !mainOf(accountId).id().equals(accountId);
  }

  /**
   * Ids of postable accounts whose main account and own code satisfy the given filters.
   *
   * @param mainFilter filter on the main account
   * @param subFilter filter on the postable account itself
   * @return account ids
   */
  public Set<Long> postableIds(
      Predicate<AccountNode> mainFilter, Predicate<AccountNode> subFilter) {
    return byId.values().stream()
        .filter(AccountNode::postable)
        .filter(n -> mainFilter.test(mainOf(n.id())) && subFilter.test(n))
        .map(AccountNode::id)
        .collect(Collectors.toSet());
  }

  /**
   * Main accounts (level MAIN or top-level roll-up targets of postable accounts), sorted by code.
   *
   * @return main accounts
   */
  public List<AccountNode> mainAccounts() {
    return byId.values().stream()
        .filter(AccountNode::postable)
        .map(n -> mainOf(n.id()))
        .distinct()
        .sorted(Comparator.comparing(AccountNode::code))
        .toList();
  }

  /**
   * All accounts sorted by code.
   *
   * @return nodes
   */
  public List<AccountNode> all() {
    return byId.values().stream().sorted(Comparator.comparing(AccountNode::code)).toList();
  }

  private AccountNode resolveMain(Long accountId) {
    AccountNode start = node(accountId);
    AccountNode highestNonGroup = start;
    AccountNode current = start;
    for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
      if (current.level() == AccountLevel.MAIN) {
        return current;
      }
      if (current.level() != AccountLevel.GROUP) {
        highestNonGroup = current;
      }
      current = current.parentId() == null ? null : byId.get(current.parentId());
    }
    return highestNonGroup;
  }
}
