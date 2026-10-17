package com.iortatechnxt.brokerverse.migration.mapping.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The unmapped legacy codes found by the validations (FR-DM-012; report MIG-UNMAPPED-CODES): per
 * code map set, source system and legacy code, the rows that use it and sample legacy keys, from
 * the open lookup issues of the batches not yet loaded.
 */
@Service
@Transactional(readOnly = true)
public class UnmappedCodes {

  private static final Pattern SET = Pattern.compile("^Code .* of (\\S+) is not mapped$");
  private static final int SAMPLES = 3;

  private final JdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC
   */
  public UnmappedCodes(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The unmapped codes of a company.
   *
   * @param companyId company
   * @return codes by set, source and code
   */
  public List<Code> list(Long companyId) {
    Map<String, Code> out = new LinkedHashMap<>();
    jdbc.query(
        "select i.message, i.value, i.severity, e.source_system, r.legacy_key from mig_issue i"
            + " join mig_stage_row r on r.id = i.stage_row_id join mig_extract e on e.id = r.extract_id"
            + " join mig_batch b on b.id = i.batch_id"
            + " where e.company_id = ? and i.rule_code = 'DQ-003' and i.resolution = 'OPEN'"
            + " and b.status in ('PLANNED', 'VALIDATED') order by i.id",
        rs -> {
          Matcher m = SET.matcher(rs.getString("message"));
          if (!m.matches()) {
            return;
          }
          String set = m.group(1);
          String source = rs.getString("source_system");
          String code = rs.getString("value");
          Code c =
              out.computeIfAbsent(
                  set + "|" + source + "|" + code, k -> new Code(set, source, code));
          c.add(rs.getString("legacy_key"), "ERROR".equals(rs.getString("severity")));
        },
        companyId);
    return new ArrayList<>(out.values());
  }

  /** An unmapped code with its rows and sample keys. */
  public static final class Code {
    private final String setCode;
    private final String sourceSystem;
    private final String legacyCode;
    private long rows;
    private boolean error;
    private final List<String> samples = new ArrayList<>();

    Code(String setCode, String sourceSystem, String legacyCode) {
      this.setCode = setCode;
      this.sourceSystem = sourceSystem;
      this.legacyCode = legacyCode;
    }

    void add(String key, boolean isError) {
      rows++;
      error |= isError;
      if (samples.size() < SAMPLES) {
        samples.add(key);
      }
    }

    public String setCode() {
      return setCode;
    }

    public String sourceSystem() {
      return sourceSystem;
    }

    public String legacyCode() {
      return legacyCode;
    }

    public long rows() {
      return rows;
    }

    public boolean error() {
      return error;
    }

    public String sampleKeys() {
      return String.join(", ", samples);
    }
  }
}
