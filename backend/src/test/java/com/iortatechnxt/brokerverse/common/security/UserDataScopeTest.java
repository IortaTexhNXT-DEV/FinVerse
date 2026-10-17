package com.iortatechnxt.brokerverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserDataScopeTest {

  private static final UserDataScope LISTED =
      UserDataScope.of(
          List.of(CompanyScope.allBranchesOf(12L), CompanyScope.branchesOf(14L, List.of(5L, 3L))));

  @Test
  void allCompaniesAllowsEverything() {
    assertThat(UserDataScope.ALL.allowsCompany(99L)).isTrue();
    assertThat(UserDataScope.ALL.allowsBranch(99L, 7L)).isTrue();
    assertThat(UserDataScope.ALL.text()).isEqualTo("ALL");
    assertThat(UserDataScope.ALL.companyIds()).isEmpty();
  }

  @Test
  void noneAllowsNothing() {
    assertThat(UserDataScope.NONE.allowsCompany(12L)).isFalse();
    assertThat(UserDataScope.NONE.allowsBranch(12L, 1L)).isFalse();
    assertThat(UserDataScope.NONE.text()).isEmpty();
  }

  @Test
  void listedCompaniesAndBranches() {
    assertThat(LISTED.allowsCompany(12L)).isTrue();
    assertThat(LISTED.allowsCompany(13L)).isFalse();
    assertThat(LISTED.allowsBranch(12L, 77L)).isTrue();
    assertThat(LISTED.allowsBranch(14L, 3L)).isTrue();
    assertThat(LISTED.allowsBranch(14L, 4L)).isFalse();
    assertThat(LISTED.allowsBranch(13L, 3L)).isFalse();
    assertThat(LISTED.companyIds()).containsExactlyInAnyOrder(12L, 14L);
  }

  @Test
  void textRoundTrips() {
    assertThat(LISTED.text()).isEqualTo("12:*;14:3,5");
    assertThat(UserDataScope.parse("12:*;14:3,5")).isEqualTo(LISTED);
    assertThat(UserDataScope.parse(" ALL ")).isEqualTo(UserDataScope.ALL);
    assertThat(UserDataScope.parse(null)).isEqualTo(UserDataScope.NONE);
    assertThat(UserDataScope.parse("  ")).isEqualTo(UserDataScope.NONE);
    assertThat(UserDataScope.parse("14:").company(14L).orElseThrow().branchIds()).isEmpty();
  }

  @Test
  void malformedTextIsRefused() {
    assertThatThrownBy(() -> UserDataScope.parse("x")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UserDataScope.parse("a:*"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new CompanyScope(null, true, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void entriesOfOneCompanyAreMerged() {
    UserDataScope merged =
        UserDataScope.of(
            List.of(
                CompanyScope.branchesOf(14L, List.of(5L)),
                CompanyScope.branchesOf(14L, List.of(3L)),
                CompanyScope.branchesOf(12L, List.of(1L)),
                CompanyScope.allBranchesOf(12L)));
    assertThat(merged.text()).isEqualTo("12:*;14:3,5");
    assertThat(new UserDataScope(true, List.of(CompanyScope.allBranchesOf(1L))).companies())
        .isEmpty();
    assertThat(new UserDataScope(false, null).companies()).isEmpty();
    assertThat(new CompanyScope(1L, false, null).branchIds()).isEmpty();
  }

  @Test
  void withinComparesWithTheGrantersScope() {
    UserDataScope oneBranch = UserDataScope.of(List.of(CompanyScope.branchesOf(14L, List.of(3L))));
    assertThat(oneBranch.within(LISTED)).isTrue();
    assertThat(LISTED.within(UserDataScope.ALL)).isTrue();
    assertThat(UserDataScope.ALL.within(LISTED)).isFalse();
    assertThat(LISTED.within(oneBranch)).isFalse();
    UserDataScope allOf14 = UserDataScope.of(List.of(CompanyScope.allBranchesOf(14L)));
    assertThat(allOf14.within(LISTED)).isFalse();
    assertThat(UserDataScope.of(List.of(CompanyScope.allBranchesOf(12L))).within(LISTED)).isTrue();
  }

  @Test
  void filterKeepsAllowedRowsAndRowsWithoutCompany() {
    DataScope scope =
        new DataScope() {
          @Override
          public void requireCompany(Long companyId) {
            // not used
          }

          @Override
          public void requireBranch(Long companyId, Long branchId) {
            // not used
          }

          @Override
          public UserDataScope allowed() {
            return LISTED;
          }
        };
    List<Long> rows = java.util.Arrays.asList(12L, 13L, null, 14L);
    assertThat(scope.filter(rows, r -> r)).containsExactly(12L, null, 14L);
  }
}
