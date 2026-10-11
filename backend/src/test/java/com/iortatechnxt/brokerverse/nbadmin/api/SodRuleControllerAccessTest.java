package com.iortatechnxt.brokerverse.nbadmin.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SodRuleControllerAccessTest {

  @Test
  void requestorsAndApproversReadTheRulesButOnlyMaintainersChangeThem() throws Exception {
    String list =
        SodRuleController.class.getMethod("list").getAnnotation(PreAuthorize.class).value();
    assertThat(list)
        .contains("'UAM_VIEW'", "'ACCESS_APPROVE'", "'USER_MANAGE'")
        .contains("'UAM_SOD_MAINTAIN'", "'UAM_SOD_AUTHORIZE'", "'AUDIT_VIEW'");
    for (var method : SodRuleController.class.getDeclaredMethods()) {
      PreAuthorize rule = method.getAnnotation(PreAuthorize.class);
      if (rule != null && !"list".equals(method.getName())) {
        assertThat(rule.value()).doesNotContain("UAM_VIEW").doesNotContain("ACCESS_APPROVE");
      }
    }
  }
}
