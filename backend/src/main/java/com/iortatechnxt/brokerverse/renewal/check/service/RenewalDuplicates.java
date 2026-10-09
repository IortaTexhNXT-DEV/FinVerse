package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Duplicate checking of renewal accounts (BDOI Renewal FRS FRRN.006.01, Annex B): the renewal
 * accounts of the company that share the configured criteria of the product line - client number,
 * risk code, PN and expiring policy number ({@value #CRITERIA}; by default the expiring policy
 * number and the PN). All criteria matching is an exact duplicate; at least {@value #MIN_MATCH} of
 * them (one by default) a potential duplicate. A match with a cancelled or closed renewal account
 * is reported as such and never blocks.
 */
@Component
public class RenewalDuplicates {

  /** Parameter: criteria per line, e.g. {@code *:CLIENT+RISK_CODE+EXPIRING_POLICY;PROPERTY:...}. */
  private static final Map<String, String> COLUMNS =
      Map.of("CLIENT", "client_code", "RISK_CODE", "product_code", "PN", "pn_nos");

  public static final String CRITERIA = "RNW_DUPLICATE_CRITERIA";

  /** Parameter: criteria that must match for a potential duplicate. */
  public static final String MIN_MATCH = "RNW_DUPLICATE_MIN_MATCH";

  /** Default criteria: the expiring policy number or the PN, for every line. */
  public static final String DEFAULT = "*:EXPIRING_POLICY+PN";

  private static final int DEFAULT_MIN = 1;

  private static final String SQL =
      "select c.renewal_ref, c.stage, c.closed_as, c.client_code, c.product_code, c.pn_nos,"
          + " c.expiring_policy_no from rnw_candidate c where c.company_id = :company"
          + " and c.renewal_ref <> :ref and (c.client_code = :client or c.expiring_policy_no = :policy"
          + " or (cast(:pn as varchar) is not null and c.pn_nos = :pn))";

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;

  /**
   * Creates the checking.
   *
   * @param jdbc SQL
   * @param parameters system parameters
   */
  public RenewalDuplicates(NamedParameterJdbcTemplate jdbc, SystemParameterService parameters) {
    this.jdbc = jdbc;
    this.parameters = parameters;
  }

  /**
   * The duplicates of an account.
   *
   * @param companyId company
   * @param subject the account's criteria
   * @return matches, exact ones first
   */
  public List<Match> of(Long companyId, Subject subject) {
    Set<String> criteria = criteriaOf(subject.lineCode());
    int min = Math.min(parameters.intValue(MIN_MATCH, DEFAULT_MIN), criteria.size());
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("company", companyId)
            .addValue("ref", Objects.toString(subject.ref(), ""))
            .addValue("client", Objects.toString(subject.clientCode(), ""))
            .addValue("policy", Objects.toString(subject.policyNo(), ""))
            .addValue("pn", subject.pnNos());
    List<Match> matches = new ArrayList<>();
    for (Map<String, Object> r : jdbc.queryForList(SQL, args)) {
      List<String> matched = matched(criteria, subject, r);
      if (matched.size() >= Math.max(min, 1)) {
        boolean cancelled =
            "CLOSED".equals(r.get("stage")) && !"RENEWED".equals(r.get("closed_as"));
        matches.add(
            new Match(
                (String) r.get("renewal_ref"),
                matched.size() == criteria.size(),
                cancelled,
                matched));
      }
    }
    matches.sort((a, b) -> Boolean.compare(b.exact(), a.exact()));
    return matches;
  }

  private static List<String> matched(Set<String> criteria, Subject s, Map<String, Object> r) {
    List<String> matched = new ArrayList<>();
    for (String c : criteria) {
      Object ours = ours(c, s);
      if (ours != null && ours.equals(r.get(COLUMNS.getOrDefault(c, "expiring_policy_no")))) {
        matched.add(c);
      }
    }
    return matched;
  }

  private static Object ours(String criterion, Subject s) {
    return switch (criterion) {
      case "CLIENT" -> s.clientCode();
      case "RISK_CODE" -> s.productCode();
      case "PN" -> s.pnNos();
      default -> s.policyNo();
    };
  }

  /**
   * The criteria of a product line.
   *
   * @param line line code
   * @return criteria
   */
  Set<String> criteriaOf(String line) {
    String text = parameters.text(CRITERIA, DEFAULT);
    Set<String> any = Set.of("EXPIRING_POLICY", "PN");
    for (String part : text.split(";")) {
      String[] kv = part.split(":", 2);
      if (kv.length == 2) {
        Set<String> values =
            Arrays.stream(kv[1].split("\\+"))
                .map(String::strip)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toSet());
        if (kv[0].strip().equals(line)) {
          return values;
        }
        if ("*".equals(kv[0].strip())) {
          any = values;
        }
      }
    }
    return any;
  }

  /**
   * The criteria of an account.
   *
   * @param ref its renewal reference (excluded), or null
   * @param lineCode product line
   * @param clientCode client number
   * @param productCode risk code
   * @param pnNos PN numbers
   * @param policyNo expiring policy number
   */
  public record Subject(
      String ref,
      String lineCode,
      String clientCode,
      String productCode,
      String pnNos,
      String policyNo) {}

  /**
   * A matching renewal account.
   *
   * @param ref its renewal reference (the link)
   * @param exact whether every criterion matches
   * @param cancelled whether it is cancelled or closed without renewal (never blocks)
   * @param criteria the criteria that match
   */
  public record Match(String ref, boolean exact, boolean cancelled, List<String> criteria) {

    /** Defensive copy. */
    public Match {
      criteria = List.copyOf(criteria);
    }
  }
}
