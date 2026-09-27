package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.RiskIdentifiers;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The reference and seed data corrections of the sign-off packs: package products with business
 * names (sold in the seed data), distinct seed clients, location keys written as the application
 * writes them, no placeholder texts, PAR08 version 2 of its released request, and document upload
 * for ManCom and the TSU Team Head.
 */
@IntegrationTest
class SignoffCorrectionsIT {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void packageProductsHaveBusinessNamesAndAreSoldInTheSeedData() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cat_product where name like '%matrix to be loaded%'",
                Integer.class))
        .isZero();
    assertThat(jdbc.queryForMap("select name, record_status from cat_product where code = 'PAR09'"))
        .containsEntry("name", "Property All Risks Package PAR09")
        .containsEntry("record_status", "ACTIVE");
    assertThat(
            jdbc.queryForObject("select name from cat_product where code = 'MTR10'", String.class))
        .isEqualTo("Motor Comprehensive Package MTR10");
  }

  @Test
  void seedClientsAreDistinctAndHaveRealisticAddresses() {
    assertThat(
            jdbc.queryForList(
                "select display_name from crm_client where client_code in"
                    + " ('CL-2026-000001', 'CL-2026-900001')",
                String.class))
        .doesNotHaveDuplicates()
        .hasSize(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_risk_item where address in"
                    + " ('9 Booking Street, Ortigas Center', '12 Booking Road, Lahug',"
                    + " '27 Multi-Year Avenue, Kapitolyo')",
                Integer.class))
        .isZero();
  }

  @Test
  void seedLocationKeysMatchTheApplicationNormalisation() {
    List<Map<String, Object>> items =
        jdbc.queryForList(
            "select address, city, location_key from acc_risk_item where address is not null");
    assertThat(items).isNotEmpty();
    assertThat(items)
        .allSatisfy(
            i ->
                assertThat(i.get("location_key"))
                    .isEqualTo(
                        RiskIdentifiers.locationKey(
                            (String) i.get("address"), (String) i.get("city"))));
  }

  @Test
  void noSeedTextIsAPlaceholder() {
    assertThat(
            jdbc.queryForObject(
                "select (select count(*) from bkg_incentive_rule where description like 'Placeholder%')"
                    + " + (select count(*) from bkg_auto_book_rule where description like 'Placeholder%')"
                    + " + (select count(*) from rem_incentive_rule where description like 'Placeholder%')"
                    + " + (select count(*) from cat_product_version where change_summary like 'Placeholder%')"
                    + " + (select count(*) from cat_incentive_criteria where name like 'Placeholder%'"
                    + "    or description like 'Placeholder%')"
                    + " + (select count(*) from lov_value where label like 'Placeholder%')",
                Integer.class))
        .isZero();
  }

  @Test
  void theReleasedPackageRequestHasItsVersion() {
    // Other tests may release later versions of PAR08: version 2 stays released or superseded.
    Map<String, Object> second =
        jdbc.queryForMap(
            "select status, source_request_no from cat_product_version"
                + " where product_code = 'PAR08' and version_no = 2");
    assertThat(second).containsEntry("source_request_no", "PKR-2026-900006");
    assertThat(second.get("status")).isIn("RELEASED", "SUPERSEDED", "EXPIRED");
    assertThat(
            jdbc.queryForObject(
                "select status from cat_product_version where product_code = 'PAR08' and version_no = 1",
                String.class))
        .isNotEqualTo("RELEASED");
  }

  @Test
  void manComAndTheTsuHeadMayAttachDocuments() {
    assertThat(
            jdbc.queryForList(
                "select r.code from sec_role r join sec_role_permission p on p.role_id = r.id"
                    + " where p.permission = 'ATTACHMENT_MANAGE' and r.code in ('MANCOM', 'TSU_HEAD')",
                String.class))
        .containsExactlyInAnyOrder("MANCOM", "TSU_HEAD");
  }
}
