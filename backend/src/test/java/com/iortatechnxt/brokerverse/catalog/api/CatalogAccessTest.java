package com.iortatechnxt.brokerverse.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogAccessTest {

  @Test
  void theScreensThatShowAnInsurerByNameMayReadTheInsurerList() {
    assertThat(CatalogAccess.INSURER_LIST)
        .contains("'SBM_VIEW'", "'RNW_VIEW'", "'BCL_VIEW'", "'CLX_VIEW'", "'EB_VIEW'", "'CSF_VIEW'")
        .contains("'MASTER_VIEW'", "'ACSL_VIEW'", "'REMIT_DEDUCTION_CONFIRM'");
    // Maintaining insurers stays with master data.
    assertThat(CatalogAccess.MAINTAIN).isEqualTo("hasAuthority('MASTER_MAINTAIN')");
  }
}
