package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource.CodeOption;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The platform lists that report parameters choose from by name (Report Centre): users, insurers,
 * sales units, product lines and products, market segments, cost centres, bank accounts, group
 * profiles, modules, screening risk categories and alert rules. The value of a choice is the code
 * the reports filter on.
 */
@Configuration
public class PlatformCodeSets {

  /** Users by name; the value is the login. */
  public static final String USER = "platform.user";

  /** Insurers of the company by name; the value is the party code. */
  public static final String INSURER = "platform.insurer";

  /** Sales units (regions, departments, teams) of the company by name. */
  public static final String SALES_UNIT = "platform.salesUnit";

  /** Product lines by name. */
  public static final String PRODUCT_LINE = "platform.productLine";

  /** Products by name. */
  public static final String PRODUCT = "platform.product";

  /** Market segments by name. */
  public static final String SEGMENT = "platform.segment";

  /** Cost centres (departments) of the company by name. */
  public static final String COST_CENTRE = "platform.costCentre";

  /** Bank accounts of the company by name. */
  public static final String BANK_ACCOUNT = "platform.bankAccount";

  /** Group profiles by name. */
  public static final String GROUP_PROFILE = "platform.groupProfile";

  /** Modules (permission areas) by name. */
  public static final String MODULE = "platform.module";

  /** Screening risk categories by name. */
  public static final String RISK_CATEGORY = "platform.riskCategory";

  /** Alert rules (Exception Codes Master) by name. */
  public static final String ALERT_RULE = "platform.alertRule";

  /** Account schedules of the report pack by name. */
  public static final String GL_SCHEDULE = "platform.glSchedule";

  /** Insurer statement of account uploads of the company. */
  public static final String SOA_UPLOAD = "platform.soaUpload";

  private static final String CO = "co";

  /**
   * The platform lists.
   *
   * @param jdbc JDBC
   * @return sources
   */
  @Bean
  public CodeSetSourceGroup platformLists(NamedParameterJdbcTemplate jdbc) {
    return () ->
        List.of(
            query(jdbc, USER, Sql.USERS),
            query(jdbc, INSURER, Sql.INSURERS),
            query(jdbc, SALES_UNIT, Sql.SALES_UNITS),
            query(jdbc, PRODUCT_LINE, Sql.PRODUCT_LINES),
            query(jdbc, PRODUCT, Sql.PRODUCTS),
            query(jdbc, SEGMENT, Sql.SEGMENTS),
            query(jdbc, COST_CENTRE, Sql.COST_CENTRES),
            query(jdbc, BANK_ACCOUNT, Sql.BANK_ACCOUNTS),
            query(jdbc, GROUP_PROFILE, Sql.GROUP_PROFILES),
            labelled(jdbc, MODULE, Sql.MODULES, DisplayFormat::label),
            query(jdbc, RISK_CATEGORY, Sql.RISK_CATEGORIES),
            query(jdbc, ALERT_RULE, Sql.ALERT_RULES),
            query(jdbc, GL_SCHEDULE, Sql.GL_SCHEDULES),
            query(jdbc, SOA_UPLOAD, Sql.SOA_UPLOADS));
  }

  private static CodeSetSource query(NamedParameterJdbcTemplate jdbc, String key, String sql) {
    return labelled(jdbc, key, sql, UnaryOperator.identity());
  }

  private static CodeSetSource labelled(
      NamedParameterJdbcTemplate jdbc, String key, String sql, UnaryOperator<String> label) {
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
            (rs, i) -> new CodeOption(rs.getString(1), label.apply(rs.getString(2))));
      }
    };
  }

  /** Constant queries of the lists (the company is bound as :co where the list is per company). */
  private static final class Sql {
    static final String USERS =
        "select username, coalesce(full_name, username) from sec_user"
            + " where enabled order by 2";
    static final String INSURERS =
        "select party_code, name from cat_insurer where company_id = :co"
            + " and record_status <> 'INACTIVE' order by name";
    static final String SALES_UNITS =
        "select code, name from cat_sales_unit where company_id = :co"
            + " and record_status <> 'INACTIVE' order by name";
    static final String PRODUCT_LINES =
        "select code, name from cat_product_line where record_status <> 'INACTIVE'"
            + " order by sort_order, name";
    static final String PRODUCTS =
        "select code, name from cat_product where record_status <> 'INACTIVE'" + " order by name";
    static final String SEGMENTS =
        "select code, min(label) from lov_value where type_code = 'MARKET_SEGMENT'"
            + " group by code order by 2";
    static final String COST_CENTRES =
        "select code, name from dim_value where company_id = :co"
            + " and dimension_type = 'COST_CENTER' and active order by name";
    static final String BANK_ACCOUNTS =
        "select code, name from pay_bank_account where company_id = :co order by name";
    static final String GROUP_PROFILES = "select code, name from sec_role order by name";
    static final String MODULES =
        "select distinct area, area from sec_permission_action"
            + " where area is not null order by 1";
    static final String RISK_CATEGORIES =
        "select code, min(name) from scr_risk_category group by code order by 2";
    static final String GL_SCHEDULES =
        "select code, name from fin_schedule_def where active order by name";
    static final String SOA_UPLOADS =
        "select u.upload_no, u.upload_no || ' – ' || coalesce(i.name, u.insurer_code)"
            + " from acsl_soa_upload u left join cat_insurer i on i.party_code = u.insurer_code"
            + " and i.company_id = u.company_id where u.company_id = :co order by u.id desc";
    static final String ALERT_RULES = "select code, name from alt_exception_code order by name";
  }
}
