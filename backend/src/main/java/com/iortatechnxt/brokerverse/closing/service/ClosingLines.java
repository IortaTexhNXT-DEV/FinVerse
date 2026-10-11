package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Journal line helpers shared by the period-end processes. */
final class ClosingLines {

  private ClosingLines() {}

  /**
   * Line request with an explicit rate and branch.
   *
   * @param account account code
   * @param side side
   * @param value amount, currency and rate
   * @param branchId posting branch
   * @param dims cost centre and line of business (may be null values)
   * @param narration narration
   * @return request
   */
  static JournalLineRequest line(
      String account, BalanceSide side, Value value, Long branchId, Dims dims, String narration) {
    return new JournalLineRequest(
        account,
        side,
        value.amount(),
        value.currency(),
        value.rate(),
        branchId,
        dims.costCenter(),
        dims.businessLine(),
        null,
        null,
        narration);
  }

  /**
   * Copy of a posted line with the opposite side (auto-reversal), same amount, rate and dimensions.
   *
   * @param l posted line
   * @return request
   */
  static JournalLineRequest reversed(JournalLine l) {
    return new JournalLineRequest(
        l.getAccount().getCode(),
        l.getSide().opposite(),
        l.getAmount(),
        l.getCurrency(),
        l.getExchangeRate(),
        l.getBranchId(),
        l.getCostCenter(),
        l.getBusinessLine(),
        l.getPartyCode(),
        l.getReference(),
        l.getNarration());
  }

  /**
   * Head office (or first active) branch, used as the originating branch of period-end journals.
   *
   * @param branches branches of the company
   * @return branch
   */
  static Branch headOffice(List<Branch> branches) {
    return branches.stream()
        .filter(Branch::isActive)
        .min(Comparator.comparing((Branch b) -> !b.isHeadOffice()).thenComparing(Branch::getCode))
        .orElseThrow(() -> new BusinessRuleException("NO_BRANCH", "Company has no active branch"));
  }

  /**
   * Line amount.
   *
   * @param amount positive amount in line currency
   * @param currency line currency
   * @param rate exchange rate to base (null for base currency)
   */
  record Value(BigDecimal amount, String currency, BigDecimal rate) {}

  /**
   * Line dimensions.
   *
   * @param costCenter cost centre or null
   * @param businessLine line of business or null
   */
  record Dims(String costCenter, String businessLine) {
    static final Dims NONE = new Dims(null, null);
  }
}
