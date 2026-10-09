package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The configuration catalogue of the platform is complete and consistent. */
class ConfigCatalogueTest {

  private final ConfigCatalogue catalogue = ConfigCatalogue.load();

  @Test
  void theCatalogueListsTheConfigurationOfEveryModuleTheBriefNames() {
    Set<String> codes =
        catalogue.datasets().stream().map(CatalogueDataset::code).collect(Collectors.toSet());

    assertThat(catalogue.datasets()).hasSizeGreaterThan(140);
    assertThat(codes)
        .contains(
            "LOV_VALUE",
            "SYS_PARAMETER",
            "ORG_COMPANY",
            "ORG_BRANCH",
            "CAT_SALES_UNIT",
            "DIM_VALUE",
            "ORG_HOLIDAY",
            "CAT_PRODUCT_LINE",
            "CAT_PRODUCT",
            "CAT_PRODUCT_VERSION",
            "CAT_COVERAGE",
            "CAT_RATE",
            "CAT_CLAUSE",
            "CAT_FIELD_RULE",
            "CAT_INSURER",
            "PTY_PARTY",
            "CMR_INCENTIVE_SCHEME",
            "SBM_APPROVAL_MATRIX",
            "WF_STAGE",
            "WF_TRANSITION",
            "MSG_NOTIFICATION_EVENT",
            "DOC_TEMPLATE",
            "FIN_SCHEDULE_DEF",
            "COA_ACCOUNT",
            "ACC_RULE",
            "TAX_CODE",
            "SCR_CONFIG_VERSION",
            "CLX_ESCALATION_RULE",
            "SEC_ROLE",
            "SEC_ROLE_PERMISSION",
            "NBA_SOD_RULE",
            "SEC_USER_DATA_SCOPE",
            "MIG_LAYOUT",
            "MIG_CODE_MAP_ENTRY",
            "SYS_PRODUCT_MODULE");
  }

  @Test
  void transactionsSecretsLogsAndCountersAreNeverPromoted() {
    assertThat(catalogue.excludedTables())
        .containsEntry("acc_account", "TRANSACTION")
        .containsEntry("crm_client", "CLIENT_DATA")
        .containsEntry("sec_user_mfa", "SECRET")
        .containsEntry("sec_user_session", "SECRET")
        .containsEntry("audit_log", "LOG")
        .containsEntry("document_sequence", "COUNTER")
        .containsEntry("stored_file", "FILE")
        .containsEntry("jnl_batch", "TRANSACTION");
    assertThat(catalogue.byTable("sec_user").orElseThrow().exclude())
        .contains("password_hash", "failed_attempts", "last_login_at");
  }

  @Test
  void usersAreOptionalAndNotSelectedByDefault() {
    CatalogueDataset users = catalogue.dataset("SEC_USER");

    assertThat(users.users()).isTrue();
    assertThat(users.selectedByDefault(false)).isFalse();
    assertThat(users.selectedByDefault(true)).isTrue();
    assertThat(catalogue.dataset("CUR_EXCHANGE_RATE").selectedByDefault(true)).isFalse();
    assertThat(catalogue.dataset("SEC_ROLE").selectedByDefault(false)).isTrue();
  }

  @Test
  void runningCountersAndBalancesStayWithTheEnvironment() {
    assertThat(catalogue.dataset("CSH_RECEIPT_SERIES").environment()).contains("next_no");
    assertThat(catalogue.dataset("PAY_CHEQUE_BOOK").environment()).contains("next_no");
    assertThat(catalogue.dataset("PAY_PETTY_CASH_FUND").environment()).contains("cash_balance");
    assertThat(catalogue.dataset("SYS_PARAMETER").environmentRows().values())
        .contains("MAIL_FROM_ADDRESS", "AUTH_MODE", "CONFIG_PROMOTION_PRODUCTION_WINDOW");
  }

  @Test
  void collectionsHaveTheirParentInTheKey() {
    for (CatalogueDataset d : catalogue.datasets()) {
      if (d.collection()) {
        assertThat(d.key()).as(d.code()).contains(d.parent());
      }
    }
  }

  @Test
  void anInconsistentCatalogueIsRefused() {
    String unknownGroup =
        """
        groups: [{code: G, name: Group}]
        datasets:
          - {code: A, name: A, group: X, module: m, table: a, key: [code]}
        reasons: {}
        excluded: {}
        """;
    String twice =
        """
        groups: [{code: G, name: Group}]
        datasets:
          - {code: A, name: A, group: G, module: m, table: a, key: [code]}
        reasons: {T: Transactions}
        excluded: {T: [a]}
        """;

    assertThatThrownBy(() -> read(unknownGroup)).hasMessageContaining("unknown group");
    assertThatThrownBy(() -> read(twice)).hasMessageContaining("both a dataset and excluded");
  }

  private static ConfigCatalogue read(String yaml) {
    return ConfigCatalogue.read(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }
}
