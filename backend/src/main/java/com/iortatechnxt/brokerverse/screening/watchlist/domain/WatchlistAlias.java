package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * An alias of a watchlist entry (AKA, FKA, spelling); replaced as a whole when a change applies.
 */
@Entity
@Table(name = "scr_watchlist_alias")
public class WatchlistAlias extends BaseEntity {

  @Column(name = "entry_id", nullable = false, updatable = false)
  private Long entryId;

  @Column(name = "alias_name", nullable = false, length = 300, updatable = false)
  private String aliasName;

  @Enumerated(EnumType.STRING)
  @Column(name = "alias_type", nullable = false, length = 20, updatable = false)
  private AliasType aliasType;

  /** For JPA. */
  protected WatchlistAlias() {}

  /**
   * Creates an alias.
   *
   * @param entryId entry
   * @param aliasName alias
   * @param aliasType kind
   */
  public WatchlistAlias(Long entryId, String aliasName, AliasType aliasType) {
    this.entryId = entryId;
    this.aliasName = aliasName;
    this.aliasType = aliasType;
  }

  public Long getEntryId() {
    return entryId;
  }

  public String getAliasName() {
    return aliasName;
  }

  public AliasType getAliasType() {
    return aliasType;
  }
}
