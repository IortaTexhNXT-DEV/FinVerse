package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;

/**
 * Detached, immutable view of a chart-of-accounts entry used by the finance reports.
 *
 * @param id account id
 * @param code account code
 * @param name account name
 * @param level hierarchy level
 * @param parentId parent account id or null
 * @param accountClass class
 * @param postable whether entries can be posted to it
 * @param controlAccount whether it is a sub-ledger control account
 * @param bankOrCash whether its category is flagged as bank / cash
 * @param reportGroup statement caption (report group) or null
 */
public record AccountNode(
    Long id,
    String code,
    String name,
    AccountLevel level,
    Long parentId,
    AccountClass accountClass,
    boolean postable,
    boolean controlAccount,
    boolean bankOrCash,
    String reportGroup) {

  /**
   * Copies the attributes the reports need from an account entity.
   *
   * @param a account
   * @return node
   */
  public static AccountNode of(GlAccount a) {
    return new AccountNode(
        a.getId(),
        a.getCode(),
        a.getName(),
        a.getLevel(),
        a.getParent() == null ? null : a.getParent().getId(),
        a.getAccountClass(),
        a.isPostable(),
        a.isControlAccount(),
        a.getCategory() != null && a.getCategory().isBankCategory(),
        a.getReportGroup());
  }

  /**
   * Code and name for captions.
   *
   * @return "code - name"
   */
  public String caption() {
    return code + " - " + name;
  }
}
