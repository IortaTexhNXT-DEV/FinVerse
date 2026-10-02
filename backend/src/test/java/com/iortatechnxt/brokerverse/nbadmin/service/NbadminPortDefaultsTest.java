package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalPartyKind;
import com.iortatechnxt.brokerverse.nbadmin.service.ExternalUserProvisioner.ExternalUserAction;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** The refusing default of the external-user port (decision D7). */
class NbadminPortDefaultsTest {

  private final ExternalUserProvisioner provisioner = new NbadminPortDefaults().noExternalUsers();
  private final ExternalUserAccount account =
      new ExternalUserAccount(
          "AR-1",
          "hr.user",
          "HR User",
          "hr@example.ph",
          ExternalPartyKind.CLIENT,
          "C-1",
          "CLIENT_HR",
          null);

  @Test
  void refusesEveryExternalUserCall() {
    assertThat(provisioner.available()).isFalse();
    List<Consumer<ExternalUserAccount>> calls =
        List.of(
            a -> provisioner.validate(ExternalUserAction.CREATE, a),
            provisioner::create,
            provisioner::disable,
            provisioner::enable);
    calls.forEach(
        call ->
            assertThatThrownBy(() -> call.accept(account))
                .extracting("code")
                .isEqualTo(ExternalUserProvisioner.NOT_AVAILABLE));
  }
}
