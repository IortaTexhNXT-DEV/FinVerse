package com.iortatechnxt.brokerverse.configpromo;

import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.ColumnInfo;
import com.iortatechnxt.brokerverse.configpromo.engine.ForeignKey;
import com.iortatechnxt.brokerverse.configpromo.engine.TableSchema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small catalogue and schema for the engine tests: companies, branches of a company, accounts of
 * a company with a parent account, rules and the lines of a rule, and a transaction table that uses
 * accounts.
 */
public final class EngineFixtures {

  /** Catalogue of the fixture. */
  public static final String CATALOGUE =
      """
      groups:
        - {code: G, name: Group}
      datasets:
        - {code: COMPANY, name: Companies, group: G, module: org, table: company, key: [code]}
        - {code: BRANCH, name: Branches, group: G, module: org, table: branch, key: [company_id, code]}
        - {code: ACCOUNT, name: Accounts, group: G, module: coa, table: account, key: [company_id, code],
           environment: [balance], usedBy: [journal.account_code]}
        - {code: RULE, name: Rules, group: G, module: acc, table: rule, key: [code]}
        - {code: RULE_LINE, name: Rule lines, group: G, module: acc, table: rule_line, key: [rule_id, line_no],
           parent: rule_id}
        - code: PARAM
          name: Parameters
          group: G
          module: sys
          table: param
          key: [param_key]
          environmentRows: {column: param_key, values: [MAIL_FROM]}
      reasons:
        TRANSACTION: Transactions
      excluded:
        TRANSACTION: [journal]
      """;

  private EngineFixtures() {}

  /**
   * The catalogue.
   *
   * @return catalogue
   */
  public static ConfigCatalogue catalogue() {
    return ConfigCatalogue.read(
        new ByteArrayInputStream(CATALOGUE.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * The schema of the fixture.
   *
   * @return tables
   */
  public static Map<String, TableSchema> tables() {
    Map<String, TableSchema> t = new LinkedHashMap<>();
    t.put(
        "company",
        table("company", List.of(), "id:int8", "version:int8", "code:varchar", "name:varchar"));
    t.put(
        "branch",
        table(
            "branch",
            List.of(new ForeignKey(List.of("company_id"), "company", List.of("id"))),
            "id:int8",
            "company_id:int8",
            "code:varchar",
            "name:varchar",
            "record_status:varchar",
            "authorized_by:varchar",
            "authorized_at:timestamptz",
            "created_at:timestamptz",
            "created_by:varchar"));
    t.put(
        "account",
        table(
            "account",
            List.of(
                new ForeignKey(List.of("company_id"), "company", List.of("id")),
                new ForeignKey(List.of("parent_id"), "account", List.of("id"))),
            "id:int8",
            "company_id:int8",
            "code:varchar",
            "parent_id:int8?",
            "active:bool",
            "balance:numeric"));
    t.put("rule", table("rule", List.of(), "id:int8", "code:varchar", "name:varchar"));
    t.put(
        "rule_line",
        table(
            "rule_line",
            List.of(new ForeignKey(List.of("rule_id"), "rule", List.of("id"))),
            "id:int8",
            "rule_id:int8",
            "line_no:int4",
            "account_code:varchar"));
    t.put(
        "param", table("param", List.of(), "id:int8", "param_key:varchar", "param_value:varchar"));
    t.put(
        "journal",
        table(
            "journal",
            List.of(new ForeignKey(List.of("account_id"), "account", List.of("id"))),
            "id:int8",
            "account_id:int8",
            "account_code:varchar"));
    return t;
  }

  /**
   * The resolved catalogue of the fixture.
   *
   * @return model
   */
  public static CatalogueModel model() {
    return new CatalogueModel(catalogue(), tables());
  }

  /**
   * A table from "name:type" columns; a trailing "?" marks a nullable column.
   *
   * @param name table
   * @param keys foreign keys
   * @param columns columns
   * @return table
   */
  public static TableSchema table(String name, List<ForeignKey> keys, String... columns) {
    Map<String, ColumnInfo> cols = new LinkedHashMap<>();
    for (String c : columns) {
      String[] p = c.split(":");
      boolean nullable = p[1].endsWith("?");
      String type = nullable ? p[1].substring(0, p[1].length() - 1) : p[1];
      cols.put(p[0], new ColumnInfo(p[0], type, nullable, "id".equals(p[0])));
    }
    return new TableSchema(name, cols, keys);
  }
}
