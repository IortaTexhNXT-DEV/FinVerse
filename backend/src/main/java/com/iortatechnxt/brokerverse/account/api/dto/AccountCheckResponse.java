package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.service.AccountCheck;
import com.iortatechnxt.brokerverse.account.service.DuplicateFinding;
import java.util.List;
import java.util.Map;

/**
 * Readiness of an account (wizard review step).
 *
 * @param fieldErrors missing minimum fields
 * @param missingDocuments mandatory documents not uploaded
 * @param duplicates live accounts insuring the same risk
 * @param premiumRated premium computed
 * @param tsuRequired TSU clearance required
 * @param tsuRule matching TSU rule
 * @param tsuReason TSU decision
 * @param tsuCleared TSU cleared
 * @param readyToSubmit whether Marketing may submit
 */
public record AccountCheckResponse(
    Map<String, String> fieldErrors,
    List<String> missingDocuments,
    List<DuplicateFinding> duplicates,
    boolean premiumRated,
    boolean tsuRequired,
    String tsuRule,
    String tsuReason,
    boolean tsuCleared,
    boolean readyToSubmit) {

  /**
   * Maps an entity.
   *
   * @param c entity
   * @return response
   */
  public static AccountCheckResponse from(AccountCheck c) {
    return new AccountCheckResponse(
        c.fieldErrors(),
        c.missingDocuments(),
        c.duplicates(),
        c.premiumRated(),
        c.tsuRequired(),
        c.tsuRule(),
        c.tsuReason(),
        c.tsuCleared(),
        c.readyToSubmit());
  }
}
