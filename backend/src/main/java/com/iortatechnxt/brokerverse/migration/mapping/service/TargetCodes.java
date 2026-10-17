package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.TargetKind;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The active codes of a target domain, read to check the MAP entries of a code map version before
 * it is approved (FR-DM-011 R4: "the target code of a MAP entry must exist and be active in BIBS
 * when the version is approved") and to check that the mapped codes of a batch still exist. The
 * codes are read with constant SQL from the owning modules' master tables; nothing is written.
 */
@Component
public class TargetCodes {

  /** Target of the legacy control accounts in the GL_ACCOUNT map. */
  public static final String CLEARING = "CLEARING";

  private static final String ACTIVE = "ACTIVE";

  private static final Map<TargetKind, Query> QUERIES = new EnumMap<>(TargetKind.class);

  static {
    QUERIES.put(
        TargetKind.INSURER,
        new Query("select party_code from cat_insurer where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.PRODUCT,
        new Query("select code from cat_product where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.LINE,
        new Query("select code from cat_product_line where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.COVER_TYPE,
        new Query("select code from cat_cover_type where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.BRANCH,
        new Query("select code from org_branch where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.SALES_UNIT,
        new Query("select code from cat_sales_unit where record_status = ?", ACTIVE));
    QUERIES.put(
        TargetKind.USER,
        new Query("select username from sec_user where enabled = ?", Boolean.TRUE));
    QUERIES.put(
        TargetKind.CURRENCY,
        new Query("select code from cur_currency where active = ?", Boolean.TRUE));
    QUERIES.put(
        TargetKind.PACKAGE,
        new Query(
            "select product_code || ' v' || version_no from cat_product_version"
                + " where status in ('RELEASED', ?)",
            "FOR_VALIDATION"));
    QUERIES.put(
        TargetKind.COST_CENTER,
        new Query("select code from dim_value where dimension_type = ? and active", "COST_CENTER"));
  }

  private final JdbcTemplate jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc JDBC
   */
  public TargetCodes(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The codes of the given set that do not exist or are not active in its target domain.
   *
   * @param set code map set
   * @param codes target codes
   * @return missing codes (empty for sets whose target is not checked)
   */
  @Transactional(readOnly = true)
  public Set<String> missing(CodeMapSet set, Collection<String> codes) {
    Set<String> wanted = new HashSet<>(codes);
    wanted.remove(null);
    if (wanted.isEmpty()) {
      return Set.of();
    }
    Set<String> known = known(set.getTargetKind(), set.getTargetRef());
    if (known == null) {
      return Set.of();
    }
    Set<String> missing = new HashSet<>();
    for (String code : wanted) {
      if (!known.contains(code) && !known.contains(code.toUpperCase(Locale.ROOT))) {
        missing.add(code);
      }
    }
    return missing;
  }

  /**
   * The active codes of a target kind, null when the kind is not checked.
   *
   * @param kind target kind
   * @param ref LOV type of a LOV set
   * @return codes
   */
  @Transactional(readOnly = true)
  public Set<String> known(TargetKind kind, String ref) {
    if (kind == TargetKind.LOV) {
      return codes(
          "select code from lov_value where type_code = ? and record_status = 'ACTIVE'", ref);
    }
    if (kind == TargetKind.GL_ACCOUNT) {
      return glAccounts();
    }
    Query query = QUERIES.get(kind);
    return query == null ? null : codes(query.sql(), query.arg());
  }

  private Set<String> glAccounts() {
    Set<String> codes =
        codes(
            "select code from coa_account where record_status = ? and postable and not frozen",
            ACTIVE);
    codes.add(CLEARING);
    return codes;
  }

  private Set<String> codes(String sql, Object arg) {
    List<String> rows = jdbc.queryForList(sql, String.class, arg);
    return new HashSet<>(rows);
  }

  private record Query(String sql, Object arg) {}
}
