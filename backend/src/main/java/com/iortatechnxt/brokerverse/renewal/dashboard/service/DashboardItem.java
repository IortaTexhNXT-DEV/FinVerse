package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * One account of a dashboard (a renewal account, or a New Business account in the pipeline and the
 * processing dashboard), read once per request: every figure and every drill-down list of the
 * dashboard is computed from the same items, so a list holds exactly the accounts a figure counts.
 */
public final class DashboardItem {

  /** Kind of a renewal account. */
  public static final String RENEWAL = "RENEWAL";

  /** Kind of a New Business account. */
  public static final String NEW_BUSINESS = "NEW_BUSINESS";

  private final Map<String, Object> values;

  /**
   * Wraps a row of the dashboard query.
   *
   * @param row column values by name
   */
  public DashboardItem(Map<String, Object> row) {
    this.values = new HashMap<>(row);
  }

  /**
   * A text value.
   *
   * @param name column
   * @return value or null
   */
  public String text(String name) {
    Object v = values.get(name);
    return v == null ? null : v.toString();
  }

  /**
   * An amount, zero when absent.
   *
   * @param name column
   * @return amount
   */
  public BigDecimal amount(String name) {
    Object v = values.get(name);
    if (v instanceof BigDecimal b) {
      return b;
    }
    return v instanceof Number n ? new BigDecimal(n.toString()) : BigDecimal.ZERO;
  }

  /**
   * A date value.
   *
   * @param name column
   * @return date or null
   */
  public LocalDate date(String name) {
    Object v = values.get(name);
    if (v instanceof Date d) {
      return d.toLocalDate();
    }
    if (v instanceof LocalDate d) {
      return d;
    }
    Instant i = instant(name);
    return i == null ? null : BusinessClock.dateOf(i);
  }

  /**
   * A time value.
   *
   * @param name column
   * @return instant or null
   */
  public Instant instant(String name) {
    Object v = values.get(name);
    if (v instanceof Timestamp t) {
      return t.toInstant();
    }
    if (v instanceof OffsetDateTime o) {
      return o.toInstant();
    }
    return v instanceof Instant i ? i : null;
  }

  /**
   * A yes / no value.
   *
   * @param name column
   * @return true when set
   */
  public boolean flag(String name) {
    return Boolean.TRUE.equals(values.get(name));
  }

  /**
   * Sets a value computed after the query (pipeline stage, category, tier, labels).
   *
   * @param name name
   * @param value value
   */
  public void put(String name, Object value) {
    values.put(name, value);
  }

  /**
   * A value as read or computed.
   *
   * @param name name
   * @return value or null
   */
  public Object get(String name) {
    return values.get(name);
  }

  /**
   * Whether the item is a renewal account.
   *
   * @return true for a renewal
   */
  public boolean renewal() {
    return RENEWAL.equals(text("kind"));
  }

  /**
   * The stage of a renewal, or the status of a New Business account.
   *
   * @return code
   */
  public String stage() {
    return text("stage");
  }

  /**
   * Whether the renewal is still open (not renewed, not closed).
   *
   * @return true when open
   */
  public boolean open() {
    String s = stage();
    return renewal() ? !"RENEWED".equals(s) && !"CLOSED".equals(s) : !"BOOKED".equals(s);
  }
}
