package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.identity.service.IdentityReconciliationJob;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentityReconciliationJobTest {

  private static DirectoryAccount account(DirectoryStatus status, List<String> groups) {
    return new DirectoryAccount(
        "BDO\\msantos",
        "a100000001",
        null,
        null,
        null,
        "Maria Santos",
        null,
        status,
        null,
        null,
        "UIDM-1",
        groups);
  }

  private static IdentityReconciliationJob.Snapshot user(boolean enabled, Set<String> groups) {
    return new IdentityReconciliationJob.Snapshot("a100000001", "BDO\\msantos", enabled, groups);
  }

  @Test
  void anActiveUserUnknownToTheDirectoryIsAMismatch() {
    assertThat(IdentityReconciliationJob.compare(user(true, Set.of()), Optional.empty()))
        .get()
        .extracting(IdentityReconciliationJob.Mismatch::kind)
        .isEqualTo(IdentityReconciliationJob.NOT_IN_DIRECTORY);
    assertThat(IdentityReconciliationJob.compare(user(false, Set.of()), Optional.empty()))
        .isEmpty();
  }

  @Test
  void aDifferentStatusIsAMismatch() {
    assertThat(
            IdentityReconciliationJob.compare(
                user(true, Set.of("AUDITOR")),
                Optional.of(account(DirectoryStatus.DEACTIVATED, List.of()))))
        .get()
        .extracting(IdentityReconciliationJob.Mismatch::kind)
        .isEqualTo(IdentityReconciliationJob.STATUS);
  }

  @Test
  void groupProfilesAreComparedWhenTheDirectoryNamesThem() {
    assertThat(
            IdentityReconciliationJob.compare(
                user(true, Set.of("AUDITOR")),
                Optional.of(account(DirectoryStatus.ACTIVE, List.of("AUDITOR", "UAM_ADMIN")))))
        .get()
        .extracting(IdentityReconciliationJob.Mismatch::kind)
        .isEqualTo(IdentityReconciliationJob.GROUP_PROFILES);
    assertThat(
            IdentityReconciliationJob.compare(
                user(true, Set.of("AUDITOR")),
                Optional.of(account(DirectoryStatus.ACTIVE, List.of("AUDITOR")))))
        .isEmpty();
    assertThat(
            IdentityReconciliationJob.compare(
                user(true, Set.of("AUDITOR")),
                Optional.of(account(DirectoryStatus.ACTIVE, List.of()))))
        .isEmpty();
  }
}
