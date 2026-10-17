package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import java.util.List;
import java.util.Optional;

/**
 * The CSF status mapping (BRCSF-005 / 5.001, CSF-EM07; FRS section 5.1) as data: rows of the list
 * CSF_STATUS_MAP in their order, each an account stage ({@code ANY} for every stage) optionally
 * followed by {@code _OUTSTANDING} (premium receivable not fully paid) or {@code _EXPIRED} (policy
 * period ended) and the CSF status it gives. The first matching row applies. Pure logic.
 *
 * @param rules rows in evaluation order
 */
public record CsfStatusRules(List<Rule> rules) {

  /** Code of a row that applies to every stage. */
  public static final String ANY = "ANY";

  private static final String OUTSTANDING = "_OUTSTANDING";
  private static final String EXPIRED = "_EXPIRED";

  /** Defensive copy. */
  public CsfStatusRules {
    rules = List.copyOf(rules);
  }

  /**
   * Rules parsed from list rows.
   *
   * @param rows code and CSF status of each row, in order
   * @return rules
   */
  public static CsfStatusRules of(List<Row> rows) {
    return new CsfStatusRules(
        rows.stream().filter(r -> r.status() != null).map(CsfStatusRules::parse).toList());
  }

  private static Rule parse(Row row) {
    String code = row.code();
    if (code.endsWith(OUTSTANDING)) {
      return new Rule(stage(code, OUTSTANDING), Condition.OUTSTANDING, row.status());
    }
    if (code.endsWith(EXPIRED)) {
      return new Rule(stage(code, EXPIRED), Condition.EXPIRED, row.status());
    }
    return new Rule(code, Condition.NONE, row.status());
  }

  private static String stage(String code, String suffix) {
    return code.substring(0, code.length() - suffix.length());
  }

  /**
   * The CSF status of an account.
   *
   * @param stage account stage
   * @param facts payment and period facts
   * @return status code, empty when no row applies
   */
  public Optional<String> statusOf(AccountStatus stage, Facts facts) {
    return rules.stream().filter(r -> r.applies(stage, facts)).map(Rule::status).findFirst();
  }

  /**
   * What a mapping row needs to know besides the stage.
   *
   * @param outstanding the premium receivable is not fully paid
   * @param expired the policy period has ended
   */
  public record Facts(boolean outstanding, boolean expired) {}

  /**
   * A row of the list.
   *
   * @param code code
   * @param status CSF status (group above the row)
   */
  public record Row(String code, String status) {}

  /** Extra condition of a row. */
  public enum Condition {
    /** Stage only. */
    NONE,
    /** Premium outstanding. */
    OUTSTANDING,
    /** Policy period ended. */
    EXPIRED
  }

  /**
   * A parsed row.
   *
   * @param stage stage code or {@link #ANY}
   * @param condition extra condition
   * @param status CSF status
   */
  public record Rule(String stage, Condition condition, String status) {

    boolean applies(AccountStatus accountStage, Facts facts) {
      boolean stageMatches = ANY.equals(stage) || accountStage.name().equals(stage);
      return stageMatches
          && switch (condition) {
            case NONE -> true;
            case OUTSTANDING -> facts.outstanding();
            case EXPIRED -> facts.expired();
          };
    }
  }
}
