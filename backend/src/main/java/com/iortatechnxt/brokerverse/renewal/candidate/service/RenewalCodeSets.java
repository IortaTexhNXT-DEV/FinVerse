package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource.CodeOption;
import com.iortatechnxt.brokerverse.report.core.CodeSetSourceGroup;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Options of the multi-select filters of the renewal lists and reports (BRD 1.003.3.1.1-13): the
 * distinct values found on the renewals of a company, with the insurer and user names as labels,
 * and the fixed buckets, dispositions and stages.
 */
@Configuration
public class RenewalCodeSets {

  private static final String CO = "co";

  /**
   * Distinct-value sources.
   *
   * @param jdbc JDBC
   * @return sources
   */
  @Bean
  public CodeSetSourceGroup renewalColumnSources(NamedParameterJdbcTemplate jdbc) {
    return () ->
        List.of(
            column(jdbc, "renewal.unitHead", Sql.UNIT_HEAD),
            column(jdbc, "renewal.origin", Sql.ORIGIN),
            column(jdbc, "renewal.accountType", Sql.ACCOUNT_TYPE),
            column(jdbc, "renewal.region", Sql.REGION),
            column(jdbc, "renewal.department", Sql.DEPARTMENT),
            column(jdbc, "renewal.branch", Sql.BRANCH),
            column(jdbc, "renewal.riskCode", Sql.RISK_CODE),
            column(jdbc, "renewal.segment", Sql.SEGMENT),
            column(jdbc, "renewal.officer", Sql.OFFICER),
            column(jdbc, "renewal.insurer", Sql.INSURER),
            column(jdbc, "renewal.unit", Sql.UNIT));
  }

  /**
   * Fixed-value sources.
   *
   * @return sources
   */
  @Bean
  public CodeSetSourceGroup renewalEnumSources() {
    return () ->
        List.of(
            fixed("renewal.bucket", Bucket.values(), Bucket::label),
            fixed("renewal.disposition", RenewalDisposition.values(), RenewalDisposition::label),
            fixed("renewal.stage", RenewalStage.values(), RenewalStage::label));
  }

  private static CodeSetSource column(NamedParameterJdbcTemplate jdbc, String key, String sql) {
    return new CodeSetSource() {
      @Override
      public String source() {
        return key;
      }

      @Override
      public List<CodeOption> options(Long companyId) {
        return jdbc.query(
            sql,
            Map.of(CO, companyId),
            (rs, i) -> new CodeOption(rs.getString(1), rs.getString(2)));
      }
    };
  }

  private static <E extends Enum<E>> CodeSetSource fixed(
      String key, E[] values, Function<E, String> label) {
    List<CodeOption> options =
        Arrays.stream(values).map(v -> new CodeOption(v.name(), label.apply(v))).toList();
    return new CodeSetSource() {
      @Override
      public String source() {
        return key;
      }

      @Override
      public List<CodeOption> options(Long companyId) {
        return options;
      }
    };
  }

  /** Constant queries of the distinct values. */
  private static final class Sql {
    static final String UNIT_HEAD =
        "select distinct c.unit_head, coalesce(u.full_name, c.unit_head) from rnw_candidate c"
            + " left join sec_user u on u.username = c.unit_head"
            + " where c.company_id = :co and c.unit_head is not null order by 2";
    static final String OFFICER =
        "select distinct c.account_officer, coalesce(u.full_name, c.account_officer)"
            + " from rnw_candidate c left join sec_user u on u.username = c.account_officer"
            + " where c.company_id = :co and c.account_officer is not null order by 2";
    static final String INSURER =
        "select distinct c.insurer_code, coalesce(i.name, c.insurer_code) from rnw_candidate c"
            + " left join cat_insurer i on i.party_code = c.insurer_code"
            + " and i.company_id = c.company_id"
            + " where c.company_id = :co and c.insurer_code is not null order by 2";
    static final String ORIGIN =
        "select distinct business_origin, business_origin from rnw_candidate"
            + " where company_id = :co and business_origin is not null order by 1";
    static final String ACCOUNT_TYPE =
        "select distinct account_type, account_type from rnw_candidate"
            + " where company_id = :co and account_type is not null order by 1";
    static final String REGION =
        "select distinct c.region_code, coalesce(s.name, c.region_code) from rnw_candidate c"
            + " left join cat_sales_unit s on s.code = c.region_code and s.company_id = c.company_id"
            + " where c.company_id = :co and c.region_code is not null order by 2";
    static final String DEPARTMENT =
        "select distinct c.department_code, coalesce(s.name, c.department_code) from rnw_candidate c"
            + " left join cat_sales_unit s on s.code = c.department_code and s.company_id = c.company_id"
            + " where c.company_id = :co and c.department_code is not null order by 2";
    static final String BRANCH =
        "select distinct branch_code, branch_code from rnw_candidate"
            + " where company_id = :co and branch_code is not null order by 1";
    static final String RISK_CODE =
        "select distinct product_code, product_code || coalesce(' - ' || product_name, '')"
            + " from rnw_candidate where company_id = :co and product_code is not null order by 1";
    static final String SEGMENT =
        "select distinct c.segment, coalesce((select min(l.label) from lov_value l"
            + " where l.type_code = 'MARKET_SEGMENT' and l.code = c.segment), c.segment)"
            + " from rnw_candidate c where c.company_id = :co and c.segment is not null order by 2";
    static final String UNIT =
        "select distinct c.owner_unit, coalesce(s.name, c.owner_unit) from rnw_candidate c"
            + " left join cat_sales_unit s on s.code = c.owner_unit and s.company_id = c.company_id"
            + " where c.company_id = :co and c.owner_unit is not null order by 2";

    private Sql() {}
  }
}
