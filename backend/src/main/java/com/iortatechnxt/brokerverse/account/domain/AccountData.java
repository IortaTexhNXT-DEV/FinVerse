package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import java.time.LocalDate;
import java.util.List;

/**
 * Maintainable data of an account, with the client and product already resolved by the service.
 *
 * @param client client id, code and name
 * @param product risk code, line, cover type and item kind
 * @param marketSegment market segment (list MARKET_SEGMENT)
 * @param sourceChannel source channel (list SOURCE_CHANNEL)
 * @param insurerCode insurer party code, may be null until placement
 * @param insurerBranch insurer branch code
 * @param periodFrom period start
 * @param periodTo period end
 * @param multiYear multi-year account (BRNB.112)
 * @param termYears term in years
 * @param currency currency (PHP base; foreign allowed)
 * @param paymentArrangement via BDOI or direct to insurer (BRNB.114)
 * @param mortgage mortgagee, loan application and PN numbers
 * @param contact account contact (BRNB.109)
 * @param items risk items
 */
public record AccountData(
    ClientRef client,
    ProductRef product,
    String marketSegment,
    String sourceChannel,
    String insurerCode,
    String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    boolean multiYear,
    int termYears,
    String currency,
    PaymentArrangement paymentArrangement,
    Mortgage mortgage,
    AccountContact contact,
    List<RiskItemData> items) {

  /** Defensive copy. */
  public AccountData {
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * The client of the account.
   *
   * @param id crm client id
   * @param code client (or prospect) code
   * @param name display name
   */
  public record ClientRef(Long id, String code, String name) {}

  /**
   * The product of the account.
   *
   * @param code risk code
   * @param lineCode product line
   * @param coverTypeCode cover type
   * @param itemKind kind of the line's risk items
   */
  public record ProductRef(
      String code, String lineCode, String coverTypeCode, RiskItemKind itemKind) {}

  /**
   * Mortgage block (CLPC billing, Insurance Advice).
   *
   * @param bank mortgagee bank (list MORTGAGEE_BANK)
   * @param loanApplicationNo loan application number
   * @param pnNumbers promissory note numbers
   */
  public record Mortgage(String bank, String loanApplicationNo, List<String> pnNumbers) {

    /** No mortgage. */
    public static final Mortgage NONE = new Mortgage(null, null, List.of());

    /** Defensive copy. */
    public Mortgage {
      pnNumbers = pnNumbers == null ? List.of() : List.copyOf(pnNumbers);
    }
  }
}
