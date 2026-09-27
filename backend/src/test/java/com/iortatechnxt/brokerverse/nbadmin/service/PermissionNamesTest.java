package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Readable names of the permission codes, the same rule as the screens (permissionLabel). */
class PermissionNamesTest {

  @Test
  void theModulePrefixAndTheAcronymsAreReadable() {
    assertThat(PermissionNames.name("UAM_ENROLL")).isEqualTo("User access enroll");
    assertThat(PermissionNames.name("PKG_TSU_APPROVE")).isEqualTo("Package TSU approve");
    assertThat(PermissionNames.name("AUDIT_VIEW")).isEqualTo("Audit view");
    assertThat(PermissionNames.name("UAM_SOD_AUTHORIZE")).isEqualTo("User access SOD authorize");
    assertThat(PermissionNames.name(" ")).isEmpty();
    assertThat(PermissionNames.name(null)).isEmpty();
  }

  @Test
  void severalNamesFollowTheOrderOfTheCodes() {
    assertThat(PermissionNames.names(List.of("REPORT_VIEW", "AUDIT_VIEW")))
        .isEqualTo("Audit view, Report view");
  }
}
