package com.iortatechnxt.brokerverse.accounting.api.dto;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingRuleLine;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Rule line in requests and responses.
 *
 * @param side debit or credit
 * @param accountCode GL account code or {@code @ROLE}
 * @param amountComponent event amount component
 * @param partyLine carry the event party on the line
 * @param narration optional narration
 */
public record RuleLineDto(
    @NotNull BalanceSide side,
    @NotBlank @Size(max = 30) String accountCode,
    @NotBlank @Size(max = 40) String amountComponent,
    boolean partyLine,
    @Size(max = 200) String narration) {

  /**
   * Maps an entity.
   *
   * @param l line
   * @return dto
   */
  public static RuleLineDto from(AccountingRuleLine l) {
    return new RuleLineDto(
        l.getSide(), l.getAccountCode(), l.getAmountComponent(), l.isPartyLine(), l.getNarration());
  }

  /**
   * Creates the entity line.
   *
   * @return line
   */
  public AccountingRuleLine toEntity() {
    return new AccountingRuleLine(side, accountCode, amountComponent, partyLine, narration);
  }
}
