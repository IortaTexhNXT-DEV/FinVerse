package com.iortatechnxt.brokerverse.nbadmin.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The audit log report reads in words: attribute labels, Yes / No, profile and branch names. */
class UamReportSupportTest {

  @Test
  void attributesAreLabelledAsTheScreensNameThem() {
    assertThat(UamReportSupport.attributeLabel("fullName")).isEqualTo("Full Name");
    assertThat(UamReportSupport.attributeLabel("email")).isEqualTo("E-mail");
    assertThat(UamReportSupport.attributeLabel("windowsId")).isEqualTo("Windows ID");
    assertThat(UamReportSupport.attributeLabel("homeBranchId")).isEqualTo("Home Branch");
    assertThat(UamReportSupport.attributeLabel("effectiveDate")).isEqualTo("Effective Date");
    assertThat(UamReportSupport.attributeLabel(null)).isEmpty();
  }

  @Test
  void absentAndBooleanValuesReadAsNullYesAndNo() {
    assertThat(UamReportSupport.orNull(null)).isEqualTo("Null");
    assertThat(UamReportSupport.orNull(" ")).isEqualTo("Null");
    assertThat(UamReportSupport.orNull("true")).isEqualTo("Yes");
    assertThat(UamReportSupport.orNull("false")).isEqualTo("No");
    assertThat(UamReportSupport.orNull("Isabel Navarro")).isEqualTo("Isabel Navarro");
  }

  @Test
  void profileCodesAndBranchIdsReadAsNames() {
    Map<String, String> roles = Map.of("MKT_AO", "Marketing Account Officer");
    Map<String, String> branches = Map.of("1", "HO - Head Office");
    assertThat(UamReportSupport.shownValue("roles", "MKT_AO,ZZZ", roles, branches))
        .isEqualTo("Marketing Account Officer, ZZZ");
    assertThat(UamReportSupport.shownValue("homeBranchId", "1", roles, branches))
        .isEqualTo("HO - Head Office");
    assertThat(UamReportSupport.shownValue("homeBranchId", "9", roles, branches)).isEqualTo("9");
    assertThat(UamReportSupport.shownValue("fullName", "Isabel", roles, branches))
        .isEqualTo("Isabel");
    assertThat(UamReportSupport.shownValue("homeBranchId", null, roles, branches)).isNull();
  }
}
