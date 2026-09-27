package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * TSU involvement of an account (BRNB.098): whether a routing rule requires TSU clearance and who
 * cleared it.
 *
 * @param required whether TSU clearance is required
 * @param rule matching routing rule
 * @param clearedBy TSU user who cleared the account
 * @param clearedAt clearance time
 */
@Embeddable
public record TsuClearance(
    @Column(name = "tsu_required", nullable = false) boolean required,
    @Column(name = "tsu_rule", length = 30) String rule,
    @Column(name = "tsu_cleared_by", length = 50) String clearedBy,
    @Column(name = "tsu_cleared_at") Instant clearedAt) {

  /** Not required. */
  public static final TsuClearance NONE = new TsuClearance(false, null, null, null);

  /**
   * Whether the account may pass validation as far as TSU is concerned.
   *
   * @return true when not required or cleared
   */
  public boolean satisfied() {
    return !required || clearedAt != null;
  }
}
